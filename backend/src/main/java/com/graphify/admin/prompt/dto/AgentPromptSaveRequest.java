package com.graphify.admin.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentPromptSaveRequest(
        @NotBlank @Size(max = 64) String type,
        @NotBlank String systemPrompt,
        @NotBlank String taskTemplate,
        @Size(max = 255) String changeNote
) {
}
