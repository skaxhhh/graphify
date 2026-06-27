package com.graphify.trading.backtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * v1.6.0 단위 테스트: BacktestRequest.overrideSymbols 폴백 경로.
 *
 * overrideSymbols가 있으면 volume_top_n 자동해석(symbolsByMarket)을 우회하고
 * 선택 종목을 엔진에 그대로 전달하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BacktestServiceOverrideTest {

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

    @Test
    void overrideSymbols_bypassesVolumeTopNResolution() throws Exception {
        LocalDate date = LocalDate.of(2024, 1, 2);
        Bar bar = new Bar(date, 10000.0, 50000.0);
        when(marketData.historicalDailyBars("005930")).thenReturn(List.of(bar));

        LocalDateTime dt = date.atStartOfDay();
        BacktestResult fakeResult = new BacktestResult(
                10_000_000.0, 10_000_000.0, 0.0, 0.0, 0.0, 0,
                0.0, 0.0, 0.0, List.of(), List.of(),
                List.of(new BacktestResult.EquityPoint(dt, 10_000_000.0))
        );
        ArgumentCaptor<List<String>> symbolsCaptor = ArgumentCaptor.forClass(List.class);
        when(intradayEngine.run(any(), any(RuleDefinition.class), symbolsCaptor.capture(), any(), any()))
                .thenReturn(fakeResult);

        // volume_top_n 룰 + override 종목 직접 지정
        JsonNode defNode = buildVolumeTopNDefNode();
        BacktestRequest request = new BacktestRequest(
                null, defNode, null, null, null, null, null, List.of("005930"));

        service.run(request);

        // volume_top_n 자동해석을 우회 → symbolsByMarket 호출 안 됨
        verify(marketData, never()).symbolsByMarket(any());
        // 엔진에는 override 종목이 그대로 전달됨
        assertThat(symbolsCaptor.getValue()).containsExactly("005930");
    }

    private JsonNode buildVolumeTopNDefNode() throws Exception {
        String json = """
                {
                  "version": 1,
                  "universe": { "type": "volume_top_n", "market": "KOSPI", "topN": 10 },
                  "entry": {
                    "logic": "AND",
                    "conditions": [
                      { "left": { "indicator": "VOLUME" }, "op": ">", "right": { "value": 30000 } }
                    ]
                  },
                  "exit": { "takeProfitPct": 10.0, "stopLossPct": -5.0 },
                  "sizing": { "type": "cash", "value": 1000000 }
                }
                """;
        return objectMapper.readTree(json);
    }
}
