package com.graphify.trading.engine;

import java.util.List;

/**
 * RuleEvaluator 리치 반환 타입 — RULE-09.
 *
 * <p>진입/청산 평가 시 조건별 실제 지표값 + passed 상태, 청산 사유(exitReason)를 포착한다.
 * Plan 03(백테스트)와 Plan 04(모의 평가)가 동일 EvalResult를 기반으로 동일 JSON 스키마의
 * 매매 근거를 생성한다.
 *
 * <p>JSON 스키마 (rationale 블록):
 * <pre>
 * {
 *   "side": "BUY",
 *   "exitReason": null,
 *   "conditions": [
 *     {
 *       "expr": "RSI(14) < 30",
 *       "leftLabel": "RSI(14)",
 *       "leftValue": 28.57,
 *       "op": "<",
 *       "rightLabel": "30.0",
 *       "rightValue": 30.0,
 *       "passed": true
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>청산 시 exitReason = TAKE_PROFIT | STOP_LOSS | INDICATOR, exitPct = 변동률(TP/SL) or null(INDICATOR).
 */
public record EvalResult(
        boolean triggered,
        java.util.List<ConditionResult> conditions,
        ExitReason exitReason,   // null = 진입 평가
        Double exitPct           // null if not TP/SL
) {

    /**
     * 단일 조건 평가 결과.
     *
     * <p>expr 예시: "RSI(14) < 30", "EMA(9) crossAbove SMA(20)"
     * crossAbove/crossBelow: leftValue/rightValue는 현재 인덱스(i) 기준 값 (Pitfall 3).
     */
    public record ConditionResult(
            String expr,
            String leftLabel,
            double leftValue,
            String op,
            String rightLabel,
            double rightValue,
            boolean passed
    ) {}

    /** 청산 사유 — Plan 03/04가 JSON의 exitReason 필드로 직렬화한다. */
    public enum ExitReason {
        TAKE_PROFIT,
        STOP_LOSS,
        INDICATOR
    }
}
