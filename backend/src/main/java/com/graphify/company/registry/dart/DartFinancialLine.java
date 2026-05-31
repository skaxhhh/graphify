package com.graphify.company.registry.dart;

public record DartFinancialLine(
        String bsnsYear,
        String reprtCode,
        String reportLabel,
        String accountName,
        String currentAmount,
        String previousAmount,
        String currency
) {}
