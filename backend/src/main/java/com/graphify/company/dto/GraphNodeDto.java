package com.graphify.company.dto;

public record GraphNodeDto(
        String id,
        String label,
        String type,
        String summary,
        int degree,
        String clusterId
) {
}
