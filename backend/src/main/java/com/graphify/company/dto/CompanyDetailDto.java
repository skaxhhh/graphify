package com.graphify.company.dto;

import java.time.Instant;
import java.util.Map;

public record CompanyDetailDto(
        Long id,
        String name,
        String ticker,
        String industry,
        String market,
        String dataStatus,
        String summary,
        FinancialSummaryDto financials,
        Instant lastUpdated,
        Map<String, Integer> coverageByRelationType,
        ProvenanceDto provenance,
        boolean needsSync,
        String syncStatus,
        CompanyDartProfileDto dartProfile
) {
}
