package com.graphify.trading.paper.dto;

import java.time.Instant;
import java.util.List;

public record MonitorDto(
        Instant schedulerLastRun,   // max ts from paper_signal_log, or null
        String marketStatus,        // "OPEN" | "CLOSED"
        List<SignalLogItem> recentSignals,
        List<TradeItem> todayTrades
) {}
