package com.graphify.company;

import com.graphify.common.dto.ApiMeta;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.dto.CompanySearchDataDto;
import com.graphify.company.dto.CompanySearchItemDto;
import com.graphify.company.dto.SemanticHintsDto;
import com.graphify.company.dto.SimilarCompanyDto;
import com.graphify.company.registry.CompanyRegistryClient;
import com.graphify.company.registry.ExternalCompanyCandidate;
import com.graphify.config.GraphifyDartProperties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanySearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final CompanyRepository companyRepository;
    private final CompanyRegistryClient companyRegistryClient;
    private final CompanyUpsertService companyUpsertService;
    private final GraphifyDartProperties dartProperties;

    public CompanySearchService(
            CompanyRepository companyRepository,
            CompanyRegistryClient companyRegistryClient,
            CompanyUpsertService companyUpsertService,
            GraphifyDartProperties dartProperties
    ) {
        this.companyRepository = companyRepository;
        this.companyRegistryClient = companyRegistryClient;
        this.companyUpsertService = companyUpsertService;
        this.dartProperties = dartProperties;
    }

    public ApiResponse<CompanySearchDataDto> search(
            String query,
            String sort,
            String industry,
            String market,
            String dataStatus,
            int page,
            Integer size,
            boolean enrich,
            Integer enrichThreshold
    ) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 1) {
            throw new GraphifyException(
                    "ERR_SEARCH_002",
                    "검색어를 입력해 주세요.",
                    HttpStatus.BAD_REQUEST
            );
        }

        int pageSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize, resolveSort(sort));

        Page<Company> resultPage = companyRepository.searchCompanies(
                normalizedQuery,
                emptyToNull(industry),
                emptyToNull(market),
                emptyToNull(dataStatus),
                pageable
        );

        Set<Long> externalIds = new HashSet<>();
        int threshold = enrichThreshold != null ? enrichThreshold : dartProperties.getEnrichThreshold();
        if (enrich && resultPage.getTotalElements() < threshold) {
            int externalLimit = Math.max(threshold, dartProperties.getSearchMaxResults());
            List<ExternalCompanyCandidate> external = companyRegistryClient.searchByKeyword(
                    normalizedQuery,
                    externalLimit
            );
            for (ExternalCompanyCandidate candidate : external) {
                CompanyUpsertResult saved = companyUpsertService.upsertFromCandidate(candidate);
                if (saved.created()) {
                    externalIds.add(saved.company().getId());
                }
            }
            if (!external.isEmpty()) {
                resultPage = companyRepository.searchCompanies(
                        normalizedQuery,
                        emptyToNull(industry),
                        emptyToNull(market),
                        emptyToNull(dataStatus),
                        pageable
                );
            }
        }

        Set<Long> finalExternalIds = externalIds;
        List<CompanySearchItemDto> items = resultPage.getContent().stream()
                .map(company -> toItemDto(company, finalExternalIds.contains(company.getId())))
                .toList();

        SemanticHintsDto hints = buildSemanticHints(normalizedQuery, items, industry);

        ApiMeta meta = new ApiMeta(resultPage.getNumber(), resultPage.getSize(), resultPage.getTotalElements());
        return ApiResponse.ok(new CompanySearchDataDto(items, hints), meta);
    }

    private SemanticHintsDto buildSemanticHints(
            String query,
            List<CompanySearchItemDto> items,
            String industryFilter
    ) {
        if (!items.isEmpty() && items.size() >= 3) {
            return new SemanticHintsDto(List.of(), List.of());
        }

        Set<String> relatedQueries = new LinkedHashSet<>();
        relatedQueries.add(query + " 관련주");
        if (industryFilter == null && !items.isEmpty() && items.get(0).industry() != null) {
            relatedQueries.add(items.get(0).industry() + " 업종");
        }

        List<SimilarCompanyDto> similar = new ArrayList<>();
        if (!items.isEmpty() && items.get(0).industry() != null) {
            companyRepository.findTop5ByIndustryAndIdNot(
                    items.get(0).industry(),
                    items.get(0).id()
            ).forEach(company -> similar.add(new SimilarCompanyDto(
                    company.getId(),
                    company.getName(),
                    company.getTicker()
            )));
        } else if (items.isEmpty()) {
            companyRepository.findTop5ByOrderByUpdatedAtDesc().forEach(company ->
                    similar.add(new SimilarCompanyDto(
                            company.getId(),
                            company.getName(),
                            company.getTicker()
                    )));
        }

        return new SemanticHintsDto(List.copyOf(relatedQueries), similar);
    }

    private CompanySearchItemDto toItemDto(Company company, boolean fromExternalEnrich) {
        String source = fromExternalEnrich ? "EXTERNAL" : "LOCAL";
        if (!fromExternalEnrich && company.getExternalSource() != null) {
            source = "LOCAL";
        }
        return new CompanySearchItemDto(
                company.getId(),
                company.getName(),
                company.getTicker(),
                company.getIndustry(),
                company.getMarket(),
                company.getDataStatus(),
                company.getUpdatedAt(),
                source,
                company.getSyncStatus() != null ? company.getSyncStatus() : "FULL"
        );
    }

    private static Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by("name").ascending();
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "industry" -> Sort.by("industry").ascending().and(Sort.by("name").ascending());
            case "updatedat", "updated_at" -> Sort.by("updatedAt").descending();
            default -> Sort.by("name").ascending();
        };
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
