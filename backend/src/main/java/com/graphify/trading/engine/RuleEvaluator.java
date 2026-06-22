package com.graphify.trading.engine;

import com.graphify.trading.engine.EvalResult.ConditionResult;
import com.graphify.trading.engine.EvalResult.ExitReason;
import com.graphify.trading.rule.definition.RuleDefinition.Condition;
import com.graphify.trading.rule.definition.RuleDefinition.ConditionGroup;
import com.graphify.trading.rule.definition.RuleDefinition.ExitSpec;
import com.graphify.trading.rule.definition.RuleDefinition.Operand;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 룰 definition + 종가 시계열 → 진입/청산 조건 평가. 백테스트·포워드 공용.
 * 데이터 소스가 종가만 제공하므로 VOLUME 등 미지원 지표는 NaN → 조건 false 로 처리.
 *
 * <p>리치 반환 타입 ({@link EvalResult}):
 * <ul>
 *   <li>{@link #evalEntry} — 조건별 실제 지표값 + passed 포착 (RULE-09)</li>
 *   <li>{@link #evalExit}  — TP/SL/지표 청산 사유 구분 (RULE-09)</li>
 * </ul>
 *
 * <p>기존 boolean 시그니처는 리치 메서드로 위임되어 하위 호환을 유지한다.
 */
@Component
public class RuleEvaluator {

    // ─── 기존 boolean 시그니처 (하위 호환 — 리치 메서드로 위임) ───────────────────

    /** index i 에서 진입 그룹이 충족되는가 */
    public boolean entryTriggered(ConditionGroup entry, double[] closes, Double[] volumes, int i) {
        return evalEntry(entry, closes, volumes, i).triggered();
    }

    /**
     * index i 에서 청산 조건이 충족되는가.
     * exit.conditions(logic) 또는 takeProfit/stopLoss(진입가 대비) 중 하나라도 충족 시 true.
     */
    public boolean exitTriggered(ExitSpec exit, double[] closes, Double[] volumes, int i, double entryPrice) {
        return evalExit(exit, closes, volumes, i, entryPrice).triggered();
    }

    // ─── 리치 반환 타입 메서드 (RULE-09) ──────────────────────────────────────────

    /**
     * 진입 그룹 리치 평가.
     *
     * @return EvalResult(triggered, conditions[], exitReason=null, exitPct=null)
     */
    public EvalResult evalEntry(ConditionGroup entry, double[] closes, Double[] volumes, int i) {
        if (entry == null || entry.conditions() == null || entry.conditions().isEmpty()) {
            return new EvalResult(false, List.of(), null, null);
        }
        List<ConditionResult> results = evaluateGroupRich(entry.conditions(), closes, volumes, i);
        boolean triggered = applyLogic(entry.logic(), results);
        return new EvalResult(triggered, results, null, null);
    }

    /**
     * 청산 조건 리치 평가.
     *
     * <p>우선순위: TP → SL → 지표 조건.
     * TP/SL은 exitPct(변동률)를 함께 반환한다. 지표 청산은 exitPct=null.
     *
     * @return EvalResult(triggered, conditions[], exitReason, exitPct)
     */
    public EvalResult evalExit(ExitSpec exit, double[] closes, Double[] volumes, int i, double entryPrice) {
        if (exit == null) {
            return new EvalResult(false, List.of(), null, null);
        }
        double price = closes[i];

        // 1. Take-profit 체크
        if (exit.takeProfitPct() != null && entryPrice > 0) {
            double changePct = (price - entryPrice) / entryPrice * 100.0;
            if (changePct >= exit.takeProfitPct()) {
                return new EvalResult(true, List.of(), ExitReason.TAKE_PROFIT, changePct);
            }
        }

        // 2. Stop-loss 체크
        if (exit.stopLossPct() != null && entryPrice > 0) {
            double changePct = (price - entryPrice) / entryPrice * 100.0;
            if (changePct <= exit.stopLossPct()) {
                return new EvalResult(true, List.of(), ExitReason.STOP_LOSS, changePct);
            }
        }

        // 3. 지표 조건 체크
        if (exit.conditions() != null && !exit.conditions().isEmpty()) {
            List<ConditionResult> results = evaluateGroupRich(exit.conditions(), closes, volumes, i);
            boolean triggered = applyLogic(exit.logic(), results);
            if (triggered) {
                return new EvalResult(true, results, ExitReason.INDICATOR, null);
            }
            return new EvalResult(false, results, null, null);
        }

        return new EvalResult(false, List.of(), null, null);
    }

    // ─── 내부 리치 평가 헬퍼 ──────────────────────────────────────────────────────

    /**
     * 조건 목록을 개별 평가해 ConditionResult 리스트를 반환한다.
     * 각 결과에는 expr, leftLabel/leftValue, op, rightLabel/rightValue, passed가 포함된다.
     */
    private List<ConditionResult> evaluateGroupRich(
            List<Condition> conditions, double[] closes, Double[] volumes, int i) {
        List<ConditionResult> results = new ArrayList<>(conditions.size());
        for (Condition c : conditions) {
            results.add(evaluateConditionRich(c, closes, volumes, i));
        }
        return results;
    }

    /**
     * 단일 조건 리치 평가.
     *
     * <p>crossAbove/crossBelow: leftValue/rightValue = 현재 인덱스 값 (Pitfall 3).
     * expr = leftLabel + " " + op + " " + rightLabel.
     */
    private ConditionResult evaluateConditionRich(Condition c, double[] closes, Double[] volumes, int i) {
        if (c == null || c.op() == null) {
            return new ConditionResult("null < null", "null", Double.NaN, "<", "null", Double.NaN, false);
        }
        String op = c.op();
        String leftLabel = operandLabel(c.left());
        String rightLabel = operandLabel(c.right());

        if ("crossAbove".equals(op) || "crossBelow".equals(op)) {
            // crossAbove/crossBelow: 현재 인덱스 값 기록 (Pitfall 3)
            double leftValue = Double.NaN;
            double rightValue = Double.NaN;
            boolean passed = false;
            if (i >= 1) {
                double lPrev = operand(c.left(), closes, volumes, i - 1);
                double rPrev = operand(c.right(), closes, volumes, i - 1);
                double lNow = operand(c.left(), closes, volumes, i);
                double rNow = operand(c.right(), closes, volumes, i);
                leftValue = lNow;
                rightValue = rNow;
                if (!anyNaN(lPrev, rPrev, lNow, rNow)) {
                    passed = "crossAbove".equals(op)
                            ? (lPrev <= rPrev && lNow > rNow)
                            : (lPrev >= rPrev && lNow < rNow);
                }
            }
            String expr = leftLabel + " " + op + " " + rightLabel;
            return new ConditionResult(expr, leftLabel, leftValue, op, rightLabel, rightValue, passed);
        }

        // 일반 비교 조건
        double leftValue = operand(c.left(), closes, volumes, i);
        double rightValue = operand(c.right(), closes, volumes, i);
        boolean passed = false;
        if (!anyNaN(leftValue, rightValue)) {
            passed = switch (op) {
                case ">" -> leftValue > rightValue;
                case ">=" -> leftValue >= rightValue;
                case "<" -> leftValue < rightValue;
                case "<=" -> leftValue <= rightValue;
                case "==" -> leftValue == rightValue;
                default -> false;
            };
        }
        String expr = leftLabel + " " + op + " " + rightLabel;
        return new ConditionResult(expr, leftLabel, leftValue, op, rightLabel, rightValue, passed);
    }

    /**
     * 조건 그룹 logic(AND/OR)에 따라 ConditionResult 목록을 합산한다.
     * ConditionResult 각각의 passed 값을 그대로 보존한 채 그룹 boolean만 계산한다.
     */
    private boolean applyLogic(String logic, List<ConditionResult> results) {
        if (results.isEmpty()) return false;
        boolean or = "OR".equalsIgnoreCase(logic);
        boolean acc = !or; // AND 초기값=true, OR 초기값=false
        for (ConditionResult r : results) {
            acc = or ? (acc || r.passed()) : (acc && r.passed());
        }
        return acc;
    }

    /**
     * Operand → 사람이 읽기 쉬운 레이블 문자열.
     * 지표: "RSI(14)", "SMA(20)", "EMA(9)", "PRICE", "VOLUME"
     * 상수: "30.0" (Double.toString)
     */
    private String operandLabel(Operand o) {
        if (o == null) return "null";
        if (o.value() != null) return String.valueOf(o.value());
        String ind = o.indicator();
        if (ind == null) return "null";
        int period = period(o);
        if (period > 0) {
            return ind.toUpperCase() + "(" + period + ")";
        }
        return ind.toUpperCase();
    }

    // ─── 기존 private 헬퍼 (변경 없음) ────────────────────────────────────────────

    private double operand(Operand o, double[] closes, Double[] volumes, int i) {
        if (o == null) {
            return Double.NaN;
        }
        if (o.value() != null) {
            return o.value();
        }
        String ind = o.indicator();
        if (ind == null) {
            return Double.NaN;
        }
        int period = period(o);
        return switch (ind.toUpperCase()) {
            case "PRICE" -> closes[i];
            case "SMA" -> Indicators.sma(closes, i, period);
            case "EMA" -> Indicators.ema(closes, i, period);
            case "RSI" -> Indicators.rsi(closes, i, period);
            case "VOLUME" -> (volumes != null && volumes[i] != null) ? volumes[i] : Double.NaN;
            default -> Double.NaN;
        };
    }

    private int period(Operand o) {
        if (o.params() == null) {
            return 0;
        }
        Object p = o.params().get("period");
        if (p instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private boolean anyNaN(double... vals) {
        for (double v : vals) {
            if (Double.isNaN(v)) {
                return true;
            }
        }
        return false;
    }
}
