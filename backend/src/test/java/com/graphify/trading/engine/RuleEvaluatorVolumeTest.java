package com.graphify.trading.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.graphify.trading.rule.definition.RuleDefinition.Condition;
import com.graphify.trading.rule.definition.RuleDefinition.ConditionGroup;
import com.graphify.trading.rule.definition.RuleDefinition.Operand;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * VOLUME 지표 조건 단위 테스트 — DATA-05 회귀 방지.
 */
class RuleEvaluatorVolumeTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();

    /** VOLUME > 30000 조건 그룹 생성 헬퍼 */
    private ConditionGroup volumeGt30000() {
        Operand left = new Operand("VOLUME", null, null);
        Operand right = new Operand(null, 30000.0, null);
        Condition cond = new Condition(left, ">", right);
        return new ConditionGroup("AND", List.of(cond));
    }

    @Test
    void volumeGt30000_withVolumeOf50000_returnsTrue() {
        // volumes=[50000.0], VOLUME > 30000 → true
        Double[] volumes = {50000.0};
        double[] closes = {1000.0};
        boolean triggered = evaluator.entryTriggered(volumeGt30000(), closes, volumes, 0);
        assertThat(triggered).isTrue();
    }

    @Test
    void volumeGt30000_withNullVolumes_returnsFalse() {
        // volumes=null → NaN fallback → false
        double[] closes = {1000.0};
        boolean triggered = evaluator.entryTriggered(volumeGt30000(), closes, null, 0);
        assertThat(triggered).isFalse();
    }

    @Test
    void volumeGt30000_withVolumeOf10000_returnsFalse() {
        // volumes=[10000.0], VOLUME > 30000 → false (10000 is not > 30000)
        Double[] volumes = {10000.0};
        double[] closes = {1000.0};
        boolean triggered = evaluator.entryTriggered(volumeGt30000(), closes, volumes, 0);
        assertThat(triggered).isFalse();
    }
}
