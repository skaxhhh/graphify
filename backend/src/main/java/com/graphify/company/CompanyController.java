package com.graphify.company;

import com.graphify.common.dto.ApiResponse;
import com.graphify.company.dart.CompanyInsightAgentService;
import com.graphify.company.market.CompanyMarketTechnicalService;
import com.graphify.company.dto.AgentInsightDto;
import com.graphify.company.dto.CompanyDetailDto;
import com.graphify.company.dto.CompanyGraphDto;
import com.graphify.company.dto.CompanyInsightsDto;
import com.graphify.company.dto.CompanyMarketTechnicalDto;
import com.graphify.company.dto.CompanyCompareDataDto;
import com.graphify.company.dto.CompanyResolveRequest;
import com.graphify.company.dto.CompanyResolveResultDto;
import com.graphify.company.dto.CompanySearchDataDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanySearchService companySearchService;
    private final CompanyDetailService companyDetailService;
    private final CompanyGraphService companyGraphService;
    private final CompanyCompareService companyCompareService;
    private final CompanyResolveService companyResolveService;
    private final CompanySyncService companySyncService;
    private final CompanyInsightAgentService companyInsightAgentService;
    private final CompanyMarketTechnicalService companyMarketTechnicalService;

    public CompanyController(
            CompanySearchService companySearchService,
            CompanyDetailService companyDetailService,
            CompanyGraphService companyGraphService,
            CompanyCompareService companyCompareService,
            CompanyResolveService companyResolveService,
            CompanySyncService companySyncService,
            CompanyInsightAgentService companyInsightAgentService,
            CompanyMarketTechnicalService companyMarketTechnicalService
    ) {
        this.companySearchService = companySearchService;
        this.companyDetailService = companyDetailService;
        this.companyGraphService = companyGraphService;
        this.companyCompareService = companyCompareService;
        this.companyResolveService = companyResolveService;
        this.companySyncService = companySyncService;
        this.companyInsightAgentService = companyInsightAgentService;
        this.companyMarketTechnicalService = companyMarketTechnicalService;
    }

    @GetMapping("/compare")
    public ApiResponse<CompanyCompareDataDto> compare(
            @RequestParam("ids") String ids,
            @RequestParam(value = "basis", defaultValue = "INVESTMENT") String basis
    ) {
        return companyCompareService.compare(ids, basis);
    }

    @GetMapping("/search")
    public ApiResponse<CompanySearchDataDto> search(
            @RequestParam("q") String q,
            @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "industry", required = false) String industry,
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "dataStatus", required = false) String dataStatus,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "enrich", defaultValue = "true") boolean enrich,
            @RequestParam(value = "enrichThreshold", required = false) Integer enrichThreshold
    ) {
        String resolvedIndustry = industry != null ? industry : parseFilterField(filter, "industry");
        String resolvedMarket = market != null ? market : parseFilterField(filter, "market");
        String resolvedDataStatus = dataStatus != null ? dataStatus : parseFilterField(filter, "dataStatus");

        return companySearchService.search(
                q,
                sort,
                resolvedIndustry,
                resolvedMarket,
                resolvedDataStatus,
                page,
                size,
                enrich,
                enrichThreshold
        );
    }

    @PostMapping("/resolve")
    public ApiResponse<CompanyResolveResultDto> resolve(@RequestBody CompanyResolveRequest request) {
        return companyResolveService.resolve(request);
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyDetailDto> getDetail(@PathVariable Long id) {
        return companyDetailService.getDetail(id);
    }

    @GetMapping("/{id}/insights")
    public ApiResponse<CompanyInsightsDto> getInsights(@PathVariable Long id) {
        return companyDetailService.getInsights(id);
    }

    @GetMapping("/{id}/market-technical")
    public ApiResponse<CompanyMarketTechnicalDto> getMarketTechnical(@PathVariable Long id) {
        return companyMarketTechnicalService.getMarketTechnical(id);
    }

    @PostMapping("/{id}/sync")
    public ApiResponse<CompanyDetailDto> syncDetail(@PathVariable Long id) {
        return companySyncService.syncDetail(id);
    }

    @PostMapping("/{id}/insights/generate")
    public ApiResponse<AgentInsightDto> generateInsights(@PathVariable Long id) {
        return companyInsightAgentService.generate(id);
    }

    @GetMapping("/{id}/graph")
    public ApiResponse<CompanyGraphDto> getGraph(
            @PathVariable Long id,
            @RequestParam(value = "depth", defaultValue = "2") int depth,
            @RequestParam(value = "filter", required = false) String filter
    ) {
        return companyGraphService.getGraph(id, depth, filter);
    }

    /**
     * filter 형식: industry:반도체 또는 industry=반도체 (단일 필드만 지원)
     */
    private static String parseFilterField(String filter, String field) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        String trimmed = filter.trim();
        String prefixColon = field + ":";
        String prefixEq = field + "=";
        if (trimmed.startsWith(prefixColon)) {
            return trimmed.substring(prefixColon.length()).trim();
        }
        if (trimmed.startsWith(prefixEq)) {
            return trimmed.substring(prefixEq.length()).trim();
        }
        if ("industry".equals(field) && !trimmed.contains(":") && !trimmed.contains("=")) {
            return trimmed;
        }
        return null;
    }
}
