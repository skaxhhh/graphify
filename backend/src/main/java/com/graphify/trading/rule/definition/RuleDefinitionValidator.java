package com.graphify.trading.rule.definition;

import com.graphify.common.exception.GraphifyException;
import com.graphify.trading.rule.definition.RuleDefinition.Condition;
import com.graphify.trading.rule.definition.RuleDefinition.Constraints;
import com.graphify.trading.rule.definition.RuleDefinition.ConditionGroup;
import com.graphify.trading.rule.definition.RuleDefinition.ExitSpec;
import com.graphify.trading.rule.definition.RuleDefinition.Operand;
import com.graphify.trading.rule.definition.RuleDefinition.Sizing;
import com.graphify.trading.rule.definition.RuleDefinition.Universe;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 룰 definition 검증. 잘못된 스키마는 400 으로 거부한다.
 */
@Component
public class RuleDefinitionValidator {

    private static final Set<String> UNIVERSE_TYPES = Set.of("symbols", "watchlist", "volume_top_n");
    private static final Set<String> LOGICS = Set.of("AND", "OR");
    private static final Set<String> SIZING_TYPES = Set.of("cash", "percent", "qty");
    private static final Set<String> OPERATORS =
            Set.of(">", ">=", "<", "<=", "==", "crossAbove", "crossBelow");
    private static final Set<String> INDICATORS = Set.of("PRICE", "SMA", "EMA", "RSI", "VOLUME");
    private static final Set<String> PERIOD_INDICATORS = Set.of("SMA", "EMA", "RSI");

    public void validate(RuleDefinition def) {
        require(def != null, "룰 정의가 비어 있습니다.");
        require(def.version() != null && def.version() == 1, "지원하지 않는 룰 버전입니다. (version=1)");

        validateUniverse(def.universe());
        validateGroup(def.entry(), "entry", true);
        validateExit(def.exit());
        validateSizing(def.sizing());
        validateConstraints(def.constraints());
    }

    private void validateUniverse(Universe u) {
        require(u != null && u.type() != null, "universe.type 이 필요합니다.");
        require(UNIVERSE_TYPES.contains(u.type()), "지원하지 않는 universe.type 입니다.");
        if ("symbols".equals(u.type())) {
            require(u.symbols() != null && !u.symbols().isEmpty(),
                    "universe.symbols 가 비어 있습니다.");
        }
        if ("volume_top_n".equals(u.type())) {
            require(u.topN() != null && u.topN() > 0,
                    "volume_top_n 유니버스는 topN > 0 이 필요합니다.");
        }
    }

    private void validateGroup(ConditionGroup g, String field, boolean conditionsRequired) {
        require(g != null, field + " 가 필요합니다.");
        require(g.logic() != null && LOGICS.contains(g.logic()),
                field + ".logic 은 AND 또는 OR 이어야 합니다.");
        if (conditionsRequired) {
            require(g.conditions() != null && !g.conditions().isEmpty(),
                    field + ".conditions 가 비어 있습니다.");
        }
        if (g.conditions() != null) {
            for (Condition c : g.conditions()) {
                validateCondition(c, field);
            }
        }
    }

    private void validateExit(ExitSpec exit) {
        if (exit == null) {
            return; // exit 는 optional
        }
        if (exit.logic() != null) {
            require(LOGICS.contains(exit.logic()), "exit.logic 은 AND 또는 OR 이어야 합니다.");
        }
        if (exit.conditions() != null) {
            for (Condition c : exit.conditions()) {
                validateCondition(c, "exit");
            }
        }
    }

    private void validateCondition(Condition c, String field) {
        require(c != null, field + ".conditions 항목이 비어 있습니다.");
        require(c.op() != null && OPERATORS.contains(c.op()),
                field + " 에 지원하지 않는 연산자가 있습니다: " + c.op());
        validateOperand(c.left(), field + ".left");
        validateOperand(c.right(), field + ".right");
    }

    private void validateOperand(Operand o, String where) {
        require(o != null, where + " 가 필요합니다.");
        boolean hasValue = o.value() != null;
        boolean hasIndicator = o.indicator() != null;
        require(hasValue ^ hasIndicator,
                where + " 는 value 또는 indicator 중 정확히 하나여야 합니다.");
        if (hasIndicator) {
            require(INDICATORS.contains(o.indicator()),
                    where + " 에 지원하지 않는 지표가 있습니다: " + o.indicator());
            if (PERIOD_INDICATORS.contains(o.indicator())) {
                Integer period = readPeriod(o.params());
                require(period != null && period > 0,
                        where + " (" + o.indicator() + ") 는 params.period > 0 이 필요합니다.");
            }
        }
    }

    private void validateSizing(Sizing s) {
        require(s != null && s.type() != null, "sizing.type 이 필요합니다.");
        require(SIZING_TYPES.contains(s.type()), "지원하지 않는 sizing.type 입니다.");
        require(s.value() != null && s.value() > 0, "sizing.value 는 0 보다 커야 합니다.");
    }

    private void validateConstraints(Constraints c) {
        if (c == null) {
            return;
        }
        if (c.maxPositionsPerSymbol() != null) {
            require(c.maxPositionsPerSymbol() >= 0, "constraints.maxPositionsPerSymbol 는 0 이상이어야 합니다.");
        }
        if (c.cooldownBars() != null) {
            require(c.cooldownBars() >= 0, "constraints.cooldownBars 는 0 이상이어야 합니다.");
        }
    }

    private static Integer readPeriod(java.util.Map<String, Object> params) {
        if (params == null) {
            return null;
        }
        Object p = params.get("period");
        if (p instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GraphifyException("ERR_RULE_001", message, HttpStatus.BAD_REQUEST);
        }
    }
}
