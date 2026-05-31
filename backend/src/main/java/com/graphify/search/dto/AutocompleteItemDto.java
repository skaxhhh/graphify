package com.graphify.search.dto;

public record AutocompleteItemDto(
        Long id,
        String name,
        String ticker,
        String matchType
) {
}
