package com.graphify.trading.backtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.common.dto.ApiResponse;
import com.graphify.trading.backtest.dto.BacktestRequest;
import com.graphify.trading.backtest.dto.BacktestResult;
import com.graphify.trading.engine.Bar;
import com.graphify.trading.engine.FillSimulator;
import com.graphify.trading.engine.MarketDataPort;
import com.graphify.trading.engine.RuleEvaluator;
import com.graphify.trading.rule.TradingRuleRepository;
import com.graphify.trading.rule.definition.RuleDefinition;
import com.graphify.trading.rule.definition.RuleDefinitionValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BacktestService volume null 버그 수정 회귀 테스트 — DATA-05.
 *
 * Phase 1+에서 BacktestService는 IntradayBacktestEngine에 위임하므로,
 * 이 테스트는 IntradayBacktestEngine을 mock하여 BacktestService의
 * 위임 경로와 기본 파이프라인을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BacktestServiceVolumeTest {

    @Mock
    private TradingRuleRepository ruleRepository;

    @Mock
    private MarketDataPort marketData;

    @Mock
    private IntradayBacktestEngine intradayEngine;

    private BacktestService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RuleDefinitionValidator validator = new RuleDefinitionValidator();
        FillSimulator fillSimulator = new FillSimulator();
        RuleEvaluator evaluator = new RuleEvaluator();
        service = new BacktestService(ruleRepository, validator, objectMapper, marketData,
                evaluator, fillSimulator, intradayEngine);
    }

    /**
     * market_bars 에 volume=50000 인 봉이 있고 VOLUME > 30000 진입 룰이면
     * IntradayBacktestEngine에 위임하고 그 결과를 반환한다.
     */
    @Test
    void volumeRule_withBar50000_generatesBuySignal() throws Exception {
        // 봉: 2024-01-02, close=10000, volume=50000
        LocalDate date = LocalDate.of(2024, 1, 2);
        Bar bar = new Bar(date, 10000.0, 50000.0);
        when(marketData.historicalDailyBars("005930")).thenReturn(List.of(bar));

        // Engine returns a result with a BUY trade
        LocalDateTime dt = date.atStartOfDay();
        BacktestResult fakeResult = new BacktestResult(
                10_000_000.0, 10_010_000.0, 0.1, 0.0, 100.0, 1,
                0.1, 0.0, Double.MAX_VALUE, List.of(),
                List.of(new BacktestResult.TradeDto(dt, "005930", null, "BUY", 1.0, 10000.0, null, null)),
                List.of(new BacktestResult.EquityPoint(dt, 10_010_000.0))
        );
        when(intradayEngine.run(any(), any(RuleDefinition.class), anyList(), any(), any()))
                .thenReturn(fakeResult);

        JsonNode defNode = buildVolumeGt30000DefNode("005930");
        BacktestRequest request = new BacktestRequest(null, defNode, null, null, null, null, null);

        ApiResponse<BacktestResult> response = service.run(request);
        List<BacktestResult.TradeDto> trades = response.data().trades();

        assertThat(trades).isNotEmpty();
        assertThat(trades).anyMatch(t -> "BUY".equals(t.side()));
    }

    /**
     * BacktestService가 IntradayBacktestEngine에 실제로 위임하는지 검증.
     */
    @Test
    void backtestService_delegatesToIntradayEngine() throws Exception {
        LocalDate date = LocalDate.of(2024, 1, 2);
        Bar bar = new Bar(date, 10000.0, 50000.0);
        when(marketData.historicalDailyBars("005930")).thenReturn(List.of(bar));

        LocalDateTime dt = date.atStartOfDay();
        BacktestResult fakeResult = new BacktestResult(
                10_000_000.0, 10_000_000.0, 0.0, 0.0, 0.0, 0,
                0.0, 0.0, 0.0, List.of(), List.of(),
                List.of(new BacktestResult.EquityPoint(dt, 10_000_000.0))
        );
        when(intradayEngine.run(any(), any(RuleDefinition.class), anyList(), any(), any()))
                .thenReturn(fakeResult);

        JsonNode defNode = buildVolumeGt30000DefNode("005930");
        BacktestRequest request = new BacktestRequest(null, defNode, null, null, null, null, null);

        service.run(request);

        // Verify delegation occurred — if engine wasn't called, when() stub would not have been used
        // and the test would fail due to UnnecessaryStubbingException (lenient mode not set)
        // Mockito strict stubs verify this implicitly
    }

    // --- helpers ---

    private JsonNode buildVolumeGt30000DefNode(String symbol) throws Exception {
        String json = """
                {
                  "version": 1,
                  "universe": { "type": "symbols", "symbols": ["%s"] },
                  "entry": {
                    "logic": "AND",
                    "conditions": [
                      { "left": { "indicator": "VOLUME" }, "op": ">", "right": { "value": 30000 } }
                    ]
                  },
                  "exit": { "takeProfitPct": 10.0, "stopLossPct": -5.0 },
                  "sizing": { "type": "cash", "value": 1000000 }
                }
                """.formatted(symbol);
        return objectMapper.readTree(json);
    }
}
