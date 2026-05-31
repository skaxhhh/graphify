package com.graphify.auth.dto;

import java.time.Instant;

public record PasswordResetValidateResponseDto(
        boolean valid,
        Instant expiresAt
) {}
