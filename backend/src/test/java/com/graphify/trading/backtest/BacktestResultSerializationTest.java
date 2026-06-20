package com.graphify.trading.backtest;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graphify.trading.backtest.dto.BacktestResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * BacktestResult JSON 직렬화 단위 테스트.
 * CHART-01: EquityPoint.datetime 필드가 ISO 문자열로 직렬화됨을 검증.
 */
class BacktestResultSerializationTest {

    @Test
    void equityPointSerializesDatetime() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        var point = new BacktestResult.EquityPoint(
                LocalDateTime.of(2026, 1, 2, 9, 0), 10_000_000.0);

        String json = mapper.writeValueAsString(point);

        assertTrue(json.contains("\"datetime\""),
                "Field must be 'datetime', got: " + json);
        assertTrue(json.contains("2026-01-02"),
                "Must contain the date portion, got: " + json);
        assertTrue(json.contains("09:00"),
                "Must contain the time portion, got: " + json);
        assertFalse(json.contains("\"date\""),
                "Old 'date' field must not appear, got: " + json);
    }
}
