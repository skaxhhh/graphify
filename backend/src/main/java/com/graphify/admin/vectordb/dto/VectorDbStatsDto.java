package com.graphify.admin.vectordb.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record VectorDbStatsDto(
        long totalVectors,
        Map<String, Long> byType,
        long indexSizeBytes,
        BigDecimal avgLatencyMs,
        BigDecimal avgSimilarity,
        long requestCount24h,
        List<Number> latencySeries,
        List<Number> similaritySeries,
        List<Number> requestSeries,
        List<VectorDbJobSummaryDto> lastJobs,
        Instant updatedAt
) {
}
