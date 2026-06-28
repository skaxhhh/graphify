package com.graphify.trading.paper;

import com.graphify.common.exception.GraphifyException;
import com.graphify.market.MarketBarIntraday;
import com.graphify.market.MarketBarIntradayRepository;
import com.graphify.market.SymbolNameService;
import com.graphify.trading.paper.dto.PaperPositionItem;
import com.graphify.trading.paper.dto.PaperTradeHistoryItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UIX-02-BE-6: GET /runs/:id/dashboard 검증 — run 기여분 + account 전체 equity.
 *
 * PaperRunContributionService.dashboardContribution() 과
 * PaperHistoryService.getHistoryByRun/ByRuleAndPeriod() 를 직접 테스트한다.
 * (컨트롤러는 얇은 매핑 래퍼이므로 서비스 계층 검증으로 충분.)
 */
@ExtendWith(MockitoExtension.class)
class PaperRunDashboardControllerTest {

    // ─── Contribution service mocks ───────────────────────────────────────────
    @Mock PaperRunRepository           runRepo;
    @Mock PaperTradeRepository         tradeRepo;
    @Mock PaperAccountRepository       accountRepo;
    @Mock PaperPositionRepository      positionRepo;
    @Mock PaperEquitySnapshotRepository snapshotRepo;
    @Mock MarketBarIntradayRepository  intradayRepo;
    @Mock SymbolNameService            symbolNameService;

    // History service reuses same repos (no new mocks needed for signal log)
    @Mock PaperSignalLogRepository     signalLogRepo;

    PaperRunContributionService contributionService;
    PaperHistoryService         historyService;

    static final Long USER_ID    = 1L;
    static final Long RULE_ID    = 3L;
    static final Long RUN_ID     = 7L;
    static final Long ACCOUNT_ID = 11L;
    static final String SYMBOL   = "A005930";

    @BeforeEach
    void setUp() {
        contributionService = new PaperRunContributionService(
                runRepo, tradeRepo, accountRepo, positionRepo,
                snapshotRepo, intradayRepo, symbolNameService);
        historyService = new PaperHistoryService(
                accountRepo, tradeRepo, signalLogRepo, symbolNameService);
    }

    // ─── Dashboard: run 기여분 + account 전체 equity ──────────────────────────

