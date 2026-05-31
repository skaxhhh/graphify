package com.graphify.company.dto;

import java.time.Instant;

public record AgentInsightDto(
        String content,
        String modelLabel,
        String status,
        Instant generatedAt
) {}
