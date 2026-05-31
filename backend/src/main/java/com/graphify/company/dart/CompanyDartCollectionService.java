package com.graphify.company.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.company.CompanyUpsertService;
import com.graphify.company.registry.ExternalCompanyProfile;
import com.graphify.company.news.CompanyNewsSearchService;
import com.graphify.company.registry.dart.DartCompanyRegistryClient;
import com.graphify.company.registry.dart.DartDisclosureClient;
import com.graphify.company.registry.dart.DartDisclosureItem;
import com.graphify.company.registry.dart.DartFinancialClient;
import com.graphify.company.registry.dart.DartFinancialLine;
import com.graphify.home.news.ExternalNewsArticle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyDartCollectionService {

    private final CompanyRepository companyRepository;
    private final CompanyUpsertService companyUpsertService;
    private final DartCompanyRegistryClient dartCompanyRegistryClient;
    private final DartDisclosureClient dartDisclosureClient;
    private final DartFinancialClient dartFinancialClient;
    private final CompanyNewsSearchService companyNewsSearchService;
    private final CompanyDartSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public CompanyDartCollectionService(
            CompanyRepository companyRepository,
            CompanyUpsertService companyUpsertService,
            DartCompanyRegistryClient dartCompanyRegistryClient,
            DartDisclosureClient dartDisclosureClient,
            DartFinancialClient dartFinancialClient,
            CompanyNewsSearchService companyNewsSearchService,
            CompanyDartSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper
    ) {
        this.companyRepository = companyRepository;
        this.companyUpsertService = companyUpsertService;
        this.dartCompanyRegistryClient = dartCompanyRegistryClient;
        this.dartDisclosureClient = dartDisclosureClient;
        this.dartFinancialClient = dartFinancialClient;
        this.companyNewsSearchService = companyNewsSearchService;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Company collectFromDart(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_COMPANY_001",
                        "기업 정보를 찾을 수 없습니다.",
                        HttpStatus.NOT_FOUND
                ));

        if (company.getExternalId() == null || !"DART".equalsIgnoreCase(company.getExternalSource())) {
            throw new GraphifyException(
                    "ERR_COMPANY_002",
                    "DART 연동 기업만 수집할 수 있습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        String corpCode = company.getExternalId().trim();
        Optional<JsonNode> profileNode = dartCompanyRegistryClient.fetchCompanyJson(corpCode);
        if (profileNode.isEmpty()) {
            throw new GraphifyException(
                    "ERR_COMPANY_003",
                    "DART에서 기업개황을 가져오지 못했습니다.",
                    HttpStatus.BAD_GATEWAY
            );
        }

        Optional<ExternalCompanyProfile> profile = dartCompanyRegistryClient.mapProfile(profileNode.get(), corpCode);
        profile.ifPresent(companyUpsertService::upsertFromProfile);

        List<DartDisclosureItem> disclosures = dartDisclosureClient.fetchRecentDisclosures(corpCode, 100, 6);
        List<DartFinancialLine> financials = dartFinancialClient.fetchRecentFinancials(corpCode);
        List<ExternalNewsArticle> news = companyNewsSearchService.searchForCompany(
                company.getName(),
                company.getTicker()
        );
        saveSnapshot(companyId, corpCode, profileNode.get(), disclosures, financials, news);

        Company refreshed = companyRepository.findById(companyId).orElse(company);
        refreshed.setSyncStatus("FULL");
        refreshed.setDataStatus("FRESH");
        refreshed.setDetailSyncedAt(Instant.now());
        refreshed.setUpdatedAt(Instant.now());
        return companyRepository.save(refreshed);
    }

    private void saveSnapshot(
            Long companyId,
            String corpCode,
            JsonNode profileNode,
            List<DartDisclosureItem> disclosures,
            List<DartFinancialLine> financials,
            List<ExternalNewsArticle> news
    ) {
        try {
            String profileJson = objectMapper.writeValueAsString(profileNode);
            String disclosuresJson = objectMapper.writeValueAsString(disclosures);
            String financialsJson = objectMapper.writeValueAsString(financials);
            String newsJson = objectMapper.writeValueAsString(news);

            CompanyDartSnapshot snapshot = snapshotRepository.findById(companyId).orElseGet(CompanyDartSnapshot::new);
            snapshot.setCompanyId(companyId);
            snapshot.setCorpCode(corpCode);
            snapshot.setProfileJson(profileJson);
            snapshot.setDisclosuresJson(disclosuresJson);
            snapshot.setFinancialsJson(financialsJson);
            snapshot.setNewsJson(newsJson);
            snapshot.setCollectedAt(Instant.now());
            snapshotRepository.save(snapshot);
        } catch (Exception ex) {
            throw new GraphifyException(
                    "ERR_COMPANY_004",
                    "수집 데이터 저장에 실패했습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
