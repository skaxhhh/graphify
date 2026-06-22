package com.graphify.trading.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.common.exception.GraphifyException;
import com.graphify.trading.rule.definition.RuleDefinitionValidator;
import com.graphify.trading.rule.dto.RuleUpsertRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.graphify.history.HistoryService;

/**
 * RULE-08: RUNNING 룰 편집 차단 + CRUD가 run_status를 변경하지 않음.
 */
@ExtendWith(MockitoExtension.class)
class PaperRuleServiceTest {

    @Mock TradingRuleRepository ruleRepository;
    @Mock RuleDefinitionValidator validator;

    ObjectMapper objectMapper = new ObjectMapper();
    PaperRuleService service;

    static final Long USER_ID = 1L;
    static final Long RULE_ID = 10L;
    static final String DEFINITION = "{\"version\":1,\"universe\":{\"type\":\"symbols\",\"symbols\":[\"A005930\"]}," +
        "\"entry\":{\"logic\":\"AND\",\"conditions\":[]},\"exit\":null," +
        "\"sizing\":{\"type\":\"fixed_cash\",\"value\":10000000},\"constraints\":null}";

    @BeforeEach
    void setUp() {
        service = new PaperRuleService(ruleRepository, validator, objectMapper);
    }

    @Test
    void update_running_rule_throws() throws Exception {
        // RULE-08: RUNNING 중인 룰은 수정 불가
        TradingRule rule = runningActiveRule();

        try (MockedStatic<HistoryService> hs = mockStatic(HistoryService.class)) {
            hs.when(HistoryService::requireCurrentUserId).thenReturn(USER_ID);
            when(ruleRepository.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));

            JsonNode defNode = objectMapper.valueToTree(
                objectMapper.readValue(DEFINITION, Object.class));
            RuleUpsertRequest req = new RuleUpsertRequest("New Name", "DRAFT", defNode);

            assertThatThrownBy(() -> service.update(RULE_ID, req))
                .isInstanceOf(GraphifyException.class)
                .hasMessageContaining("RUNNING");
        }
    }

    @Test
    void update_stopped_rule_does_not_change_run_status() throws Exception {
        // CRUD update는 run_status를 변경하지 않음
        TradingRule rule = activeStoppedRule();

        try (MockedStatic<HistoryService> hs = mockStatic(HistoryService.class)) {
            hs.when(HistoryService::requireCurrentUserId).thenReturn(USER_ID);
            when(ruleRepository.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));
            when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(validator).validate(any());

            JsonNode defNode = objectMapper.valueToTree(
                objectMapper.readValue(DEFINITION, Object.class));
            RuleUpsertRequest req = new RuleUpsertRequest("Updated Name", "ACTIVE", defNode);

            service.update(RULE_ID, req);

            // run_status는 STOPPED 그대로 — lifecycle에서만 변경 가능
            assertThat(rule.getRunStatus()).isEqualTo("STOPPED");
        }
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private TradingRule runningActiveRule() {
        TradingRule rule = new TradingRule(USER_ID, "Rule", "PAPER", "DRAFT", DEFINITION);
        setId(rule, RULE_ID);
        rule.setConfigStatus("ACTIVE");
        rule.setRunStatus("RUNNING");
        return rule;
    }

    private TradingRule activeStoppedRule() {
        TradingRule rule = new TradingRule(USER_ID, "Rule", "PAPER", "DRAFT", DEFINITION);
        setId(rule, RULE_ID);
        rule.setConfigStatus("ACTIVE");
        rule.setRunStatus("STOPPED");
        return rule;
    }

    private void setId(TradingRule rule, Long id) {
        try {
            var field = TradingRule.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(rule, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set rule id", e);
        }
    }
}
