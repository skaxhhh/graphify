package com.graphify.admin.mcp.dto;

public record McpToolPingResultDto(
        boolean ok,
        long latencyMs,
        String message
) {
}
