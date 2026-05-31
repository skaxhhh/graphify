package com.graphify.admin.prompt.dto;

import jakarta.validation.constraints.NotNull;

public record AgentPromptRollbackRequest(
        @NotNull Long targetVersionId
) {
}
