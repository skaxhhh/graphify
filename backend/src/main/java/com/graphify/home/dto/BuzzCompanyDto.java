package com.graphify.home.dto;

public record BuzzCompanyDto(
        int rank,
        Long companyId,
        String name,
        String ticker,
        String industry,
        Double price,
        String priceDirection,
        String sourceLabel
) {
}
