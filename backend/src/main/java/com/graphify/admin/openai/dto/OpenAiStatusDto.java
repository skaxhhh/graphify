package com.graphify.admin.openai.dto;

import java.time.Instant;

public record OpenAiStatusDto(
        String connection,
        long tokensUsed,
        int rateLimitRemaining,
        Instant lastCheckedAt,
        String message
) {
}
