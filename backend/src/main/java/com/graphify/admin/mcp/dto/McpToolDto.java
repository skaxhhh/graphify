package com.graphify.admin.mcp.dto;

import java.time.Instant;
import java.util.List;

public record McpToolDto(
        Long id,
        String name,
        String description,
        String endpointUrl,
        String authType,
        String schemaJson,
        String status,
        boolean enabled,
        List<String> allowedRoles,
        Instant lastCalledAt
) {
}
