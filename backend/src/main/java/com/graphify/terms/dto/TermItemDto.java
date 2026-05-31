package com.graphify.terms.dto;

public record TermItemDto(
        Long id,
        String type,
        String title,
        String version,
        boolean required,
        String content
) {
}
