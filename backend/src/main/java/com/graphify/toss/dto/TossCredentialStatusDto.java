package com.graphify.toss.dto;

import java.time.Instant;

public record TossCredentialStatusDto(
        boolean configured,
        boolean tokenValid,
        Instant tokenExpiresAt
) {}
