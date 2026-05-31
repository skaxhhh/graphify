package com.graphify.auth.dto;

public record PasswordResetRequestResponseDto(
        String message,
        String maskedEmail
) {}
