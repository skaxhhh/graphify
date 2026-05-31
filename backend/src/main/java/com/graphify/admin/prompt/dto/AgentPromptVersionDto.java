package com.graphify.admin.prompt.dto;

import java.time.Instant;

public record AgentPromptVersionDto(
        Long id,
        int versionNumber,
        Instant createdAt,
        String author,
        String summary,
        String changeNote
) {
}
