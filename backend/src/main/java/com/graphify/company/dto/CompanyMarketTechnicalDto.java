package com.graphify.company.dto;

import com.graphify.company.market.MaAlignment;
import java.time.Instant;

public record CompanyMarketTechnicalDto(
        String yahooSymbol,
        String currency,
        Double price,
        Double changePercent,
        Double previousClose,
        Double ma5,
        Double ma20,
        Double ma60,
        Double ma120,
        Double ma240,
        Double rsi14,
        MaAlignment maAlignment,
        boolean shortTermRise5,
        boolean shortTermRise20,
        Instant quoteTime,
        String tradingDate,
        String priceKind,
        String priceLabel,
        Instant asOf,
        String priceSource,
        String historySource
) {
}
