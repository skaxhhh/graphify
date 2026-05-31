package com.graphify.admin.prompt.dto;

import java.util.List;

public record AgentPromptDetailDto(
        Long id,
        String type,
        String systemPrompt,
        String taskTemplate,
        List<AgentPromptVersionDto> versions
) {
}
