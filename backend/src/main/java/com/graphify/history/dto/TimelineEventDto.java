package com.graphify.history.dto;

import java.time.Instant;
import java.util.Map;

public record TimelineEventDto(
        Instant t,
        String eventType,
        String label,
        Map<String, Object> payload
) {
}
