package com.graphify.admin.dto;

public record UserUsageRowDto(
        Long userId,
        String name,
        String email,
        long requests,
        long tokens,
        long errors
) {
}
