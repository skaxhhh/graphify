package com.graphify.admin.vectordb.dto;

import java.time.Instant;

public record EmbeddingJobDto(
        long jobId,
        String jobType,
        String scope,
        String status,
        int progress,
        String message,
        Instant createdAt,
        Instant completedAt
) {
}
