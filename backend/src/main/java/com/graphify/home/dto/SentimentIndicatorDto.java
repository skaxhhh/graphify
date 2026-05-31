package com.graphify.home.dto;

public record SentimentIndicatorDto(
        String id,
        String name,
        String description,
        double score,
        String signal
) {
}
