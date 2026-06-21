package com.graphify.trading.paper.dto;

public record PaperPositionItem(
        String symbol,
        double qty,
        double avgPrice,
        double markPrice,
        double marketValue,
        double unrealizedPnl,
        double unrealizedPnlPct
) {}
