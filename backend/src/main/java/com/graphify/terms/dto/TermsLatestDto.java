package com.graphify.terms.dto;

import java.util.List;

public record TermsLatestDto(
        String version,
        List<TermItemDto> terms,
        long companyCount
) {
}
