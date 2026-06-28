package com.graphify.trading.paper;

import com.graphify.common.exception.GraphifyException;
import com.graphify.market.MarketBarIntraday;
import com.graphify.market.MarketBarIntradayRepository;
import com.graphify.market.SymbolNameService;
import com.graphify.trading.paper.dto.PaperPositionItem;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * run-scoped 대시보드 기여분 + 2-mode 기간 집계 서비스.
 *
 * D5(단일 계좌 공유) 전제:
 *  - totalEquity / availableCash = 계좌 전체
 *  - realizedPnl / tradeCount   = run 기여분
 *  - openPositions              = paper_trades에서 파생 (positions 테이블에 run_id 추가 금지 — Pitfall 1)
 *
 * Wave 3 컨트롤러에서 RunDashboardDto로 매핑한다.
 */
@Service
@Transactional(readOnly = true)
public class PaperRunContributionService {

    private final PaperRunRepository           runRepo;
    private final PaperTradeRepository         tradeRepo;
    private final PaperAccountRepository       accountRepo;
    private final PaperPositionRepository      positionRepo;
    private final PaperEquitySnapshotRepository snapshotRepo;
    private final MarketBarIntradayRepository  intradayRepo;
    private final SymbolNameService            symbolNameService;

    public PaperRunContributionService(
            PaperRunRepository runRepo,
            PaperTradeRepository tradeRepo,
            PaperAccountRepository accountRepo,
            PaperPositionRepository positionRepo,
            PaperEquitySnapshotRepository snapshotRepo,
            MarketBarIntradayRepository intradayRepo,
            SymbolNameService symbolNameService) {
        this.runRepo          = runRepo;
        this.tradeRepo        = tradeRepo;
        this.accountRepo      = accountRepo;
        this.positionRepo     = positionRepo;
        this.snapshotRepo     = snapshotRepo;
        this.intradayRepo     = intradayRepo;
        this.symbolNameService = symbolNameService;
    }

    // ─── Return types ─────────────────────────────────────────────────────────

    /**
     * 대시보드 탭 기여분 결과. Wave 3에서 RunDashboardDto로 매핑.
     */
    public record RunDashboardContribution(
            double totalEquity,
            double availableCash,
            double realizedPnl,
            int    tradeCount,
            double unrealizedPnl,
            List<PaperPositionItem> positions
    ) {}

    /**
     * RULE_AGGREGATE 기간 집계 결과. Wave 3에서 DTO로 매핑.
     */
    public record RuleAggregateResult(
            double  realizedPnl,
            int     tradeCount,
            Instant from,
            Instant to
    ) {}

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * 단일-run 대시보드 기여분 계산.
     * totalEquity / availableCash = 계좌 전체(D5).
     * realizedPnl / tradeCount = run 스코프.
     * openPositions = run의 BUY symbol 중 SELL 없는 symbol → paper_positions 조회.
     */
    public RunDashboardContribution dashboardContribution(Long userId, Long runId) {
        runRepo.findById(runId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_PAPER_RUN_001", "Run not found: " + runId, HttpStatus.NOT_FOUND));

        PaperAccount account = accountRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("PaperAccount not found for user: " + userId));

        double cash = account.getCash().doubleValue();

        // Account-wide equity: cash + ALL positions marked-to-market (D5)
        List<PaperPosition> allPositions = positionRepo.findByAccountId(account.getId());
        double totalMarketValue = allPositions.stream()
                .mapToDouble(p -> p.getQty().doubleValue()
                        * latestMarkPrice(p.getSymbol(), p.getAvgPrice().doubleValue()))
                .sum();
        double totalEquity = cash + totalMarketValue;

        // Run-scoped trade aggregation
        List<PaperTrade> runTrades = tradeRepo.findByRunIdOrderByTradedAtDesc(runId);

        double realizedPnl = runTrades.stream()
                .filter(t -> "SELL".equals(t.getSide()) && t.getPnl() != null)
                .mapToDouble(t -> t.getPnl().doubleValue())
                .sum();

        int tradeCount = (int) runTrades.stream()
                .filter(t -> "BUY".equals(t.getSide()))
                .count();

        // Open positions: BUY symbols NOT matched by SELL in this run
        Set<String> buySymbols = runTrades.stream()
                .filter(t -> "BUY".equals(t.getSide()))
                .map(PaperTrade::getSymbol)
                .collect(Collectors.toSet());
        Set<String> sellSymbols = runTrades.stream()
                .filter(t -> "SELL".equals(t.getSide()))
                .map(PaperTrade::getSymbol)
                .collect(Collectors.toSet());
        Set<String> openSymbols = new HashSet<>(buySymbols);
        openSymbols.removeAll(sellSymbols);

        // Fetch PaperPosition for each open symbol and mark-to-market
        List<PaperPositionItem> openPositions = openSymbols.stream()
                .map(sym -> positionRepo.findByAccountIdAndSymbol(account.getId(), sym).orElse(null))
                .filter(Objects::nonNull)
                .map(this::toPositionItem)
                .toList();

        double unrealizedPnl = openPositions.stream()
                .mapToDouble(PaperPositionItem::unrealizedPnl)
                .sum();

        return new RunDashboardContribution(
                totalEquity, cash, realizedPnl, tradeCount, unrealizedPnl, openPositions);
    }

    /**
     * RULE_AGGREGATE 2-mode 기간 집계.
     * rule_id + [from, to] 필터 — run_id=NULL 구형 거래 포함(D7 mode 2).
     *
     * @param ruleId 전략 id
     * @param from   기간 시작 (inclusive)
     * @param to     기간 종료 (inclusive)
     */
    public RuleAggregateResult aggregate(Long ruleId, Instant from, Instant to) {
        List<PaperTrade> trades =
                tradeRepo.findByRuleIdAndTradedAtBetweenOrderByTradedAtDesc(ruleId, from, to);

        double realizedPnl = trades.stream()
                .filter(t -> "SELL".equals(t.getSide()) && t.getPnl() != null)
                .mapToDouble(t -> t.getPnl().doubleValue())
                .sum();

        int tradeCount = (int) trades.stream()
                .filter(t -> "BUY".equals(t.getSide()))
                .count();

        return new RuleAggregateResult(realizedPnl, tradeCount, from, to);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private PaperPositionItem toPositionItem(PaperPosition pos) {
        double qty           = pos.getQty().doubleValue();
        double avgPrice      = pos.getAvgPrice().doubleValue();
        double markPrice     = latestMarkPrice(pos.getSymbol(), avgPrice);
        double marketValue   = qty * markPrice;
        double costBasis     = qty * avgPrice;
        double unrealizedPnl = marketValue - costBasis;
        double unrealizedPnlPct = costBasis > 0 ? unrealizedPnl / costBasis * 100.0 : 0.0;
        String companyName   = symbolNameService.resolve(pos.getSymbol());
        return new PaperPositionItem(
                pos.getSymbol(), companyName, qty, avgPrice,
                markPrice, marketValue, unrealizedPnl, unrealizedPnlPct);
    }

    /** 최신 5m 분봉 종가. 분봉 없으면 avgPrice fallback. */
    private double latestMarkPrice(String symbol, double fallback) {
        List<MarketBarIntraday> bars = intradayRepo.findBySymbolAndIntervalOrderByTsAsc(symbol, "5m");
        if (bars.isEmpty()) return fallback;
        return bars.get(bars.size() - 1).getClose();
    }
}
