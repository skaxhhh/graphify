package com.graphify.trading.paper;

import com.graphify.market.MarketBarIntraday;
import com.graphify.market.MarketBarIntradayRepository;
import com.graphify.market.SymbolNameService;
import com.graphify.trading.paper.dto.PaperPositionItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UIX-02-BE-7: RULE_AGGREGATE 기간 집계 + 단일-run 기여분(오픈포지션 파생) 검증.
 */
@ExtendWith(MockitoExtension.class)
class PaperRunContributionServiceTest {

    @Mock PaperRunRepository          runRepo;
    @Mock PaperTradeRepository        tradeRepo;
    @Mock PaperAccountRepository      accountRepo;
    @Mock PaperPositionRepository     positionRepo;
    @Mock PaperEquitySnapshotRepository snapshotRepo;
    @Mock MarketBarIntradayRepository intradayRepo;
    @Mock SymbolNameService           symbolNameService;

    PaperRunContributionService service;

    static final Long USER_ID    = 1L;
    static final Long RULE_ID    = 3L;
    static final Long RUN_ID     = 7L;
    static final Long ACCOUNT_ID = 11L;
    static final String SYMBOL   = "A005930";

    @BeforeEach
    void setUp() {
        service = new PaperRunContributionService(
                runRepo, tradeRepo, accountRepo, positionRepo,
                snapshotRepo, intradayRepo, symbolNameService);
    }

    // ─── RULE_AGGREGATE ───────────────────────────────────────────────────────

    /**
     * RULE_AGGREGATE: aggregate(ruleId, from, to) → rule_id 스코프 집계.
     * NULL run_id를 가진 구형 거래도 포함 (findByRuleIdAndTradedAtBetween 사용).
     * realizedPnl = SUM(pnl WHERE SELL). tradeCount = COUNT(BUY).
     */
    @Test
    void aggregate_ruleScope_sumsSellPnlAndCountsBuys() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to   = Instant.parse("2026-06-30T23:59:59Z");

        // NULL run_id trades (legacy) included in rule-scoped query
        PaperTrade sell1 = makeTrade(null, "SELL", qty(100), price(72_000), pnl(200_000));
        PaperTrade sell2 = makeTrade(null, "SELL", qty(50),  price(50_000), pnl(41_800));
        PaperTrade buy1  = makeTrade(null, "BUY",  qty(100), price(70_000), null);
        PaperTrade buy2  = makeTrade(RUN_ID, "BUY", qty(50), price(48_000), null);

        when(tradeRepo.findByRuleIdAndTradedAtBetweenOrderByTradedAtDesc(RULE_ID, from, to))
                .thenReturn(List.of(sell1, sell2, buy1, buy2));

        var result = service.aggregate(RULE_ID, from, to);

