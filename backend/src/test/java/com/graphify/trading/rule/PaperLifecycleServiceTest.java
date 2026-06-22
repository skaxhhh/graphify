package com.graphify.trading.rule;

import com.graphify.common.exception.GraphifyException;
import com.graphify.trading.engine.MarketDataPort;
import com.graphify.trading.paper.PaperLifecycleService;
import com.graphify.trading.rule.dto.RuleResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RULE-08 전이 가드 + ACTIVE 필터 검증.
 */
@ExtendWith(MockitoExtension.class)
class PaperLifecycleServiceTest {

    @Mock TradingRuleRepository ruleRepo;
    @Mock PaperLiveSymbolService paperLiveSymbolService;
    @Mock MarketDataPort marketData;
    @Mock com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    PaperLifecycleService service;

    static final Long USER_ID = 1L;
    static final Long RULE_ID = 42L;
    static final String DEFINITION = "{\"version\":1,\"universe\":{\"type\":\"symbols\",\"symbols\":[\"A005930\"]}," +
        "\"entry\":{\"logic\":\"AND\",\"conditions\":[]},\"exit\":null," +
        "\"sizing\":{\"type\":\"fixed_cash\",\"value\":10000000},\"constraints\":null}";

    @BeforeEach
    void setUp() {
        service = new PaperLifecycleService(
            ruleRepo,
            new com.fasterxml.jackson.databind.ObjectMapper(),
            paperLiveSymbolService,
            marketData
        );
    }

    // ─── activate ───────────────────────────────────────────────────────────────

    @Test
    void activate_draft_rule_succeeds_without_backtest() {
        // RULE-08: DRAFT 룰은 백테스트 없이 ACTIVE로 전환 가능 (RULE-01 게이트 폐지)
        TradingRule rule = draftRule();
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));
        when(ruleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse resp = service.activate(USER_ID, RULE_ID);

        assertThat(resp.configStatus()).isEqualTo("ACTIVE");
        assertThat(rule.getConfigStatus()).isEqualTo("ACTIVE");
        // 백테스트 안 했어도 예외 없음
        assertThat(rule.isBacktested()).isFalse();
    }

    @Test
    void activate_non_draft_throws() {
        TradingRule rule = activeRule();
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.activate(USER_ID, RULE_ID))
            .isInstanceOf(GraphifyException.class)
            .hasMessageContaining("DRAFT");
    }

    // ─── deactivate ─────────────────────────────────────────────────────────────

    @Test
    void deactivate_active_stopped_rule_succeeds() {
        TradingRule rule = activeRule(); // configStatus=ACTIVE, runStatus=STOPPED
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));
        when(ruleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse resp = service.deactivate(USER_ID, RULE_ID);

        assertThat(resp.configStatus()).isEqualTo("DRAFT");
    }

    @Test
    void deactivate_running_rule_throws() {
        // RULE-08: RUNNING 중인 룰은 하향 차단
        TradingRule rule = activeRule();
        rule.setRunStatus("RUNNING");
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.deactivate(USER_ID, RULE_ID))
            .isInstanceOf(GraphifyException.class)
            .hasMessageContaining("RUNNING");
    }

    // ─── start ──────────────────────────────────────────────────────────────────

    @Test
    void start_active_stopped_rule_sets_running() {
        TradingRule rule = activeRule(); // configStatus=ACTIVE, runStatus=STOPPED
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));
        when(ruleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse resp = service.start(USER_ID, RULE_ID);

        assertThat(resp.runStatus()).isEqualTo("RUNNING");
        assertThat(resp.configStatus()).isEqualTo("ACTIVE");
        verify(paperLiveSymbolService).assignSymbols(eq(RULE_ID), any());
    }

    @Test
    void start_draft_rule_throws() {
        TradingRule rule = draftRule();
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.start(USER_ID, RULE_ID))
            .isInstanceOf(GraphifyException.class)
            .hasMessageContaining("ACTIVE");
    }

    // ─── stop ───────────────────────────────────────────────────────────────────

    @Test
    void stop_running_rule_returns_active_stopped() {
        // RULE-08: stop 후 config는 ACTIVE 유지 (PAUSED 없음)
        TradingRule rule = activeRule();
        rule.setRunStatus("RUNNING");
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));
        when(ruleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse resp = service.stop(USER_ID, RULE_ID);

        assertThat(resp.runStatus()).isEqualTo("STOPPED");
        assertThat(resp.configStatus()).isEqualTo("ACTIVE"); // config 유지
        verify(paperLiveSymbolService).deactivateRule(RULE_ID);
    }

    @Test
    void stop_non_running_rule_throws() {
        TradingRule rule = activeRule(); // runStatus=STOPPED already
        when(ruleRepo.findByIdAndUserId(RULE_ID, USER_ID)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.stop(USER_ID, RULE_ID))
            .isInstanceOf(GraphifyException.class)
            .hasMessageContaining("RUNNING");
    }

    // ─── listActive ─────────────────────────────────────────────────────────────

    @Test
    void listActive_returns_only_active_rules_not_draft() {
        // RULE-08: 전략 운영 화면은 configStatus=ACTIVE 룰만 반환
        TradingRule activeRule = activeRule();
        TradingRule draftRule = draftRule();
        draftRule.setRunStatus("STOPPED");

        when(ruleRepo.findByUserIdAndModeOrderByUpdatedAtDesc(USER_ID, "PAPER"))
            .thenReturn(List.of(activeRule, draftRule));

        List<RuleResponse> result = service.listActive(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).configStatus()).isEqualTo("ACTIVE");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private TradingRule draftRule() {
        TradingRule rule = new TradingRule(USER_ID, "Test Rule", "PAPER", "DRAFT", DEFINITION);
        setId(rule, RULE_ID);
        rule.setConfigStatus("DRAFT");
        rule.setRunStatus("STOPPED");
        return rule;
    }

    private TradingRule activeRule() {
        TradingRule rule = new TradingRule(USER_ID, "Test Rule", "PAPER", "DRAFT", DEFINITION);
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
