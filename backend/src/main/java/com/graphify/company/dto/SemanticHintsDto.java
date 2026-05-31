package com.graphify.company.dto;

import java.util.List;

public record SemanticHintsDto(
        List<String> relatedQueries,
        List<SimilarCompanyDto> similarCompanies
) {}
