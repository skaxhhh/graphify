package com.graphify.history.dto;

import java.time.Instant;

public record HistoryItemDto(
        String sessionId,
        Long companyId,
        String companyName,
        Instant analyzedAt,
        String status,
        String summaryLine
) {
}
