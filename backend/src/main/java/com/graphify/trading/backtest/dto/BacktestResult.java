package com.graphify.trading.backtest.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BacktestResult(
        double initialCash,
        double finalEquity,
        double returnPct,
        double maxDrawdownPct,
        double winRate,
        int tradeCount,
        double sharpeRatio,
        double sortinoRatio,
        double profitFactor,
        List<DrawdownSegment> drawdownSegments,
        List<TradeDto> trades,
        List<EquityPoint> equityCurve
) {

    public record TradeDto(
            LocalDateTime datetime,
            String symbol,
            String side,
            double qty,
            double price,
            Double pnl
    ) {}

    public record EquityPoint(LocalDateTime datetime, double equity) {}

    public record DrawdownSegment(LocalDateTime start, LocalDateTime end) {}
}
