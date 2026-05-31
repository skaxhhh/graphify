package com.graphify.company.market;

import java.time.Instant;

public record NaverFinanceQuote(
        double price,
        double changePercent,
        double changeAmount,
        Instant quoteTime,
        String marketLabel
) {
}
