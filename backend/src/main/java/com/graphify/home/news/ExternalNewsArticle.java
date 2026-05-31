package com.graphify.home.news;

import java.time.Instant;

public record ExternalNewsArticle(
        String title,
        String summary,
        String sourceName,
        String sourceUrl,
        String ticker,
        String companyName,
        Instant publishedAt,
        String feedSource
) {
}
