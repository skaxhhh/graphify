package com.graphify.admin.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record McpToolUpsertRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 512) String endpointUrl,
        @NotBlank @Size(max = 32) String authType,
        @Size(max = 512) String authSecret,
        String schemaJson,
        Boolean enabled,
        List<@NotBlank @Size(max = 32) String> allowedRoles
) {
}
