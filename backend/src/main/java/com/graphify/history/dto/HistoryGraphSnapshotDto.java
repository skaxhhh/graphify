package com.graphify.history.dto;

import com.graphify.company.dto.GraphEdgeDto;
import com.graphify.company.dto.GraphNodeDto;
import java.util.List;

public record HistoryGraphSnapshotDto(List<GraphNodeDto> nodes, List<GraphEdgeDto> edges) {
}
