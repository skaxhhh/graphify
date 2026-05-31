package com.graphify.auth.dto;

public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        UserDto user
) {}
