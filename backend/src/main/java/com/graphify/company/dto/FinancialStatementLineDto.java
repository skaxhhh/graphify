package com.graphify.company.dto;

public record FinancialStatementLineDto(
        String bsnsYear,
        String reprtCode,
        String reportLabel,
        String accountName,
        String currentAmount,
        String previousAmount,
        String currency
) {}
