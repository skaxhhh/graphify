package com.graphify.company.dto;

public record FinancialSummaryDto(
        String periodLabel,
        String revenue,
        String operatingMargin,
        String netIncome
) {
}
