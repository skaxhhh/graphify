package com.graphify.trading.backtest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

/**
 * 백테스트 요청. ruleId(저장된 룰) 또는 definition(즉석 룰) 중 하나를 지정.
 * from/to(미지정 시 전체 구간), initialCash(미지정 시 기본값).
 * timeFrom/timeTo: KST 시작/종료 시각 (예: "09:00", "12:00"), null이면 서비스 기본값 사용.
 * overrideSymbols: 비어있지 않으면 유니버스 타입 무관하게 이 종목들을 사용(volume_top_n 자동해석 우회).
 */
public record BacktestRequest(
        Long ruleId,
        JsonNode definition,
        LocalDate from,
        LocalDate to,
        Double initialCash,
        String timeFrom,         // KST 시작 시각, 기본 "09:00" (nullable → service uses default)
        String timeTo,           // KST 종료 시각, 기본 "12:00" (nullable → service uses default)
        List<String> overrideSymbols  // 사용자 직접 선택 유니버스 (nullable → 기존 유니버스 해석)
) {
}
