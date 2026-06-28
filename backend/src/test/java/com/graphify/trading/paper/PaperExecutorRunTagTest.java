package com.graphify.trading.paper;

import com.graphify.market.MarketBarRepository;
import com.graphify.trading.engine.FillSimulator;
import com.graphify.trading.engine.Signal;
import com.graphify.trading.rule.TradingRule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UIX-02-BE-3, UIX-02-BE-4: BUY/SELL 체결 시 paper_trades.run_id = active run id.
 * No-active-run → run_id = null (backward compat; 체결은 정상 진행).
 */
@ExtendWith(MockitoExtension.class)
class PaperExecutorRunTagTest {

    @Mock PaperAccountRepository  accountRepo;
    @Mock PaperPositionRepository positionRepo;
    @Mock PaperTradeRepository    tradeRepo;
    @Mock PaperSignalLogRepository signalLogRepo;
    @Mock MarketBarRepository     marketBarRepo;
    @Mock PaperRunRepository      runRepo;
    FillSimulator fillSimulator = new FillSimulator();

    PaperExecutor executor;
    TradingRule   rule;

    static final String SYMBOL  = "A005930";
    static final double PRICE   = 70_000.0;
    static final Instant NOW    = Instant.now();
    static final Long RULE_ID   = 1L;
    static final Long RUN_ID    = 42L;

    @BeforeEach
    void setUp() {
        executor = new PaperExecutor(accountRepo, positionRepo, tradeRepo, signalLogRepo,
                fillSimulator, marketBarRepo, runRepo);
        // Default: no prev-close → price-limit check conservative (false).
        lenient().when(marketBarRepo.findBySymbolAndTradingDate(any(), any()))
                 .thenReturn(Optional.empty());
        // Default: no active run → null runId (backward compat). Tests that need a run override this.
        lenient().when(runRepo.findFirstByRuleIdAndStatus(any(), any()))
                 .thenReturn(Optional.empty());
        rule = new TradingRule(RULE_ID, "Tag Test Rule", "PAPER", "PAPER_LIVE",
                "{\"version\":1,\"universe\":{\"type\":\"symbols\",\"symbols\":[\"A005930\"]}," +
                "\"entry\":{\"logic\":\"AND\",\"conditions\":[]},\"exit\":null," +
                "\"sizing\":{\"type\":\"fixed_cash\",\"value\":10000000},\"constraints\":null}");
    }

    private PaperAccount accountWithCash(double cash) {
        PaperAccount acc = new PaperAccount(1L, BigDecimal.valueOf(10_000_000));
        acc.setCash(BigDecimal.valueOf(cash));
        return acc;
    }

    /**
     * Pre-create mock BEFORE entering an outer when() chain.
     * Calling mock() + when() inside thenReturn(…) causes UnfinishedStubbingException.
     */
    private PaperRun mockRunWithId(Long id) {
        PaperRun run = mock(PaperRun.class);
        when(run.getId()).thenReturn(id);
        return run;
    }

    // ─── UIX-02-BE-3 ──────────────────────────────────────────────────────────

    /**
     * BUY 체결 + active RUNNING run → PaperTrade.runId = run.id.
     * Note: TradingRule(userId, name, ...) constructor — rule.getId() is null in unit tests
     * (id is set by DB/JPA). We match with isNull() to reflect the actual call.
     */
    @Test
    void buy_activeRun_stampsRunId() {
        // Pre-create mock before the outer when() to avoid UnfinishedStubbingException.
        PaperRun activeRun = mockRunWithId(RUN_ID);
        when(runRepo.findFirstByRuleIdAndStatus(isNull(), eq("RUNNING")))
                .thenReturn(Optional.of(activeRun));

        PaperAccount account = accountWithCash(10_000_000);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());
        ArgumentCaptor<PaperTrade> captor = ArgumentCaptor.forClass(PaperTrade.class);
        when(tradeRepo.save(captor.capture())).thenAnswer(i -> i.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, PRICE, NOW, null);

        assertThat(result.executed()).isTrue();
        PaperTrade saved = captor.getValue();
        assertThat(saved.getSide()).isEqualTo("BUY");
        assertThat(saved.getRunId()).isEqualTo(RUN_ID);
    }

    // ─── UIX-02-BE-4 ──────────────────────────────────────────────────────────

    /**
     * SELL 체결 + active RUNNING run → PaperTrade.runId = run.id, pnl 기록.
     */
    @Test
    void sell_activeRun_stampsRunIdAndPnl() {
        PaperRun activeRun = mockRunWithId(RUN_ID);
        when(runRepo.findFirstByRuleIdAndStatus(isNull(), eq("RUNNING")))
                .thenReturn(Optional.of(activeRun));

        PaperAccount account = accountWithCash(0);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        PaperPosition pos = new PaperPosition(account.getId(), SYMBOL,
                BigDecimal.valueOf(100), BigDecimal.valueOf(70_000));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.of(pos));
        ArgumentCaptor<PaperTrade> captor = ArgumentCaptor.forClass(PaperTrade.class);
        when(tradeRepo.save(captor.capture())).thenAnswer(i -> i.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TradeResult result = executor.execute(Signal.SELL, rule, SYMBOL, 72_000.0, NOW, null);

        assertThat(result.executed()).isTrue();
        PaperTrade saved = captor.getValue();
        assertThat(saved.getSide()).isEqualTo("SELL");
        assertThat(saved.getRunId()).isEqualTo(RUN_ID);
        assertThat(saved.getPnl()).isNotNull();
    }

    // ─── Backward compat ──────────────────────────────────────────────────────

    /**
     * No active run → run_id = null; 체결은 정상 진행 (backward compat).
     * setUp() lenient default returns Optional.empty() — no override needed here.
     */
    @Test
    void buy_noActiveRun_runIdIsNull() {
        // lenient default in setUp returns Optional.empty() for runRepo

        PaperAccount account = accountWithCash(10_000_000);
        when(accountRepo.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepo.findByAccountIdAndSymbol(any(), eq(SYMBOL))).thenReturn(Optional.empty());
        ArgumentCaptor<PaperTrade> captor = ArgumentCaptor.forClass(PaperTrade.class);
        when(tradeRepo.save(captor.capture())).thenAnswer(i -> i.getArgument(0));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TradeResult result = executor.execute(Signal.BUY, rule, SYMBOL, PRICE, NOW, null);

        assertThat(result.executed()).isTrue();
        assertThat(captor.getValue().getRunId()).isNull();
    }
}
