package com.graphify.trading.paper.dto;

import java.time.Instant;

public record TradeItem(
        Long id,
        String symbol,
        String side,
        double qty,
        double price,
        Double pnl,
        Instant tradedAt
) {}
