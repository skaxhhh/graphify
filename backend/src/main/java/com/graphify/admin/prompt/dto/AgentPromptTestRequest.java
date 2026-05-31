package com.graphify.admin.prompt.dto;

import jakarta.validation.constraints.NotNull;

public record AgentPromptTestRequest(
        @NotNull Long companyId,
        String sampleInput
) {
}
