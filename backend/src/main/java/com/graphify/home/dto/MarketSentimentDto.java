package com.graphify.home.dto;

import java.time.Instant;

public record MarketSentimentDto(
        MarketSentimentSnapshotDto kospi,
        MarketSentimentSnapshotDto nasdaq,
        Instant asOf
) {
}
