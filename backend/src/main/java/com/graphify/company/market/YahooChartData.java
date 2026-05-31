package com.graphify.company.market;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public record YahooChartData(
        String yahooSymbol,
        String currency,
        double regularMarketPrice,
        Instant quoteTime,
        ZoneId exchangeZone,
        double previousClose,
        List<DailyBar> dailyBars
) {
    public List<Double> dailyCloses() {
        return dailyBars.stream().map(DailyBar::close).toList();
    }
}
