package com.graphify.admin.dto;

import java.util.List;

public record AdminAgentStatsDto(
        long runCount,
        long avgDurationMs,
        long tokenUsage,
        double errorRate,
        String period,
        List<StatsPointDto> series,
        List<AdminAlertDto> alerts
) {
}
