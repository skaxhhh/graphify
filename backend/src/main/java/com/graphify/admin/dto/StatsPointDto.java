package com.graphify.admin.dto;

import java.time.LocalDate;

public record StatsPointDto(
        LocalDate date,
        int runCount,
        long tokenUsage,
        int errorCount
) {
}
