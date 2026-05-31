package com.graphify.company;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.CompanyUpsertResult;
import com.graphify.company.dto.CompanyResolveRequest;
import com.graphify.company.dto.CompanyResolveResultDto;
import com.graphify.company.registry.CompanyRegistryClient;
import com.graphify.company.registry.ExternalCompanyCandidate;
import com.graphify.company.registry.ExternalCompanyProfile;
import com.graphify.company.registry.dart.DartCompanyRegistryClient;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyResolveService {

    private final CompanyRepository companyRepository;
    private final CompanyRegistryClient companyRegistryClient;
    private final CompanyUpsertService companyUpsertService;
    private final DartCompanyRegistryClient dartCompanyRegistryClient;

    public CompanyResolveService(
            CompanyRepository companyRepository,
            CompanyRegistryClient companyRegistryClient,
            CompanyUpsertService companyUpsertService,
            DartCompanyRegistryClient dartCompanyRegistryClient
    ) {
        this.companyRepository = companyRepository;
        this.companyRegistryClient = companyRegistryClient;
        this.companyUpsertService = companyUpsertService;
        this.dartCompanyRegistryClient = dartCompanyRegistryClient;
    }

    @Transactional
    public ApiResponse<CompanyResolveResultDto> resolve(CompanyResolveRequest request) {
        String query = normalize(request.query());
        String ticker = normalize(request.ticker());
        String externalSource = normalize(request.externalSource());
        String externalId = normalize(request.externalId());

        if (query == null && ticker == null && externalId == null) {
            throw new GraphifyException(
                    "ERR_SEARCH_004",
                    "query, ticker, externalId 중 하나는 필수입니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        Optional<Company> existing = findInDatabase(ticker, externalSource, externalId, query);
        if (existing.isPresent()) {
            return ApiResponse.ok(toResult(existing.get(), false));
        }

        Optional<ExternalCompanyProfile> profile = resolveExternalProfile(
                query,
                ticker,
                externalSource,
                externalId
        );
        if (profile.isPresent()) {
            Company saved = companyUpsertService.upsertFromProfile(profile.get());
            return ApiResponse.ok(toResult(saved, true));
        }

        List<ExternalCompanyCandidate> candidates = companyRegistryClient.searchByKeyword(
                firstNonNull(query, ticker, ""),
                1
        );
        if (!candidates.isEmpty()) {
            CompanyUpsertResult saved = companyUpsertService.upsertFromCandidate(candidates.getFirst());
            return ApiResponse.ok(toResult(saved.company(), saved.created()));
        }

        throw new GraphifyException(
                "ERR_SEARCH_003",
                "일치하는 기업을 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
        );
    }

    private Optional<Company> findInDatabase(
            String ticker,
            String externalSource,
            String externalId,
            String query
    ) {
        if (ticker != null) {
            Optional<Company> byTicker = companyRepository.findByTicker(ticker);
            if (byTicker.isPresent()) {
                return byTicker;
            }
        }
        if (externalSource != null && externalId != null) {
            Optional<Company> byExternal = companyRepository.findByExternalSourceAndExternalId(externalSource, externalId);
            if (byExternal.isPresent()) {
                return byExternal;
            }
        }
        if (query != null) {
            return companyRepository.findFirstByNameIgnoreCase(query);
        }
        return Optional.empty();
    }

    private Optional<ExternalCompanyProfile> resolveExternalProfile(
            String query,
            String ticker,
            String externalSource,
            String externalId
    ) {
        if (externalSource != null && externalId != null) {
            Optional<ExternalCompanyProfile> byExternal = companyRegistryClient.findByExternalId(externalSource, externalId);
            if (byExternal.isPresent()) {
                return byExternal;
            }
        }
        if (ticker != null) {
            Optional<ExternalCompanyProfile> byTicker = companyRegistryClient.findByTicker(ticker);
            if (byTicker.isPresent()) {
                return byTicker;
            }
        }
        if (externalId != null) {
            return dartCompanyRegistryClient.fetchProfile(externalId);
        }
        if (query != null) {
            List<ExternalCompanyCandidate> candidates = companyRegistryClient.searchByKeyword(query, 1);
            if (!candidates.isEmpty()) {
                return dartCompanyRegistryClient.fetchProfile(candidates.getFirst().externalId());
            }
        }
        return Optional.empty();
    }

    private static CompanyResolveResultDto toResult(Company company, boolean created) {
        return new CompanyResolveResultDto(
                company.getId(),
                company.getName(),
                company.getTicker(),
                company.getSyncStatus(),
                created
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String firstNonNull(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }
}
