package com.graphify.admin.vectordb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.admin.vectordb.dto.CleanupPreviewDto;
import com.graphify.admin.vectordb.dto.CleanupRequest;
import com.graphify.admin.vectordb.dto.CleanupResultDto;
import com.graphify.admin.vectordb.dto.EmbeddingJobDto;
import com.graphify.admin.vectordb.dto.ReindexRequest;
import com.graphify.admin.vectordb.dto.ReindexResultDto;
import com.graphify.admin.vectordb.dto.VectorDbJobSummaryDto;
import com.graphify.admin.vectordb.dto.VectorDbStatsDto;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.CompanyRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VectorDbAdminService {

    private static final long STATS_ID = VectorIndexStats.SINGLETON_ID;
    private static final int REINDEX_DURATION_SECONDS = 30;
    private static final Set<String> VECTOR_TYPES = Set.of("COMPANY", "INSIGHT", "RELATION");

    private final VectorIndexStatsRepository vectorIndexStatsRepository;
    private final EmbeddingJobRepository embeddingJobRepository;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    public VectorDbAdminService(
            VectorIndexStatsRepository vectorIndexStatsRepository,
            EmbeddingJobRepository embeddingJobRepository,
            CompanyRepository companyRepository,
            ObjectMapper objectMapper
    ) {
        this.vectorIndexStatsRepository = vectorIndexStatsRepository;
        this.embeddingJobRepository = embeddingJobRepository;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ApiResponse<VectorDbStatsDto> getStats() {
        return ApiResponse.ok(toStatsDto(requireStats()));
    }

    public ApiResponse<ReindexResultDto> startReindex(ReindexRequest request) {
        validateReindex(request);
        EmbeddingJob job = new EmbeddingJob();
        job.setJobType("REINDEX");
        job.setScope(request.scope());
        job.setStatus("RUNNING");
        job.setProgress(0);
        job.setMessage("재임베딩을 준비 중입니다.");
        if ("SELECTED".equals(request.scope())) {
            job.setTargetIds(writeJson(request.targetIds()));
        }
        embeddingJobRepository.save(job);
        return ApiResponse.ok(new ReindexResultDto(job.getId()));
    }

    public ApiResponse<EmbeddingJobDto> getJob(long jobId) {
        EmbeddingJob job = embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> notFound("작업을 찾을 수 없습니다."));
        advanceJobIfNeeded(job);
        return ApiResponse.ok(toJobDto(job));
    }

    @Transactional(readOnly = true)
    public ApiResponse<CleanupPreviewDto> previewCleanup(int olderThanDays, List<String> types) {
        validateCleanupTypes(types);
        if (olderThanDays < 1 || olderThanDays > 3650) {
            throw badRequest("보관 기간은 1~3650일 사이여야 합니다.");
        }
        VectorIndexStats stats = requireStats();
        long preview = computePreviewCount(stats, olderThanDays, types);
        return ApiResponse.ok(new CleanupPreviewDto(preview));
    }

    public ApiResponse<CleanupResultDto> runCleanup(CleanupRequest request) {
        validateCleanupTypes(request.types());
        VectorIndexStats stats = requireStats();
        long preview = computePreviewCount(stats, request.olderThanDays(), request.types());
        if (preview <= 0) {
            return ApiResponse.ok(new CleanupResultDto(0));
        }

        Map<String, Long> byType = readByType(stats.getVectorsByType());
        long remaining = stats.getTotalVectors();
        for (String type : request.types()) {
            long current = byType.getOrDefault(type, 0L);
            long remove = Math.min(current, Math.max(1, preview / request.types().size()));
            byType.put(type, Math.max(0, current - remove));
            remaining = Math.max(0, remaining - remove);
        }

        stats.setTotalVectors(remaining);
        stats.setVectorsByType(writeJson(byType));
        stats.setIndexSizeBytes(Math.max(0, stats.getIndexSizeBytes() - preview * 2048L));
        vectorIndexStatsRepository.save(stats);

        EmbeddingJob job = new EmbeddingJob();
        job.setJobType("CLEANUP");
        job.setScope("ALL");
        job.setStatus("SUCCESS");
        job.setProgress(100);
        job.setMessage("만료 벡터 " + preview + "건을 삭제했습니다.");
        job.setDeletedCount(preview);
        job.setCompletedAt(Instant.now());
        embeddingJobRepository.save(job);

        return ApiResponse.ok(new CleanupResultDto(preview));
    }

    private void advanceJobIfNeeded(EmbeddingJob job) {
        if (!"RUNNING".equals(job.getStatus())) {
            return;
        }
        long elapsed = Duration.between(job.getCreatedAt(), Instant.now()).getSeconds();
        int progress = (int) Math.min(100, (elapsed * 100) / REINDEX_DURATION_SECONDS);
        job.setProgress(progress);
        if (progress < 25) {
            job.setMessage("대상 문서를 수집 중입니다.");
        } else if (progress < 60) {
            job.setMessage("임베딩을 생성 중입니다.");
        } else if (progress < 100) {
            job.setMessage("인덱스를 갱신 중입니다.");
        } else {
            job.setStatus("SUCCESS");
            job.setProgress(100);
            job.setMessage("재임베딩이 완료되었습니다.");
            job.setCompletedAt(Instant.now());
            bumpStatsAfterReindex(job);
        }
        embeddingJobRepository.save(job);
    }

    private void bumpStatsAfterReindex(EmbeddingJob job) {
        VectorIndexStats stats = requireStats();
        long added = "SELECTED".equals(job.getScope()) ? countTargets(job) * 12L : 240L;
        stats.setTotalVectors(stats.getTotalVectors() + added);
        stats.setIndexSizeBytes(stats.getIndexSizeBytes() + added * 2048L);

        Map<String, Long> byType = readByType(stats.getVectorsByType());
        if ("SELECTED".equals(job.getScope())) {
            byType.merge("COMPANY", added, Long::sum);
        } else {
            byType.merge("COMPANY", added / 3, Long::sum);
            byType.merge("INSIGHT", added / 3, Long::sum);
            byType.merge("RELATION", added - (2 * (added / 3)), Long::sum);
        }
        stats.setVectorsByType(writeJson(byType));
        stats.setAvgLatencyMs(stats.getAvgLatencyMs().subtract(BigDecimal.valueOf(2)).max(BigDecimal.valueOf(80)));
        stats.setAvgSimilarity(
                stats.getAvgSimilarity().add(BigDecimal.valueOf(0.002)).min(BigDecimal.ONE)
        );
        stats.setRequestCount24h(stats.getRequestCount24h() + added / 4);
        vectorIndexStatsRepository.save(stats);
    }

    private long countTargets(EmbeddingJob job) {
        List<Long> ids = readLongList(job.getTargetIds());
        return ids.isEmpty() ? 1 : ids.size();
    }

    private long computePreviewCount(VectorIndexStats stats, int olderThanDays, List<String> types) {
        if (stats.getTotalVectors() <= 0) {
            return 0;
        }
        Map<String, Long> byType = readByType(stats.getVectorsByType());
        long typeSum = types.stream().mapToLong(t -> byType.getOrDefault(t, 0L)).sum();
        if (typeSum <= 0) {
            return 0;
        }
        double ratio = Math.min(0.85, olderThanDays / 365.0 * 0.35 + 0.05);
        return Math.max(0, Math.round(typeSum * ratio));
    }

    private void validateReindex(ReindexRequest request) {
        if (!"ALL".equals(request.scope()) && !"SELECTED".equals(request.scope())) {
            throw badRequest("scope는 ALL 또는 SELECTED여야 합니다.");
        }
        if ("SELECTED".equals(request.scope())) {
            List<Long> ids = request.targetIds() == null ? List.of() : request.targetIds();
            if (ids.isEmpty()) {
                throw badRequest("선택 재임베딩에는 targetIds가 필요합니다.");
            }
            long found = companyRepository.countByIdIn(ids);
            if (found != ids.size()) {
                throw badRequest("존재하지 않는 기업 ID가 포함되어 있습니다.");
            }
        }
        boolean running = embeddingJobRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .anyMatch(j -> "RUNNING".equals(j.getStatus()));
        if (running) {
            throw badRequest("이미 실행 중인 재임베딩 작업이 있습니다.");
        }
    }

    private void validateCleanupTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            throw badRequest("삭제 대상 유형을 하나 이상 선택하세요.");
        }
        for (String type : types) {
            if (!VECTOR_TYPES.contains(type)) {
                throw badRequest("유형은 COMPANY, INSIGHT, RELATION 중 하나여야 합니다.");
            }
        }
    }

    private VectorIndexStats requireStats() {
        return vectorIndexStatsRepository.findById(STATS_ID)
                .orElseThrow(() -> notFound("벡터 인덱스 통계가 없습니다."));
    }

    private VectorDbStatsDto toStatsDto(VectorIndexStats stats) {
        List<EmbeddingJob> jobs = embeddingJobRepository.findTop5ByOrderByCreatedAtDesc();
        List<VectorDbJobSummaryDto> lastJobs = jobs.stream().map(this::toJobSummary).toList();
        return new VectorDbStatsDto(
                stats.getTotalVectors(),
                readByType(stats.getVectorsByType()),
                stats.getIndexSizeBytes(),
                stats.getAvgLatencyMs(),
                stats.getAvgSimilarity(),
                stats.getRequestCount24h(),
                readNumberSeries(stats.getLatencySeries()),
                readNumberSeries(stats.getSimilaritySeries()),
                readNumberSeries(stats.getRequestSeries()),
                lastJobs,
                stats.getUpdatedAt()
        );
    }

    private VectorDbJobSummaryDto toJobSummary(EmbeddingJob job) {
        return new VectorDbJobSummaryDto(
                job.getId(),
                job.getJobType(),
                job.getScope(),
                job.getStatus(),
                job.getProgress(),
                job.getMessage(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }

    private EmbeddingJobDto toJobDto(EmbeddingJob job) {
        return new EmbeddingJobDto(
                job.getId(),
                job.getJobType(),
                job.getScope(),
                job.getStatus(),
                job.getProgress(),
                job.getMessage(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }

    private Map<String, Long> readByType(String json) {
        try {
            Map<String, Long> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new LinkedHashMap<>(map);
        } catch (JsonProcessingException ex) {
            return new HashMap<>();
        }
    }

    private List<Number> readNumberSeries(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Number>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<Long> readLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new GraphifyException(
                    "ERR_ADMIN_VECTOR_003",
                    "데이터 직렬화에 실패했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private GraphifyException notFound(String message) {
        return new GraphifyException("ERR_ADMIN_VECTOR_001", message, HttpStatus.NOT_FOUND);
    }

    private GraphifyException badRequest(String message) {
        return new GraphifyException("ERR_ADMIN_VECTOR_002", message, HttpStatus.BAD_REQUEST);
    }
}
