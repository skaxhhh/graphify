package com.graphify.company.dto;

import java.util.List;

public record CompanyDartProfileDto(
        String corpName,
        String stockCode,
        String ceoName,
        String corpClassLabel,
        String address,
        String homepage,
        String industryCode,
        String estDate,
        String accMonth,
        String bizrNo,
        List<DisclosureSummaryDto> recentDisclosures,
        List<FinancialStatementLineDto> financialStatements,
        List<CompanyNewsItemDto> relatedNews,
        String collectedAt
) {}
