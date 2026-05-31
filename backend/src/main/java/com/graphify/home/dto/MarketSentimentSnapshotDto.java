package com.graphify.home.dto;

import java.time.Instant;
import java.util.List;

public record MarketSentimentSnapshotDto(
        double score,
        String zone,
        String zoneLabel,
        String market,
        List<SentimentIndicatorDto> indicators,
        Instant quoteTime,
        String dataSource,
        Double vix,
        Double vixMa50
) {
}
