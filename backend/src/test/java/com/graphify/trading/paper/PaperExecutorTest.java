package com.graphify.trading.paper;

import com.graphify.trading.engine.FillSimulator;
import com.graphify.trading.engine.Signal;
import com.graphify.trading.rule.TradingRule;
import java.math.BigDecimal;
import java.time.Instant;
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
    FillSimulator fillSimulator = new FillSimulator();

    PaperExecutor executor;
    TradingRule rule;
    static final String SYMBOL = "A005930";
    static final double PRICE = 70_000.0;
    static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        executor = new PaperExecutor(accountRepo, positionRepo, tradeRepo, signalLogRepo, fillSimulator);
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
}
