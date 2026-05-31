package com.graphify.company.dto;

import java.time.Instant;

public record CompanySearchItemDto(
        Long id,
        String name,
        String ticker,
        String industry,
        String market,
        String dataFreshness,
        Instant updatedAt,
        String source,
        String syncStatus
) {}
