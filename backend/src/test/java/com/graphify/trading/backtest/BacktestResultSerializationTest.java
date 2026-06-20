package com.graphify.trading.backtest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * BacktestResult JSON 직렬화 단위 테스트.
 * Wave 0: RED 상태 — plan 02에서 EquityPoint.datetime(LocalDateTime) 변경 후 GREEN으로 전환.
 */
class BacktestResultSerializationTest {

    @Test
    void equityPointSerializesDatetime() {
        // CHART-01: EquityPoint.datetime 필드가 ISO 문자열로 직렬화됨을 검증
        // TODO plan 02: EquityPoint.date(LocalDate) → datetime(LocalDateTime) 변경 후 완성
        fail("RED stub — implement after plan 02");
    }
}
