package com.graphify.trading.backtest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 인트라데이 백테스트 엔진 단위 테스트.
 * Wave 0: RED 상태 — plan 02 구현 후 GREEN으로 전환.
 */
class BacktestServiceIntradayTest {

    @Test
    void testDrawdownSegments() {
        // CHART-02: 드로우다운 구간 계산 정확성
        // TODO plan 02: IntradayBacktestEngine.computeDrawdownSegments() 구현 후 완성
        fail("RED stub — implement after plan 02");
    }

    @Test
    void testStatsCalculation() {
        // CHART-03: Sharpe/Sortino/Profit Factor 계산값 검증
        // TODO plan 02: BacktestService.buildResult() sharpe/sortino/pf 추가 후 완성
        fail("RED stub — implement after plan 02");
    }
}
