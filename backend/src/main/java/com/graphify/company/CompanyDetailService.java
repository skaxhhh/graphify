package com.graphify.company;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.dart.CompanyAgentSignal;
import com.graphify.company.dart.CompanyDartProfileMapper;
import com.graphify.company.dart.CompanyDartSnapshotRepository;
import com.graphify.company.dart.CompanyInsightAgentService;
import com.graphify.company.dto.AgentInsightDto;
import com.graphify.company.dto.CompanyDartProfileDto;
import com.graphify.company.dto.CompanyDetailDto;
import com.graphify.company.dto.CompanyInsightsDto;
import com.graphify.company.dto.FinancialSummaryDto;
import com.graphify.company.dto.InsightCardDto;
import com.graphify.company.dto.ProvenanceDto;
import com.graphify.company.dto.SignalDto;
import com.graphify.home.CompanyViewTracker;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyDetailService {

    private final CompanyRepository companyRepository;
    private final CompanyInsightCardRepository insightCardRepository;
    private final CompanySignalRepository signalRepository;
    private final CompanyViewTracker companyViewTracker;
    private final CompanyDartSnapshotRepository dartSnapshotRepository;
    private final CompanyDartProfileMapper dartProfileMapper;
    private final CompanyInsightAgentService insightAgentService;

    public CompanyDetailService(
            CompanyRepository companyRepository,
            CompanyInsightCardRepository insightCardRepository,
            CompanySignalRepository signalRepository,
            CompanyViewTracker companyViewTracker,
            CompanyDartSnapshotRepository dartSnapshotRepository,
            CompanyDartProfileMapper dartProfileMapper,
            CompanyInsightAgentService insightAgentService
    ) {
        this.companyRepository = companyRepository;
        this.insightCardRepository = insightCardRepository;
        this.signalRepository = signalRepository;
        this.companyViewTracker = companyViewTracker;
        this.dartSnapshotRepository = dartSnapshotRepository;
        this.dartProfileMapper = dartProfileMapper;
        this.insightAgentService = insightAgentService;
    }

    public ApiResponse<CompanyDetailDto> getDetail(Long id) {
        Company company = findCompanyOrThrow(id);
        companyViewTracker.recordView(id);
        List<CompanyInsightCard> cards = insightCardRepository.findByCompanyIdOrderBySortOrderAsc(id);

        Map<String, Integer> coverage = buildCoverage(cards);
        CompanyDartProfileDto dartProfile = dartSnapshotRepository.findById(id)
                .map(dartProfileMapper::toProfileDto)
                .orElse(null);

        ProvenanceDto provenance = new ProvenanceDto(
                dartProfile != null
                        ? List.of("DART 공시·재무", "Open DART API", "뉴스/RSS")
                        : List.of("공시", "뉴스", "IR"),
                company.getUpdatedAt(),
                dartProfile != null
                        ? List.of("dart-company", "dart-list", "dart-fnltt", "news-search")
                        : List.of("disclosure-api", "news-api", "finance-api")
        );

        CompanyDetailDto dto = new CompanyDetailDto(
                company.getId(),
                company.getName(),
                company.getTicker(),
                company.getIndustry(),
                company.getMarket(),
                company.getDataStatus(),
                company.getSummary() != null ? company.getSummary() : "",
                buildFinancials(company),
                company.getUpdatedAt(),
                coverage,
                provenance,
                company.needsSync(),
                company.getSyncStatus() != null ? company.getSyncStatus() : "FULL",
                dartProfile
        );
        return ApiResponse.ok(dto);
    }

    public ApiResponse<CompanyInsightsDto> getInsights(Long id) {
        findCompanyOrThrow(id);
        AgentInsightDto agentInsight = insightAgentService.findInsight(id);
        List<InsightCardDto> cards = agentInsight != null
                ? List.of()
                : insightCardRepository.findByCompanyIdOrderBySortOrderAsc(id).stream()
                        .map(this::toCardDto)
                        .toList();
        List<SignalDto> signals = resolveSignals(id);
        return ApiResponse.ok(new CompanyInsightsDto(cards, signals, agentInsight));
    }

    private Company findCompanyOrThrow(Long id) {
        if (id == null || id <= 0) {
            throw new GraphifyException(
                    "ERR_COMPANY_001",
                    "기업 정보를 찾을 수 없습니다.",
                    HttpStatus.NOT_FOUND
            );
        }
        return companyRepository.findById(id)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_001",
                        "기업 정보를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private InsightCardDto toCardDto(CompanyInsightCard card) {
        return new InsightCardDto(
                card.getId(),
                card.getCardType(),
                card.getTitle(),
                card.getSummary(),
                card.getConfidence(),
                card.getEvidence(),
                parseNodeIds(card.getHighlightNodeIds())
        );
    }

    private List<SignalDto> resolveSignals(Long companyId) {
        boolean hasDartSnapshot = dartSnapshotRepository.existsById(companyId);
        if (hasDartSnapshot) {
            List<CompanyAgentSignal> agentSignals = insightAgentService.findSignals(companyId);
            if (!agentSignals.isEmpty()) {
                return agentSignals.stream().map(this::toAgentSignalDto).toList();
            }
            return List.of();
        }
        return signalRepository.findByCompanyIdOrderBySortOrderAsc(companyId).stream()
                .map(this::toSeedSignalDto)
                .toList();
    }

    private SignalDto toAgentSignalDto(CompanyAgentSignal signal) {
        List<String> sources = signal.getSources() == null || signal.getSources().isBlank()
                ? List.of()
                : List.of(signal.getSources().split(",")).stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        return new SignalDto(
                signal.getLabel(),
                signal.getSignalKind(),
                List.of(),
                sources
        );
    }

    private SignalDto toSeedSignalDto(CompanySignal signal) {
        List<String> sources = signal.getSources() == null || signal.getSources().isBlank()
                ? List.of()
                : Arrays.stream(signal.getSources().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        return new SignalDto(
                signal.getLabel(),
                signal.getSignalKind(),
                parseNodeIds(signal.getRelatedNodeIds()),
                sources
        );
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

    private static Map<String, Integer> buildCoverage(List<CompanyInsightCard> cards) {
        Map<String, Integer> coverage = new LinkedHashMap<>();
        coverage.put("SUPPLY_CHAIN", 0);
        coverage.put("INVESTMENT", 0);
        coverage.put("PARTNERSHIP", 0);
        coverage.put("RISK", 0);
        for (CompanyInsightCard card : cards) {
            String type = card.getCardType();
            coverage.merge(type, 1, Integer::sum);
        }
        if (coverage.values().stream().allMatch(v -> v == 0)) {
            coverage.put("SUPPLY_CHAIN", 1);
        }
        return coverage;
    }

    private static FinancialSummaryDto buildFinancials(Company company) {
        String industry = company.getIndustry() != null ? company.getIndustry() : "";
        return switch (industry) {
            case "반도체" -> new FinancialSummaryDto("2024FY 추정", "302조 원", "12.4%", "8.1%");
            case "2차전지" -> new FinancialSummaryDto("2024FY 추정", "25조 원", "5.2%", "3.8%");
            case "자동차" -> new FinancialSummaryDto("2024FY 추정", "175조 원", "9.1%", "6.5%");
            case "인터넷" -> new FinancialSummaryDto("2024FY 추정", "12조 원", "18.3%", "14.2%");
            case "바이오" -> new FinancialSummaryDto("2024FY 추정", "3.2조 원", "22.1%", "11.0%");
            case "철강" -> new FinancialSummaryDto("2024FY 추정", "77조 원", "4.8%", "2.9%");
            default -> new FinancialSummaryDto("2024FY 추정", "—", "—", "—");
        };
    }
}
