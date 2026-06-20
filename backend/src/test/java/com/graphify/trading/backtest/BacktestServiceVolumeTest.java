package com.graphify.trading.backtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
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
import com.graphify.trading.rule.definition.RuleDefinition.Condition;
import com.graphify.trading.rule.definition.RuleDefinition.ConditionGroup;
import com.graphify.trading.rule.definition.RuleDefinition.ExitSpec;
import com.graphify.trading.rule.definition.RuleDefinition.Operand;
import com.graphify.trading.rule.definition.RuleDefinition.Sizing;
import com.graphify.trading.rule.definition.RuleDefinition.Universe;
import com.graphify.trading.rule.definition.RuleDefinitionValidator;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BacktestService volume null 버그 수정 회귀 테스트 — DATA-05.
 *
 * BacktestService가 closes[] 와 동일한 방식으로 volumes[] 를 추출해
 * RuleEvaluator 에 non-null Double[] 로 전달하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BacktestServiceVolumeTest {

    @Mock
    private TradingRuleRepository ruleRepository;

    @Mock
    private MarketDataPort marketData;

    @Spy
    private RuleEvaluator evaluator;

    private BacktestService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RuleDefinitionValidator validator = new RuleDefinitionValidator();
        FillSimulator fillSimulator = new FillSimulator();
        service = new BacktestService(ruleRepository, validator, objectMapper, marketData, evaluator, fillSimulator);
    }

    /**
     * market_bars 에 volume=50000 인 봉이 있고 VOLUME > 30000 진입 룰이면
     * trades 에 BUY 가 포함된다.
     */
    @Test
    void volumeRule_withBar50000_generatesBuySignal() throws Exception {
        // 봉: 2024-01-02, close=10000, volume=50000
        LocalDate date = LocalDate.of(2024, 1, 2);
        Bar bar = new Bar(date, 10000.0, 50000.0);
        when(marketData.historicalDailyBars("005930")).thenReturn(List.of(bar));

        JsonNode defNode = buildVolumeGt30000DefNode("005930");
        BacktestRequest request = new BacktestRequest(null, defNode, null, null, null, null, null);

        ApiResponse<BacktestResult> response = service.run(request);
        List<BacktestResult.TradeDto> trades = response.data().trades();

        assertThat(trades).isNotEmpty();
        assertThat(trades).anyMatch(t -> "BUY".equals(t.side()));
    }

    /**
     * volumes[] 배열이 null 이 아닌 실제 값으로 RuleEvaluator 에 전달된다.
     * (mock spy ArgumentCaptor 로 검증)
     */
    @Test
    void backtestService_passesNonNullVolumesToEvaluator() throws Exception {
        LocalDate date = LocalDate.of(2024, 1, 2);
        Bar bar = new Bar(date, 10000.0, 50000.0);
        when(marketData.historicalDailyBars("005930")).thenReturn(List.of(bar));

        JsonNode defNode = buildVolumeGt30000DefNode("005930");
        BacktestRequest request = new BacktestRequest(null, defNode, null, null, null, null, null);

        service.run(request);

        // entryTriggered 호출 시 volumes 인자(3번째)가 null 이 아닌지 검증
        ArgumentCaptor<Double[]> volumesCaptor = ArgumentCaptor.forClass(Double[].class);
        verify(evaluator, atLeastOnce()).entryTriggered(
                any(ConditionGroup.class),
                any(double[].class),
                volumesCaptor.capture(),
                anyInt()
        );

        Double[] capturedVolumes = volumesCaptor.getValue();
        assertThat(capturedVolumes).isNotNull();
        assertThat(capturedVolumes[0]).isEqualTo(50000.0);
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
