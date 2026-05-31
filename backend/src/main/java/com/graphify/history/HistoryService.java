package com.graphify.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.common.dto.ApiMeta;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.dto.GraphEdgeDto;
import com.graphify.company.dto.GraphNodeDto;
import com.graphify.company.dto.InsightCardDto;
import com.graphify.company.dto.SignalDto;
import com.graphify.history.dto.DiffSummaryDto;
import com.graphify.history.dto.HistoryCompanyDto;
import com.graphify.history.dto.HistoryDetailDto;
import com.graphify.history.dto.HistoryGraphSnapshotDto;
import com.graphify.history.dto.HistoryItemDto;
import com.graphify.history.dto.HistoryListDataDto;
import com.graphify.history.dto.TimelineEventDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HistoryService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final AnalysisHistoryRepository analysisHistoryRepository;
    private final GraphSnapshotRepository graphSnapshotRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final HistoryInsightSnapshotRepository insightSnapshotRepository;
    private final HistorySignalSnapshotRepository signalSnapshotRepository;
    private final HistoryDiffSummaryRepository diffSummaryRepository;
    private final ObjectMapper objectMapper;

    public HistoryService(
            AnalysisHistoryRepository analysisHistoryRepository,
            GraphSnapshotRepository graphSnapshotRepository,
            TimelineEventRepository timelineEventRepository,
            HistoryInsightSnapshotRepository insightSnapshotRepository,
            HistorySignalSnapshotRepository signalSnapshotRepository,
            HistoryDiffSummaryRepository diffSummaryRepository,
            ObjectMapper objectMapper
    ) {
        this.analysisHistoryRepository = analysisHistoryRepository;
        this.graphSnapshotRepository = graphSnapshotRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.insightSnapshotRepository = insightSnapshotRepository;
        this.signalSnapshotRepository = signalSnapshotRepository;
        this.diffSummaryRepository = diffSummaryRepository;
        this.objectMapper = objectMapper;
    }

    public ApiResponse<HistoryListDataDto> getMyHistory(
            int page,
            Integer size,
            String q,
            String from,
            String to
    ) {
        Long userId = requireCurrentUserId();
        int pageSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                pageSize,
                Sort.by(Sort.Direction.DESC, "analyzedAt")
        );

        String normalizedQuery = emptyToNull(q);
        Instant fromInstant = parseDateStart(from);
        Instant toInstant = parseDateEnd(to);

        Specification<AnalysisHistory> spec = AnalysisHistorySpecifications.forUser(
                userId,
                normalizedQuery,
                fromInstant,
                toInstant
        );
        Page<AnalysisHistory> result = analysisHistoryRepository.findAll(spec, pageable);

        List<HistoryItemDto> items = result.getContent().stream()
                .map(this::toDto)
                .toList();

        ApiMeta meta = new ApiMeta(result.getNumber(), result.getSize(), result.getTotalElements());
        return ApiResponse.ok(new HistoryListDataDto(items), meta);
    }

    public ApiResponse<HistoryDetailDto> getSessionDetail(String sessionIdRaw) {
        Long userId = requireCurrentUserId();
        UUID sessionId = parseSessionId(sessionIdRaw);

        AnalysisHistory history = analysisHistoryRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_HISTORY_002",
                        "분석 이력을 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        List<TimelineEventDto> timeline = timelineEventRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId)
                .stream()
                .map(this::toTimelineDto)
                .toList();

        HistoryGraphSnapshotDto graphSnapshot = loadGraphSnapshot(sessionId);
        List<InsightCardDto> insights = insightSnapshotRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId)
                .stream()
                .map(this::toInsightDto)
                .toList();
        List<SignalDto> signals = signalSnapshotRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId)
                .stream()
                .map(this::toSignalDto)
                .toList();
        DiffSummaryDto diffSummary = diffSummaryRepository.findBySessionId(sessionId)
                .map(d -> new DiffSummaryDto(d.getSummaryText(), d.getGeneratedAt()))
                .orElse(null);

        HistoryDetailDto detail = new HistoryDetailDto(
                sessionId.toString(),
                new HistoryCompanyDto(history.getCompanyId(), history.getCompanyName()),
                history.getAnalyzedAt(),
                history.getStatus(),
                history.getSummaryLine(),
                timeline,
                graphSnapshot,
                insights,
                signals,
                diffSummary
        );
        return ApiResponse.ok(detail);
    }

    private HistoryGraphSnapshotDto loadGraphSnapshot(UUID sessionId) {
        return graphSnapshotRepository.findBySessionId(sessionId)
                .map(snapshot -> new HistoryGraphSnapshotDto(
                        parseNodes(snapshot.getNodesJson()),
                        parseEdges(snapshot.getEdgesJson())
                ))
                .orElse(new HistoryGraphSnapshotDto(List.of(), List.of()));
    }

    private List<GraphNodeDto> parseNodes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<GraphNodeDto>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<GraphEdgeDto> parseEdges(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<GraphEdgeDto>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private TimelineEventDto toTimelineDto(TimelineEvent event) {
        Map<String, Object> payload = Collections.emptyMap();
        if (event.getPayloadJson() != null && !event.getPayloadJson().isBlank()) {
            try {
                payload = objectMapper.readValue(
                        event.getPayloadJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } catch (Exception ignored) {
                payload = Collections.emptyMap();
            }
        }
        return new TimelineEventDto(
                event.getEventAt(),
                event.getEventType(),
                event.getLabel(),
                payload
        );
    }

    private InsightCardDto toInsightDto(HistoryInsightSnapshot snapshot) {
        return new InsightCardDto(
                snapshot.getId(),
                snapshot.getCardType(),
                snapshot.getTitle(),
                snapshot.getSummary(),
                snapshot.getConfidence(),
                snapshot.getEvidence(),
                parseNodeIds(snapshot.getHighlightNodeIds())
        );
    }

    private SignalDto toSignalDto(HistorySignalSnapshot snapshot) {
        List<String> sources = snapshot.getSources() == null || snapshot.getSources().isBlank()
                ? List.of()
                : Arrays.stream(snapshot.getSources().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        return new SignalDto(
                snapshot.getLabel(),
                snapshot.getSignalKind(),
                parseNodeIds(snapshot.getRelatedNodeIds()),
                sources
        );
    }

    private static UUID parseSessionId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new GraphifyException(
                    "ERR_HISTORY_002",
                    "분석 이력을 찾을 수 없습니다.",
                    HttpStatus.NOT_FOUND
            );
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new GraphifyException(
                    "ERR_HISTORY_002",
                    "분석 이력을 찾을 수 없습니다.",
                    HttpStatus.NOT_FOUND
            );
        }
    }

    private static List<String> parseNodeIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new GraphifyException(
                    "ERR_AUTH_001",
                    "로그인이 필요합니다.",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return userId;
    }

    private HistoryItemDto toDto(AnalysisHistory history) {
        String sessionId = history.getSessionId() != null
                ? history.getSessionId().toString()
                : "history-" + history.getId();
        return new HistoryItemDto(
                sessionId,
                history.getCompanyId(),
                history.getCompanyName(),
                history.getAnalyzedAt(),
                history.getStatus(),
                history.getSummaryLine()
        );
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Instant parseDateStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            throw new GraphifyException(
                    "ERR_HISTORY_001",
                    "시작일 형식이 올바르지 않습니다. (YYYY-MM-DD)",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private static Instant parseDateEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1);
        } catch (Exception ex) {
            throw new GraphifyException(
                    "ERR_HISTORY_001",
                    "종료일 형식이 올바르지 않습니다. (YYYY-MM-DD)",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
