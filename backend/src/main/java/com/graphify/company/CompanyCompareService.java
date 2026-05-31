package com.graphify.company;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.dto.CompanyCompareDataDto;
import com.graphify.company.dto.CompareCompanyDto;
import com.graphify.company.dto.CompareMetricsDto;
import com.graphify.company.dto.InsightCardDto;
import com.graphify.history.HistoryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyCompareService {

    private static final Set<String> ALLOWED_BASIS = Set.of(
            "INVESTMENT",
            "SUPPLY_CHAIN",
            "PARTNERSHIP"
    );

    private final CompanyRepository companyRepository;
    private final CompanyInsightCardRepository insightCardRepository;
    private final CompanySignalRepository signalRepository;
    private final RelationshipEdgeRepository edgeRepository;

    public CompanyCompareService(
            CompanyRepository companyRepository,
            CompanyInsightCardRepository insightCardRepository,
            CompanySignalRepository signalRepository,
            RelationshipEdgeRepository edgeRepository
    ) {
        this.companyRepository = companyRepository;
        this.insightCardRepository = insightCardRepository;
        this.signalRepository = signalRepository;
        this.edgeRepository = edgeRepository;
    }

    public ApiResponse<CompanyCompareDataDto> compare(String idsParam, String basisParam) {
        HistoryService.requireCurrentUserId();

        String basis = normalizeBasis(basisParam);
        List<Long> ids = parseIds(idsParam);

        if (ids.isEmpty()) {
            throw new GraphifyException(
                    "ERR_COMPARE_001",
                    "비교할 기업을 1개 이상 선택해 주세요.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (ids.size() > 3) {
            throw new GraphifyException(
                    "ERR_COMPARE_002",
                    "최대 3개 기업까지 비교할 수 있습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<CompareCompanyDto> companies = new ArrayList<>();
        for (Long companyId : ids) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new GraphifyException(
                            "ERR_COMPANY_001",
                            "기업 정보를 찾을 수 없습니다.",
                            HttpStatus.NOT_FOUND
                    ));

            List<InsightCardDto> cards = insightCardRepository
                    .findByCompanyIdOrderBySortOrderAsc(companyId)
                    .stream()
                    .filter(card -> basis.equals(card.getCardType()))
                    .map(this::toCardDto)
                    .toList();

            int signalCount = signalRepository.findByCompanyIdOrderBySortOrderAsc(companyId).size();
            int relationCount = edgeRepository.findByCompanyId(companyId).size();

            companies.add(new CompareCompanyDto(
                    company.getId(),
                    company.getName(),
                    company.getIndustry(),
                    cards,
                    new CompareMetricsDto(cards.size(), signalCount, relationCount)
            ));
        }

        return ApiResponse.ok(new CompanyCompareDataDto(basis, companies));
    }

    private static String normalizeBasis(String basis) {
        if (basis == null || basis.isBlank()) {
            return "INVESTMENT";
        }
        String normalized = basis.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_BASIS.contains(normalized)) {
            throw new GraphifyException(
                    "ERR_COMPARE_003",
                    "basis는 INVESTMENT, SUPPLY_CHAIN, PARTNERSHIP 중 하나여야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }

    private static List<Long> parseIds(String idsParam) {
        if (idsParam == null || idsParam.isBlank()) {
            return List.of();
        }
        Set<Long> ordered = new LinkedHashSet<>();
        Arrays.stream(idsParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(raw -> {
                    try {
                        long id = Long.parseLong(raw);
                        if (id > 0) {
                            ordered.add(id);
                        }
                    } catch (NumberFormatException ignored) {
                        // skip invalid
                    }
                });
        return ordered.stream().collect(Collectors.toList());
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

    private static List<String> parseNodeIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
