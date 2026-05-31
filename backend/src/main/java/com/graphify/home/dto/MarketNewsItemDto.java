package com.graphify.home.dto;

import java.time.Instant;

public record MarketNewsItemDto(
        long id,
        String title,
        String summary,
        String sourceName,
        String sourceUrl,
        String ticker,
        String companyName,
        Instant publishedAt
) {
}
