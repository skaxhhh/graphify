package com.graphify.trading.backtest;

import static org.junit.jupiter.api.Assertions.*;

import com.graphify.trading.backtest.dto.BacktestResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Phase 1 인트라데이 백테스트 엔진 단위 테스트.
 * IntradayBacktestEngine의 package-private static helpers를 직접 호출.
 */
class BacktestServiceIntradayTest {

    @Test
    void testDrawdownSegments() {
        // CHART-02: 드로우다운 구간 계산 정확성
        // Equity curve: 100 → 90 (dd1 start) → 80 → 100 (dd1 end/recovery) → 70 (dd2 start)
        List<BacktestResult.EquityPoint> curve = List.of(
            new BacktestResult.EquityPoint(LocalDateTime.of(2026, 1, 2, 9, 0),  100.0),
            new BacktestResult.EquityPoint(LocalDateTime.of(2026, 1, 2, 9, 5),   90.0),
            new BacktestResult.EquityPoint(LocalDateTime.of(2026, 1, 2, 9, 10),  80.0),
            new BacktestResult.EquityPoint(LocalDateTime.of(2026, 1, 2, 9, 15), 100.0),
            new BacktestResult.EquityPoint(LocalDateTime.of(2026, 1, 2, 9, 20),  70.0)
        );

        List<BacktestResult.DrawdownSegment> segments =
                IntradayBacktestEngine.computeDrawdownSegments(curve);

        assertEquals(2, segments.size(), "Expected 2 drawdown segments");
        // Segment 1: starts at 9:05 (first dip below peak 100), ends at 9:15 (recovery to 100)
        assertEquals(LocalDateTime.of(2026, 1, 2, 9, 5),  segments.get(0).start());
        assertEquals(LocalDateTime.of(2026, 1, 2, 9, 15), segments.get(0).end());
        // Segment 2: starts at 9:20 (new low), never recovers → ends at last bar
        assertEquals(LocalDateTime.of(2026, 1, 2, 9, 20), segments.get(1).start());
    }

    @Test
    void testStatsCalculation() {
        // CHART-03: Sharpe/Sortino/Profit Factor 계산값 검증
        // Build a curve with strictly increasing equity (all positive returns → sharpe > 0)
        List<BacktestResult.EquityPoint> curve = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 2, 9, 0);
        for (int i = 0; i < 10; i++) {
            curve.add(new BacktestResult.EquityPoint(
                    base.plusMinutes(5L * i),
                    10_000_000.0 + i * 10_000.0));
        }

        // A single profitable SELL trade
        List<BacktestResult.TradeDto> trades = List.of(
            new BacktestResult.TradeDto(
                    base.plusMinutes(5), "005930.KS", "SELL", 1.0, 75000.0, 5000.0)
        );

        double sharpe   = IntradayBacktestEngine.computeSharpeRatio(curve);
        double sortino  = IntradayBacktestEngine.computeSortinoRatio(curve);
        double pf       = IntradayBacktestEngine.computeProfitFactor(trades);

        assertTrue(sharpe > 0,  "Sharpe must be positive for monotonically increasing equity");
        assertTrue(sortino >= 0, "Sortino must be >= 0 (no downside returns in this curve)");
        assertTrue(pf > 0,      "PF must be positive when only profit trades exist");
    }
}
