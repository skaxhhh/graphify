package com.graphify.trading.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.market.MarketBarIntraday;
import com.graphify.market.MarketBarIntradayRepository;
import com.graphify.market.volume.VolumeRankingProvider;
import com.graphify.trading.engine.EvalResult;
import com.graphify.trading.engine.MarketDataPort;
import com.graphify.trading.engine.RuleEvaluator;
import com.graphify.trading.engine.Signal;
import com.graphify.trading.rule.PaperLiveSymbol;
import com.graphify.trading.rule.PaperLiveSymbolRepository;
import com.graphify.trading.rule.TradingRule;
import com.graphify.trading.rule.TradingRuleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

/**
 * DATA-06-SC5b: LiveEvaluationService 진입 게이팅 + top-N 이탈 종목 청산 유지 검증.
 *
 * <p>volume_top_n 룰에서:
 * - ENTRY: top-N에 없는 종목은 진입 차단
 * - EXIT: 보유 종목은 top-N 이탈 후에도 항상 청산 평가
 * - symbols 타입 룰: 게이팅 미적용 (기존 동작 유지)
 */
@ExtendWith(MockitoExtension.class)
class LiveEvaluationServiceVolumeTest {

    @Mock TradingRuleRepository ruleRepo;
    @Mock MarketDataPort marketDataPort;
    @Mock MarketBarIntradayRepository intradayRepo;
    @Mock RuleEvaluator ruleEvaluator;
    @Mock OrderExecutorPort executor;
    @Mock PaperAccountRepository accountRepo;
    @Mock PaperPositionRepository positionRepo;
    @Mock PaperEquitySnapshotRepository snapshotRepo;
    @Mock PaperLiveSymbolRepository paperLiveSymbolRepository;
    @Mock VolumeRankingProvider liveRanking;

    ObjectMapper objectMapper = new ObjectMapper();
    LiveEvaluationService service;

    static final Instant NOW = Instant.now();
    static final Long ACCOUNT_ID = 1L;
    static final Long USER_ID = 1L;

