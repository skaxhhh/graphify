package com.graphify.history.dto;

import com.graphify.company.dto.InsightCardDto;
import com.graphify.company.dto.SignalDto;
import java.time.Instant;
import java.util.List;

public record HistoryDetailDto(
        String sessionId,
        HistoryCompanyDto company,
        Instant analyzedAt,
        String status,
        String summaryLine,
        List<TimelineEventDto> timeline,
        HistoryGraphSnapshotDto graphSnapshot,
        List<InsightCardDto> insights,
        List<SignalDto> signals,
        DiffSummaryDto diffSummary
) {
}
