package com.graphify.trading.paper.dto;

import java.time.Instant;

public record SignalLogItem(
        Long id,
        Long ruleId,
        String symbol,
        Instant ts,
        String signal,
        boolean executed,
        Double rsi14,
        Double sma20,
        Double price
) {}
