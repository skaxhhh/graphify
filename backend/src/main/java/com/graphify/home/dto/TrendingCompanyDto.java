package com.graphify.home.dto;

public record TrendingCompanyDto(
        int rank,
        long companyId,
        String name,
        String ticker,
        String industry,
        long viewCount
) {
}
