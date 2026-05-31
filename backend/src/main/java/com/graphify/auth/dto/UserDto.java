package com.graphify.auth.dto;

public record UserDto(
        Long id,
        String email,
        String displayName,
        boolean termsAccepted,
        boolean isNewUser,
        String authProvider,
        String role
) {}
