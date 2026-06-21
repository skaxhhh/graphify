package com.graphify.trading.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.market.MarketBarIntraday;
import com.graphify.market.MarketBarIntradayRepository;
import com.graphify.trading.engine.MarketDataPort;
import com.graphify.trading.engine.RuleEvaluator;
import com.graphify.trading.engine.Signal;
import com.graphify.trading.rule.TradingRule;
import com.graphify.trading.rule.TradingRuleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveEvaluationServiceTest {

    @Mock TradingRuleRepository ruleRepo;
    @Mock MarketDataPort marketDataPort;
    @Mock MarketBarIntradayRepository intradayRepo;
    @Mock RuleEvaluator ruleEvaluator;
    @Mock OrderExecutorPort executor;
    @Mock PaperAccountRepository accountRepo;
    @Mock PaperPositionRepository positionRepo;
    @Mock PaperEquitySnapshotRepository snapshotRepo;
    ObjectMapper objectMapper = new ObjectMapper();

    LiveEvaluationService service;
    static final String SYMBOL = "A005930";
    static final Instant NOW = Instant.now();
    static final String RULE_DEF = """
            {"version":1,"universe":{"type":"symbols","symbols":["A005930"]},
             "entry":{"logic":"AND","conditions":[{"left":{"indicator":"RSI","params":{"period":14}},
             "op":">","right":{"value":30}}]},
             "exit":{"takeProfitPct":5.0,"stopLossPct":-3.0},
             "sizing":{"type":"full_cash","value":null},"constraints":null}
            """;

    @BeforeEach
    void setUp() {
        service = new LiveEvaluationService(
            ruleRepo, marketDataPort, intradayRepo, ruleEvaluator,
            executor, accountRepo, positionRepo, snapshotRepo, objectMapper
        );
    }

    private TradingRule paperLiveRule() {
        return new TradingRule(1L, "Test", "PAPER", "PAPER_LIVE", RULE_DEF);
    }

    private List<MarketBarIntraday> makeBars(int count, boolean stale) {
        Instant baseTs = stale
            ? NOW.minus(30, ChronoUnit.MINUTES)
            : NOW.minus(count * 5L, ChronoUnit.MINUTES);
        List<MarketBarIntraday> bars = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bars.add(new MarketBarIntraday(SYMBOL, baseTs.plus(i * 5L, ChronoUnit.MINUTES),
                "5m", 70000.0, 71000.0, 69000.0, 70000.0 + i * 100, 1000L, "YAHOO"));
        }
        return bars;
    }

    @Test
    void no_paper_live_rules_skips_everything() {
        when(ruleRepo.findAll()).thenReturn(List.of());
        service.evaluateTick(NOW);
        verifyNoInteractions(executor, snapshotRepo);
    }

    @Test
    void entry_triggered_calls_buy() {
        TradingRule rule = paperLiveRule();
        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(marketDataPort.recentIntradayBars(SYMBOL)).thenReturn(makeBars(25, false));
        when(intradayRepo.findMaxTsBySymbolAndInterval(SYMBOL, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());
        when(ruleEvaluator.entryTriggered(any(), any(), any(), anyInt())).thenReturn(true);
        when(executor.execute(eq(Signal.BUY), any(), eq(SYMBOL), anyDouble(), any(), any()))
            .thenReturn(TradeResult.filled("BUY", SYMBOL, 70000.0, 142.0, null));
        PaperAccount acc = new PaperAccount(1L, BigDecimal.valueOf(10_000_000));
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(positionRepo.findByAccountId(any())).thenReturn(Collections.emptyList());

        service.evaluateTick(NOW);

        verify(executor).execute(eq(Signal.BUY), any(), eq(SYMBOL), anyDouble(), any(), any());
    }

    @Test
    void exit_triggered_calls_sell() {
        TradingRule rule = paperLiveRule();
        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(marketDataPort.recentIntradayBars(SYMBOL)).thenReturn(makeBars(25, false));
        when(intradayRepo.findMaxTsBySymbolAndInterval(SYMBOL, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        PaperPosition pos = new PaperPosition(1L, SYMBOL,
            BigDecimal.valueOf(100), BigDecimal.valueOf(70000));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.of(pos));
        when(ruleEvaluator.exitTriggered(any(), any(), any(), anyInt(), anyDouble())).thenReturn(true);
        when(executor.execute(eq(Signal.SELL), any(), eq(SYMBOL), anyDouble(), any(), any()))
            .thenReturn(TradeResult.filled("SELL", SYMBOL, 72000.0, 100.0, 200000.0));
        PaperAccount acc = new PaperAccount(1L, BigDecimal.valueOf(10_000_000));
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(positionRepo.findByAccountId(any())).thenReturn(Collections.emptyList());

        service.evaluateTick(NOW);

        verify(executor).execute(eq(Signal.SELL), any(), eq(SYMBOL), anyDouble(), any(), any());
    }

    @Test
    void stale_bars_skips_evaluation() {
        TradingRule rule = paperLiveRule();
        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        // Max ts is 30 minutes ago — stale; evaluateSymbol returns early before bars are read
        when(intradayRepo.findMaxTsBySymbolAndInterval(SYMBOL, "5m"))
            .thenReturn(Optional.of(NOW.minus(30, ChronoUnit.MINUTES)));
        PaperAccount acc = new PaperAccount(1L, BigDecimal.valueOf(10_000_000));
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(positionRepo.findByAccountId(any())).thenReturn(Collections.emptyList());
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateTick(NOW);

        verify(executor, never()).execute(any(), any(), any(), anyDouble(), any(), any());
    }

    @Test
    void insufficient_bars_skips_evaluation() {
        TradingRule rule = paperLiveRule();
        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(marketDataPort.recentIntradayBars(SYMBOL)).thenReturn(makeBars(5, false));
        when(intradayRepo.findMaxTsBySymbolAndInterval(SYMBOL, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        PaperAccount acc = new PaperAccount(1L, BigDecimal.valueOf(10_000_000));
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(positionRepo.findByAccountId(any())).thenReturn(Collections.emptyList());

        service.evaluateTick(NOW);

        verify(executor, never()).execute(any(), any(), any(), anyDouble(), any(), any());
    }

    @Test
    void equity_snapshot_saved_after_tick() {
        TradingRule rule = paperLiveRule();
        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(marketDataPort.recentIntradayBars(SYMBOL)).thenReturn(makeBars(25, false));
        when(intradayRepo.findMaxTsBySymbolAndInterval(SYMBOL, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        when(ruleEvaluator.entryTriggered(any(), any(), any(), anyInt())).thenReturn(false);
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());
        PaperAccount acc = new PaperAccount(1L, BigDecimal.valueOf(10_000_000));
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(acc));
        when(positionRepo.findByAccountId(any())).thenReturn(Collections.emptyList());
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateTick(NOW);

        verify(snapshotRepo, atLeastOnce()).save(any(PaperEquitySnapshot.class));
    }
}
