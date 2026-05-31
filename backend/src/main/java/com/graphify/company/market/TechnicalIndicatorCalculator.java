package com.graphify.company.market;

import java.util.List;

public final class TechnicalIndicatorCalculator {

    private TechnicalIndicatorCalculator() {
    }

    public static double sma(List<Double> closes, int period) {
        if (closes == null || closes.size() < period || period <= 0) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = closes.size() - period; i < closes.size(); i++) {
            sum += closes.get(i);
        }
        return sum / period;
    }

    public static double rsi(List<Double> closes, int period) {
        if (closes == null || closes.size() < period + 1 || period <= 0) {
            return Double.NaN;
        }
        double gainSum = 0;
        double lossSum = 0;
        for (int i = closes.size() - period; i < closes.size(); i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change >= 0) {
                gainSum += change;
            } else {
                lossSum += -change;
            }
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        if (avgLoss == 0) {
            return 100;
        }
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    public static MaAlignment resolveMaAlignment(double ma60, double ma120, double ma240) {
        if (Double.isNaN(ma60) || Double.isNaN(ma120) || Double.isNaN(ma240)) {
            return MaAlignment.MIXED;
        }
        if (ma60 > ma120 && ma120 > ma240) {
            return MaAlignment.BULLISH_ALIGN;
        }
        if (ma60 < ma120 && ma120 < ma240) {
            return MaAlignment.BEARISH_ALIGN;
        }
        return MaAlignment.MIXED;
    }

    public static double changePercent(double latestPrice, double previousClose) {
        if (Double.isNaN(latestPrice) || Double.isNaN(previousClose) || previousClose == 0) {
            return Double.NaN;
        }
        return ((latestPrice - previousClose) / previousClose) * 100.0;
    }
}