    /** Create PaperAccount with id set via reflection (id is DB-generated, null without persist). */
    private PaperAccount makeAccount() {
        PaperAccount acc = new PaperAccount(USER_ID, BigDecimal.valueOf(10_000_000));
        try {
            var field = PaperAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(acc, ACCOUNT_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return acc;
    }

    /** volume_top_n 룰 definition (topN=3, market=KOSPI) */
    static final String VOLUME_TOP_N_DEF = """
            {"version":1,"universe":{"type":"volume_top_n","market":"KOSPI","topN":3},
             "entry":{"logic":"AND","conditions":[{"left":{"indicator":"RSI","params":{"period":14}},
             "op":">","right":{"value":30}}]},
             "exit":{"takeProfitPct":5.0,"stopLossPct":-3.0},
             "sizing":{"type":"cash","value":1000000},"constraints":null}
            """;

    /** symbols 타입 룰 definition */
    static final String SYMBOLS_DEF = """
            {"version":1,"universe":{"type":"symbols","symbols":["005930","000660"]},
             "entry":{"logic":"AND","conditions":[{"left":{"indicator":"RSI","params":{"period":14}},
             "op":">","right":{"value":30}}]},
             "exit":{"takeProfitPct":5.0,"stopLossPct":-3.0},
             "sizing":{"type":"cash","value":1000000},"constraints":null}
            """;

    private TradingRule makeRule(Long ruleId, String definition) {
        TradingRule rule = new TradingRule(USER_ID, "Test", "PAPER", "DRAFT", definition);
        rule.setConfigStatus("ACTIVE");
        rule.setRunStatus("RUNNING");
        try {
            var field = TradingRule.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(rule, ruleId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rule;
    }

    private List<MarketBarIntraday> makeBars(String symbol, int count) {
        List<MarketBarIntraday> bars = new ArrayList<>();
        Instant baseTs = NOW.minus(count * 5L, ChronoUnit.MINUTES);
        for (int i = 0; i < count; i++) {
            bars.add(new MarketBarIntraday(symbol, baseTs.plus(i * 5L, ChronoUnit.MINUTES),
                "5m", 70000.0, 71000.0, 69000.0, 70000.0 + i * 100, 1000L, "YAHOO"));
        }
        return bars;
    }

    @BeforeEach
    void setUp() {
        service = new LiveEvaluationService(
            ruleRepo, marketDataPort, intradayRepo, ruleEvaluator,
            List.of(executor), accountRepo, positionRepo, snapshotRepo, objectMapper,
            paperLiveSymbolRepository, liveRanking
        );
    }

    /**
     * Test 1 (DATA-06-SC5b): volume_top_n 룰, 종목 X 보유 포지션 있음 + 현재 top-N에 X 없음
     * + evalExit가 SELL 트리거 → executor.execute(SELL, ...) 호출됨 (이탈해도 청산 평가)
     */
    @Test
    void volumeTopN_exitEvaluated_evenWhenSymbolDroppedFromTopN() {
        Long ruleId = 10L;
        String symbol = "X";
        TradingRule rule = makeRule(ruleId, VOLUME_TOP_N_DEF);

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        // X는 top-N에 없음 (top-N = A, B, C)
        when(liveRanking.topVolume(eq("KOSPI"), any(LocalDate.class), eq(3), eq(true)))
            .thenReturn(List.of("A", "B", "C"));
        // paper_live_symbols에는 X가 있음 (VolumeRankRefresher가 보유 포지션 union으로 추가했음)
        when(paperLiveSymbolRepository.findByRuleId(ruleId))
            .thenReturn(List.of(new PaperLiveSymbol(ruleId, symbol)));

        PaperAccount acc = makeAccount();
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(acc));

        // X 보유 포지션 있음
        PaperPosition posX = new PaperPosition(ACCOUNT_ID, symbol, BigDecimal.valueOf(100), BigDecimal.valueOf(70000));
        when(positionRepo.findByAccountIdAndSymbol(ACCOUNT_ID, symbol)).thenReturn(Optional.of(posX));
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(posX));

        when(intradayRepo.findMaxTsBySymbolAndInterval(symbol, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        when(marketDataPort.recentIntradayBars(symbol)).thenReturn(makeBars(symbol, 25));

        // SELL 트리거
        when(ruleEvaluator.evalExit(any(), any(), any(), anyInt(), anyDouble()))
            .thenReturn(new EvalResult(true, List.of(), EvalResult.ExitReason.TAKE_PROFIT, 5.0));
        when(executor.supports(any())).thenReturn(true);
        when(executor.execute(eq(Signal.SELL), any(), eq(symbol), anyDouble(), any(), any()))
            .thenReturn(TradeResult.filled("SELL", symbol, 73500.0, 100.0, 350000.0));
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateTick(NOW);

        // 이탈 종목도 청산 평가 호출됨
        verify(executor).execute(eq(Signal.SELL), any(), eq(symbol), anyDouble(), any(), any());
    }

    /**
     * Test 2: 종목 Y 포지션 없음 + 현재 top-N에 Y 없음 + evalEntry가 BUY 트리거
     * → executor.execute 호출 안 됨 (진입 게이팅: 비-top-N 진입 차단)
     */
    @Test
    void volumeTopN_entryBlocked_whenSymbolNotInTopN() {
        Long ruleId = 10L;
        String symbol = "Y";
        TradingRule rule = makeRule(ruleId, VOLUME_TOP_N_DEF);

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        // Y는 top-N에 없음
        when(liveRanking.topVolume(eq("KOSPI"), any(LocalDate.class), eq(3), eq(true)))
            .thenReturn(List.of("A", "B", "C"));
        // paper_live_symbols에는 Y가 있음 (이전 틱에 보유로 추가됐다고 가정)
        when(paperLiveSymbolRepository.findByRuleId(ruleId))
            .thenReturn(List.of(new PaperLiveSymbol(ruleId, symbol)));

        PaperAccount acc = makeAccount();
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(acc));

        // Y 포지션 없음 — positionRepo.findByAccountIdAndSymbol is called before entry gate
        when(positionRepo.findByAccountIdAndSymbol(ACCOUNT_ID, symbol)).thenReturn(Optional.empty());
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(Collections.emptyList());

        when(intradayRepo.findMaxTsBySymbolAndInterval(symbol, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        when(marketDataPort.recentIntradayBars(symbol)).thenReturn(makeBars(symbol, 25));

        // Note: ruleEvaluator.evalEntry is NOT stubbed — entry gate fires before evalEntry is reached
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateTick(NOW);

        // top-N 비멤버이므로 BUY 차단
        verify(executor, never()).execute(any(), any(), any(), anyDouble(), any(), any());
    }

    /**
     * Test 3: 종목 Z 포지션 없음 + top-N에 Z 있음 + BUY 트리거
     * → executor.execute(BUY,...) 호출됨
     */
    @Test
    void volumeTopN_entryAllowed_whenSymbolIsInTopN() {
        Long ruleId = 10L;
        String symbol = "Z";
        TradingRule rule = makeRule(ruleId, VOLUME_TOP_N_DEF);

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        // Z가 top-N에 있음
        when(liveRanking.topVolume(eq("KOSPI"), any(LocalDate.class), eq(3), eq(true)))
            .thenReturn(List.of("A", "Z", "C"));
        when(paperLiveSymbolRepository.findByRuleId(ruleId))
            .thenReturn(List.of(new PaperLiveSymbol(ruleId, symbol)));

        PaperAccount acc = makeAccount();
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(acc));

        // Z 포지션 없음
        when(positionRepo.findByAccountIdAndSymbol(ACCOUNT_ID, symbol)).thenReturn(Optional.empty());
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(Collections.emptyList());

        when(intradayRepo.findMaxTsBySymbolAndInterval(symbol, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        when(marketDataPort.recentIntradayBars(symbol)).thenReturn(makeBars(symbol, 25));

        // BUY 트리거
        when(ruleEvaluator.evalEntry(any(), any(), any(), anyInt()))
            .thenReturn(new EvalResult(true, List.of(), null, null));
        when(executor.supports(any())).thenReturn(true);
        when(executor.execute(eq(Signal.BUY), any(), eq(symbol), anyDouble(), any(), any()))
            .thenReturn(TradeResult.filled("BUY", symbol, 70000.0, 142.0, null));
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateTick(NOW);

        // top-N 멤버이므로 BUY 허용
        verify(executor).execute(eq(Signal.BUY), any(), eq(symbol), anyDouble(), any(), any());
    }

    /**
     * Test 4: symbols 타입 룰은 게이팅 미적용 (기존 동작 유지) — 모든 종목 진입 평가
     * top-N 체크 없이 evalEntry 결과만으로 진입 결정.
     */
    @Test
    void symbolsRule_entryNotGated_byTopN() {
        Long ruleId = 20L;
        String symbol = "005930";
        TradingRule rule = makeRule(ruleId, SYMBOLS_DEF);

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        // liveRanking은 호출되지 않아야 함 (symbols 룰은 게이팅 없음)
        when(paperLiveSymbolRepository.findByRuleId(ruleId))
            .thenReturn(List.of(new PaperLiveSymbol(ruleId, symbol)));

        PaperAccount acc = makeAccount();
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(acc));

        when(positionRepo.findByAccountIdAndSymbol(ACCOUNT_ID, symbol)).thenReturn(Optional.empty());
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(Collections.emptyList());

        when(intradayRepo.findMaxTsBySymbolAndInterval(symbol, "5m"))
            .thenReturn(Optional.of(NOW.minus(2, ChronoUnit.MINUTES)));
        when(marketDataPort.recentIntradayBars(symbol)).thenReturn(makeBars(symbol, 25));

        // BUY 트리거
        when(ruleEvaluator.evalEntry(any(), any(), any(), anyInt()))
            .thenReturn(new EvalResult(true, List.of(), null, null));
        when(executor.supports(any())).thenReturn(true);
        when(executor.execute(eq(Signal.BUY), any(), eq(symbol), anyDouble(), any(), any()))
            .thenReturn(TradeResult.filled("BUY", symbol, 70000.0, 142.0, null));
        when(snapshotRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateTick(NOW);

        // symbols 룰은 게이팅 없이 BUY 허용
        verify(executor).execute(eq(Signal.BUY), any(), eq(symbol), anyDouble(), any(), any());
        // liveRanking은 symbols 룰에 대해 호출되지 않음
        verify(liveRanking, never()).topVolume(any(), any(), anyInt(), anyBoolean());
    }
}
