package com.graphify.company.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TechnicalIndicatorCalculatorTest {

    @Test
    void sma_returnsAverageOfLastPeriod() {
        List<Double> closes = List.of(10.0, 11.0, 12.0, 13.0, 14.0);
        assertThat(TechnicalIndicatorCalculator.sma(closes, 3)).isEqualTo(13.0);
    }

    @Test
    void resolveMaAlignment_detectsBullishAndBearish() {
        assertThat(TechnicalIndicatorCalculator.resolveMaAlignment(300, 280, 250))
                .isEqualTo(MaAlignment.BULLISH_ALIGN);
        assertThat(TechnicalIndicatorCalculator.resolveMaAlignment(250, 280, 300))
                .isEqualTo(MaAlignment.BEARISH_ALIGN);
        assertThat(TechnicalIndicatorCalculator.resolveMaAlignment(300, 290, 300))
                .isEqualTo(MaAlignment.MIXED);
    }

    @Test
    void rsi_returnsValueBetweenZeroAndHundred() {
        List<Double> closes = new ArrayList<>();
        double price = 100;
        for (int i = 0; i < 30; i++) {
            price += (i % 3 == 0) ? -1 : 1.5;
            closes.add(price);
        }
        double rsi = TechnicalIndicatorCalculator.rsi(closes, 14);
        assertThat(rsi).isBetween(0.0, 100.0);
    }

    @Test
    void changePercent_usesExplicitPreviousClose() {
        assertThat(TechnicalIndicatorCalculator.changePercent(110.0, 100.0)).isEqualTo(10.0);
    }
}
