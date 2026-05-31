package com.graphify.company.dto;

import java.util.List;

public record CompanyGraphDto(
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges,
        String sessionId,
        ProvenanceDto provenance
) {
}
