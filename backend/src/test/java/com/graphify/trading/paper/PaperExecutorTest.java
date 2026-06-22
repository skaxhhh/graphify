package com.graphify.trading.paper;

import com.graphify.market.MarketBar;
import com.graphify.market.MarketBarRepository;
import com.graphify.trading.engine.FillSimulator;
import com.graphify.trading.engine.Signal;
import com.graphify.trading.rule.TradingRule;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperExecutorTest {

    @Mock PaperAccountRepository accountRepo;
    @Mock PaperPositionRepository positionRepo;
    @Mock PaperTradeRepository tradeRepo;
    @Mock PaperSignalLogRepository signalLogRepo;
    @Mock MarketBarRepository marketBarRepo;
    FillSimulator fillSimulator = new FillSimulator();

    PaperExecutor executor;
    TradingRule rule;
    static final String SYMBOL = "A005930";
    static final double PRICE = 70_000.0;
    static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        executor = new PaperExecutor(accountRepo, positionRepo, tradeRepo, signalLogRepo, fillSimulator, marketBarRepo);
        // Default: no prev-close data → price limit check returns false (conservative)
        // lenient() because HOLD path never reaches the price limit check
        lenient().when(marketBarRepo.findBySymbolAndTradingDate(any(), any())).thenReturn(Optional.empty());
        rule = new TradingRule(1L, "Test Rule", "PAPER", "PAPER_LIVE",
            "{\"version\":1,\"universe\":{\"type\":\"symbols\",\"symbols\":[\"A005930\"]}," +
            "\"entry\":{\"logic\":\"AND\",\"conditions\":[]},\"exit\":null," +
            "\"sizing\":{\"type\":\"fixed_cash\",\"value\":10000000},\"constraints\":null}");
    }

    private PaperAccount accountWithCash(double cash) {
        PaperAccount acc = new PaperAccount(1L, BigDecimal.valueOf(10_000_000));
        acc.setCash(BigDecimal.valueOf(cash));
        return acc;
    }

    @Test
    void buy_success_creates_position_and_deducts_cash() {
        PaperAccount account = accountWithCash(10_000_000);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());
        when(tradeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, PRICE, NOW, null);

        assertThat(result.executed()).isTrue();
        assertThat(result.signal()).isEqualTo("BUY");
        assertThat(result.qty()).isGreaterThan(0);
        verify(positionRepo).save(any(PaperPosition.class));
        verify(tradeRepo).save(any(PaperTrade.class));
        verify(accountRepo).save(account);
    }

    @Test
    void buy_already_holds_returns_skipped() {
        PaperAccount account = accountWithCash(10_000_000);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        PaperPosition existing = new PaperPosition(account.getId(), SYMBOL,
            BigDecimal.valueOf(100), BigDecimal.valueOf(70_000));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.of(existing));

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, PRICE, NOW, null);

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("ALREADY_HOLDS");
        verify(tradeRepo, never()).save(any());
    }

    @Test
    void buy_insufficient_cash_returns_skipped() {
        PaperAccount account = accountWithCash(0);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, PRICE, NOW, null);

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("INSUFFICIENT_CASH");
    }

    @Test
    void sell_success_removes_position_and_records_pnl() {
        PaperAccount account = accountWithCash(0);  // all in position
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        PaperPosition pos = new PaperPosition(account.getId(), SYMBOL,
            BigDecimal.valueOf(100), BigDecimal.valueOf(70_000));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.of(pos));
        when(tradeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TradeResult result = executor.execute(Signal.SELL, rule, SYMBOL, 72_000.0, NOW, null);

        assertThat(result.executed()).isTrue();
        assertThat(result.pnl()).isGreaterThan(0);  // sold at 72000, bought at 70000
        verify(positionRepo).deleteByAccountIdAndSymbol(any(), eq(SYMBOL));
    }

    @Test
    void sell_no_position_returns_skipped() {
        PaperAccount account = accountWithCash(10_000_000);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());

        TradeResult result = executor.execute(Signal.SELL, rule, SYMBOL, PRICE, NOW, null);

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("NO_POSITION");
    }

    @Test
    void hold_always_returns_skipped() {
        TradeResult result = executor.execute(Signal.HOLD, rule, SYMBOL, PRICE, NOW, null);

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("HOLD");
        verifyNoInteractions(accountRepo, positionRepo, tradeRepo);
    }

    // ─── MON-05: PRICE_LIMIT_PENDING 테스트 ──────────────────────────────────

    /**
     * 상한가: 전일 종가 대비 +30% — 체결 불가 → executed=false, PRICE_LIMIT_PENDING.
     * 포지션 변동 없음, signal log에 PENDING 기록.
     */
    @Test
    void buy_priceLimitUp_returnsSkippedPending() {
        double prevClose = 70_000.0;
        double limitUpPrice = prevClose * 1.30; // +30% > 29.5% threshold
        stubPrevClose(prevClose);

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, limitUpPrice, NOW, null);

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("PRICE_LIMIT_PENDING");
        verify(positionRepo, never()).save(any());
        verify(tradeRepo, never()).save(any());
        verify(signalLogRepo).save(argThat(log ->
                "PENDING".equals(log.getSignal()) && !log.isExecuted()));
    }

    /**
     * 하한가: 전일 종가 대비 -30% — 체결 불가 → executed=false, PRICE_LIMIT_PENDING.
     */
    @Test
    void sell_priceLimitDown_returnsSkippedPending() {
        double prevClose = 70_000.0;
        double limitDownPrice = prevClose * 0.70; // -30% > 29.5% threshold
        stubPrevClose(prevClose);

        TradeResult result = executor.execute(Signal.SELL, rule, SYMBOL, limitDownPrice, NOW, null);

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("PRICE_LIMIT_PENDING");
        verify(positionRepo, never()).deleteByAccountIdAndSymbol(any(), any());
        verify(signalLogRepo).save(argThat(log ->
                "PENDING".equals(log.getSignal()) && !log.isExecuted()));
    }

    /**
     * 정상 변동률 (+5%) — 체결 정상 진행.
     */
    @Test
    void buy_normalPriceChange_executesNormally() {
        double prevClose = 70_000.0;
        double normalPrice = prevClose * 1.05; // +5%, well below 29.5%
        stubPrevClose(prevClose);

        PaperAccount account = accountWithCash(10_000_000);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());
        when(tradeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, normalPrice, NOW, null);

        assertThat(result.executed()).isTrue();
        assertThat(result.signal()).isEqualTo("BUY");
        verify(positionRepo).save(any(PaperPosition.class));
    }

    /**
     * 정확히 29.5% 변동 — 한계값(threshold)이므로 PRICE_LIMIT_PENDING 처리.
     */
    @Test
    void buy_exactThreshold_returnsSkippedPending() {
        double prevClose = 70_000.0;
        double thresholdPrice = prevClose * (1 + PaperExecutor.PRICE_LIMIT_THRESHOLD); // exactly 29.5%
        stubPrevClose(prevClose);

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, thresholdPrice, NOW, null);

        assertThat(result.executed()).isFalse();
        assertThat(result.reason()).isEqualTo("PRICE_LIMIT_PENDING");
    }

    /**
     * isPriceLimitPending: prevClose 데이터 없으면 false (보수적 처리 — 체결 허용).
     */
    @Test
    void isPriceLimitPending_noPrevClose_returnsFalse() {
        when(marketBarRepo.findBySymbolAndTradingDate(any(), any())).thenReturn(Optional.empty());
        assertThat(executor.isPriceLimitPending(SYMBOL, 70_000.0, NOW)).isFalse();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void stubPrevClose(double prevClose) {
        LocalDate tickDate = NOW.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        LocalDate prevDate = tickDate.minusDays(1);
        MarketBar bar = new MarketBar(SYMBOL, prevDate, prevClose, prevClose, prevClose, prevClose, 1000L, "YAHOO");
        when(marketBarRepo.findBySymbolAndTradingDate(SYMBOL, prevDate)).thenReturn(Optional.of(bar));
    }
}
