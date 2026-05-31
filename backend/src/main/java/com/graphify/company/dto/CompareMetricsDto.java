package com.graphify.company.dto;

public record CompareMetricsDto(
        int insightCount,
        int signalCount,
        int relationCount
) {
}
