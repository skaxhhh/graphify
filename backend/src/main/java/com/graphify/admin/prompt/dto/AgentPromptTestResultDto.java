package com.graphify.admin.prompt.dto;

public record AgentPromptTestResultDto(
        String output,
        TokenUsageDto tokenUsage,
        String companyName
) {
}