        assertThat(result.realizedPnl()).isCloseTo(241_800.0, within(0.01));
        assertThat(result.tradeCount()).isEqualTo(2);   // 2 BUY = 2 진입 횟수
        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
    }

    /**
     * aggregate 빈 결과 → 0/0 반환 (NPE 없음).
     */
    @Test
    void aggregate_noTrades_returnsZero() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to   = Instant.parse("2026-06-30T23:59:59Z");
        when(tradeRepo.findByRuleIdAndTradedAtBetweenOrderByTradedAtDesc(RULE_ID, from, to))
                .thenReturn(List.of());

        var result = service.aggregate(RULE_ID, from, to);

        assertThat(result.realizedPnl()).isEqualTo(0.0);
        assertThat(result.tradeCount()).isEqualTo(0);
    }

    // ─── 단일-run 기여분 (closed positions — no open positions) ───────────────

    /**
     * dashboardContribution: run 스코프 realizedPnl, tradeCount 계산.
     * BUY + SELL for same symbol → openSymbols = empty → no unrealizedPnl.
     */
    @Test
    void dashboardContribution_closedPositions_computesRunScopePnl() {
        PaperRun run = mockRun(RUN_ID, RULE_ID, "STOPPED");
        when(runRepo.findById(RUN_ID)).thenReturn(Optional.of(run));

        PaperAccount account = mockAccount(USER_ID, ACCOUNT_ID, 5_000_000.0);
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        // Account-wide positions (for totalEquity) — empty in this test
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(List.of());

        // Run-scoped trades
        PaperTrade sell = makeTrade(RUN_ID, "SELL", qty(100), price(72_000), pnl(200_000));
        PaperTrade buy  = makeTrade(RUN_ID, "BUY",  qty(100), price(70_000), null);
        when(tradeRepo.findByRunIdOrderByTradedAtDesc(RUN_ID)).thenReturn(List.of(sell, buy));

        var result = service.dashboardContribution(USER_ID, RUN_ID);

        assertThat(result.realizedPnl()).isCloseTo(200_000.0, within(0.01));
        assertThat(result.tradeCount()).isEqualTo(1);
        assertThat(result.unrealizedPnl()).isCloseTo(0.0, within(0.01));
        assertThat(result.positions()).isEmpty();
        // totalEquity = cash + 0 positions = 5,000,000
        assertThat(result.totalEquity()).isCloseTo(5_000_000.0, within(0.01));
        assertThat(result.availableCash()).isCloseTo(5_000_000.0, within(0.01));
    }

    // ─── 단일-run 기여분 (open position 파생) ────────────────────────────────

    /**
     * dashboardContribution: BUY without matching SELL → open position derived from paper_positions.
     * unrealizedPnl = qty * (markPrice - avgPrice).
     */
    @Test
    void dashboardContribution_openPosition_derivedFromTrades() {
        PaperRun run = mockRun(RUN_ID, RULE_ID, "RUNNING");
        when(runRepo.findById(RUN_ID)).thenReturn(Optional.of(run));

        PaperAccount account = mockAccount(USER_ID, ACCOUNT_ID, 3_000_000.0);
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        // Run-scoped trades: BUY only → SYMBOL is open
        PaperTrade buy = makeTrade(RUN_ID, "BUY", qty(100), price(70_000), null);
        when(tradeRepo.findByRunIdOrderByTradedAtDesc(RUN_ID)).thenReturn(List.of(buy));

        // Position in paper_positions for open symbol
        PaperPosition pos = new PaperPosition(ACCOUNT_ID, SYMBOL,
                BigDecimal.valueOf(100), BigDecimal.valueOf(70_000));
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(pos));
        when(positionRepo.findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL)).thenReturn(Optional.of(pos));

        // markPrice via intradayRepo
        MarketBarIntraday bar = mock(MarketBarIntraday.class);
        when(bar.getClose()).thenReturn(72_000.0);
        when(intradayRepo.findBySymbolAndIntervalOrderByTsAsc(SYMBOL, "5m")).thenReturn(List.of(bar));
        when(symbolNameService.resolve(SYMBOL)).thenReturn("삼성전자");

        var result = service.dashboardContribution(USER_ID, RUN_ID);

        assertThat(result.tradeCount()).isEqualTo(1);   // 1 BUY = 1 진입
        assertThat(result.realizedPnl()).isCloseTo(0.0, within(0.01));

        // Open position: 100 shares, avgPrice=70000, markPrice=72000
        assertThat(result.positions()).hasSize(1);
        PaperPositionItem pi = result.positions().get(0);
        assertThat(pi.symbol()).isEqualTo(SYMBOL);
        assertThat(pi.markPrice()).isCloseTo(72_000.0, within(0.01));
        assertThat(pi.unrealizedPnl()).isCloseTo(200_000.0, within(0.01));  // 100*(72000-70000)
        assertThat(result.unrealizedPnl()).isCloseTo(200_000.0, within(0.01));

        // totalEquity = cash(3M) + markValue(100*72000=7.2M) = 10.2M
        assertThat(result.totalEquity()).isCloseTo(10_200_000.0, within(0.01));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PaperTrade makeTrade(Long runId, String side, BigDecimal qty, BigDecimal price, BigDecimal pnl) {
        return new PaperTrade(ACCOUNT_ID, RULE_ID, runId, SYMBOL, side, qty, price, pnl, Instant.now());
    }

    private static BigDecimal qty(double v)   { return BigDecimal.valueOf(v); }
    private static BigDecimal price(double v) { return BigDecimal.valueOf(v); }
    private static BigDecimal pnl(double v)   { return BigDecimal.valueOf(v); }

    private PaperRun mockRun(Long id, Long ruleId, String status) {
        PaperRun run = mock(PaperRun.class);
        // lenient: service may or may not consume these fields depending on the operation
        lenient().when(run.getId()).thenReturn(id);
        lenient().when(run.getRuleId()).thenReturn(ruleId);
        lenient().when(run.getStatus()).thenReturn(status);
        return run;
    }

    private PaperAccount mockAccount(Long userId, Long accountId, double cash) {
        PaperAccount acc = mock(PaperAccount.class);
        when(acc.getId()).thenReturn(accountId);
        when(acc.getCash()).thenReturn(BigDecimal.valueOf(cash));
        return acc;
    }
}
