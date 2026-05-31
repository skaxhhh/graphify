package com.graphify.admin.prompt.dto;

public record TokenUsageDto(
        int inputTokens,
        int outputTokens,
        int totalTokens
) {
}
