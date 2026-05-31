package com.graphify.company.dto;

import java.util.List;

public record CompanySearchDataDto(
        List<CompanySearchItemDto> items,
        SemanticHintsDto semanticHints
) {}
