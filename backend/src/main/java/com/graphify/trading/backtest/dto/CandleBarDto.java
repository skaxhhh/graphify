package com.graphify.trading.backtest.dto;

import com.graphify.market.MarketBarIntraday;

public record CandleBarDto(long time, double open, double high, double low, double close, long volume) {

    public static CandleBarDto from(MarketBarIntraday bar) {
        return new CandleBarDto(
            bar.getTs().getEpochSecond(),
            bar.getOpen()  != null ? bar.getOpen()  : bar.getClose(),
            bar.getHigh()  != null ? bar.getHigh()  : bar.getClose(),
            bar.getLow()   != null ? bar.getLow()   : bar.getClose(),
            bar.getClose(),
            bar.getVolume() != null ? bar.getVolume() : 0L
        );
    }
}
