package com.graphify.company.dto;

import java.util.List;

public record InsightCardDto(
        Long id,
        String type,
        String title,
        String summary,
        String confidence,
        String evidence,
        List<String> highlightNodeIds
) {
}
