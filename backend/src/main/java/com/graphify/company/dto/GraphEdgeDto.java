package com.graphify.company.dto;

import java.time.Instant;

public record GraphEdgeDto(
        Long id,
        String source,
        String target,
        String relationType,
        double strength,
        String evidence,
        Instant updatedAt
) {
}
