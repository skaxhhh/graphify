package com.graphify.company.dto;

import java.time.Instant;
import java.util.List;

public record ProvenanceDto(
        List<String> sources,
        Instant lastUpdated,
        List<String> mcpToolsUsed
) {
}