    /**
     * dashboardContribution: totalEquity = cash + ALL account positions (account-wide, D5).
     * realizedPnl / tradeCount = run-scoped (run_id 스코프).
     */
    @Test
    void dashboardContribution_returnsRunScopedPnlAndAccountWideEquity() {
        PaperRun run = mockRun(RUN_ID, RULE_ID, "RUNNING");
        when(runRepo.findById(RUN_ID)).thenReturn(Optional.of(run));

        PaperAccount account = mockAccount(ACCOUNT_ID, 3_000_000.0);
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        // Account-wide positions (D5: total equity includes ALL positions)
        PaperPosition pos = new PaperPosition(ACCOUNT_ID, SYMBOL,
                BigDecimal.valueOf(100), BigDecimal.valueOf(70_000));
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(pos));
        // lenient: BUY+SELL for same symbol → openSymbols=empty → per-symbol lookups skipped
        lenient().when(positionRepo.findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL))
                .thenReturn(Optional.of(pos));

        // Latest intraday bar for totalEquity mark-to-market (ALL positions)
        MarketBarIntraday bar = mock(MarketBarIntraday.class);
        when(bar.getClose()).thenReturn(73_000.0);
        when(intradayRepo.findBySymbolAndIntervalOrderByTsAsc(SYMBOL, "5m"))
                .thenReturn(List.of(bar));
        lenient().when(symbolNameService.resolve(SYMBOL)).thenReturn("삼성전자"); // only called for open positions

        // Run-scoped trades: 1 BUY (open position), 1 SELL (200,000 pnl)
        PaperTrade buy  = makeTrade(RUN_ID, "BUY",  100, 70_000, null);
        PaperTrade sell = makeTrade(RUN_ID, "SELL", 100, 72_000, 200_000.0);
        when(tradeRepo.findByRunIdOrderByTradedAtDesc(RUN_ID)).thenReturn(List.of(sell, buy));

        var result = contributionService.dashboardContribution(USER_ID, RUN_ID);

        // Account-wide equity: cash(3M) + position(100 * 73,000 = 7.3M) = 10.3M
        assertThat(result.totalEquity()).isCloseTo(10_300_000.0, within(0.01));
        assertThat(result.availableCash()).isCloseTo(3_000_000.0, within(0.01));

        // run-scoped: 1 BUY, 1 SELL with 200k pnl
        // Note: BUY symbol also in SELL → openSymbols = empty (both BUY+SELL present)
        assertThat(result.tradeCount()).isEqualTo(1);   // 1 BUY
        assertThat(result.realizedPnl()).isCloseTo(200_000.0, within(0.01));
    }

    /**
     * dashboardContribution: open positions list contains run's open symbols (BUY without SELL).
     */
    @Test
    void dashboardContribution_openPositions_derivedFromRunTrades() {
        PaperRun run = mockRun(RUN_ID, RULE_ID, "RUNNING");
        when(runRepo.findById(RUN_ID)).thenReturn(Optional.of(run));

        PaperAccount account = mockAccount(ACCOUNT_ID, 5_000_000.0);
        when(accountRepo.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        // Account-wide: 1 position
        PaperPosition pos = new PaperPosition(ACCOUNT_ID, SYMBOL,
                BigDecimal.valueOf(50), BigDecimal.valueOf(68_000));
        when(positionRepo.findByAccountId(ACCOUNT_ID)).thenReturn(List.of(pos));
        when(positionRepo.findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL))
                .thenReturn(Optional.of(pos));

        // Only BUY in run → SYMBOL is open
        PaperTrade buy = makeTrade(RUN_ID, "BUY", 50, 68_000, null);
        when(tradeRepo.findByRunIdOrderByTradedAtDesc(RUN_ID)).thenReturn(List.of(buy));

        MarketBarIntraday bar = mock(MarketBarIntraday.class);
        when(bar.getClose()).thenReturn(71_000.0);
        when(intradayRepo.findBySymbolAndIntervalOrderByTsAsc(SYMBOL, "5m"))
                .thenReturn(List.of(bar));
        when(symbolNameService.resolve(SYMBOL)).thenReturn("삼성전자");

        var result = contributionService.dashboardContribution(USER_ID, RUN_ID);

        // Positions list should contain the open symbol
        assertThat(result.positions()).hasSize(1);
        PaperPositionItem pi = result.positions().get(0);
        assertThat(pi.symbol()).isEqualTo(SYMBOL);
        assertThat(pi.markPrice()).isCloseTo(71_000.0, within(0.01));
        // unrealizedPnl = 50 * (71,000 - 68,000) = 150,000
        assertThat(pi.unrealizedPnl()).isCloseTo(150_000.0, within(0.01));
        assertThat(result.unrealizedPnl()).isCloseTo(150_000.0, within(0.01));
    }

    /**
     * dashboardContribution: run not found → GraphifyException NOT_FOUND (ERR_PAPER_RUN_001).
     */
    @Test
    void dashboardContribution_runNotFound_throwsNotFound() {
        when(runRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contributionService.dashboardContribution(USER_ID, 99L))
                .isInstanceOf(GraphifyException.class)
                .satisfies(ex -> {
                    GraphifyException gex = (GraphifyException) ex;
                    assertThat(gex.getCode()).isEqualTo("ERR_PAPER_RUN_001");
                    assertThat(gex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ─── History: run-scoped vs RULE_AGGREGATE ────────────────────────────────

    /**
     * getHistoryByRun: 거래 이력 run_id 스코프 조회, rationale JOIN.
     */
    @Test
    void getHistoryByRun_returnRunScopedTrades() {
        PaperTrade buy = makeTrade(RUN_ID, "BUY", 100, 70_000, null);
        when(tradeRepo.findByRunIdOrderByTradedAtDesc(RUN_ID)).thenReturn(List.of(buy));
        when(symbolNameService.resolveAll(any())).thenReturn(java.util.Map.of(SYMBOL, "삼성전자"));
        when(signalLogRepo.findFirstByRuleIdAndSymbolAndTsAndSignal(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        List<PaperTradeHistoryItem> result = historyService.getHistoryByRun(USER_ID, RUN_ID);

        assertThat(result).hasSize(1);
        PaperTradeHistoryItem item = result.get(0);
        assertThat(item.symbol()).isEqualTo(SYMBOL);
        assertThat(item.side()).isEqualTo("BUY");
        assertThat(item.qty()).isCloseTo(100.0, within(0.01));
        assertThat(item.companyName()).isEqualTo("삼성전자");
    }

    /**
     * getHistoryByRuleAndPeriod: RULE_AGGREGATE 모드 — rule_id + 기간 필터, NULL run_id 포함.
     */
    @Test
    void getHistoryByRuleAndPeriod_returnsRuleAndPeriodScopedTrades() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to   = Instant.parse("2026-06-30T23:59:59Z");

        PaperTrade buy  = makeTrade(null, "BUY",  100, 70_000, null);   // NULL run_id (legacy)
        PaperTrade sell = makeTrade(RUN_ID, "SELL", 100, 73_000, 300_000.0);
        when(tradeRepo.findByRuleIdAndTradedAtBetweenOrderByTradedAtDesc(RULE_ID, from, to))
                .thenReturn(List.of(sell, buy));
        when(symbolNameService.resolveAll(any())).thenReturn(java.util.Map.of(SYMBOL, "삼성전자"));
        when(signalLogRepo.findFirstByRuleIdAndSymbolAndTsAndSignal(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        List<PaperTradeHistoryItem> result =
                historyService.getHistoryByRuleAndPeriod(USER_ID, RULE_ID, from, to);

        // Both trades returned (including legacy NULL run_id trade)
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(PaperTradeHistoryItem::side).toList())
                .containsExactlyInAnyOrder("SELL", "BUY");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PaperRun mockRun(Long id, Long ruleId, String status) {
        PaperRun run = mock(PaperRun.class);
        lenient().when(run.getId()).thenReturn(id);
        lenient().when(run.getRuleId()).thenReturn(ruleId);
        lenient().when(run.getStatus()).thenReturn(status);
        return run;
    }

    private PaperAccount mockAccount(Long accountId, double cash) {
        PaperAccount acc = mock(PaperAccount.class);
        when(acc.getId()).thenReturn(accountId);
        when(acc.getCash()).thenReturn(BigDecimal.valueOf(cash));
        return acc;
    }

    private PaperTrade makeTrade(Long runId, String side, int qty, double price, Double pnl) {
        return new PaperTrade(
                ACCOUNT_ID, RULE_ID, runId,
                SYMBOL, side,
                BigDecimal.valueOf(qty),
                BigDecimal.valueOf(price),
                pnl != null ? BigDecimal.valueOf(pnl) : null,
                Instant.now());
    }
}
