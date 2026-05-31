package com.graphify.company.dto;

import java.time.Instant;

public record CompanyNewsItemDto(
        String title,
        String summary,
        String sourceName,
        String sourceUrl,
        Instant publishedAt
) {}
