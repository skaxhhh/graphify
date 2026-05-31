package com.graphify.company.dto;

import java.util.List;

public record CompareCompanyDto(
        Long companyId,
        String name,
        String industry,
        List<InsightCardDto> insightCards,
        CompareMetricsDto metrics
) {
}
