package com.graphify.trading.paper.dto;

import java.util.List;

public record PaperDashboardDto(
        double cash,
        double totalEquity,
        double totalUnrealizedPnl,
        double todayRealizedPnl,
        int activePaperLiveRuleCount,
        List<PaperPositionItem> positions
) {
    public static PaperDashboardDto empty() {
        return new PaperDashboardDto(0, 0, 0, 0, 0, List.of());
    }
}
