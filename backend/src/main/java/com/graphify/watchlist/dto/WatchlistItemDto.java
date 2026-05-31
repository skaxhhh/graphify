package com.graphify.watchlist.dto;

import java.time.Instant;

public record WatchlistItemDto(
        Long companyId,
        String name,
        String industry,
        String ticker,
        Instant addedAt
) {
}
