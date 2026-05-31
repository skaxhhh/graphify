package com.graphify.company.dto;

import java.util.List;

public record SignalDto(
        String label,
        String kind,
        List<String> relatedNodeIds,
        List<String> sources
) {
}
