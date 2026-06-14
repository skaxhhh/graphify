package com.graphify.admin.dto;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String email,
        String displayName,
        String role,
        boolean tradingEnabled,
        boolean termsAccepted,
        Instant createdAt
) {}
