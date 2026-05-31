package com.graphify.company.dto;

import java.util.List;

public record CompanyInsightsDto(
        List<InsightCardDto> cards,
        List<SignalDto> signals,
        AgentInsightDto agentInsight
) {
}
