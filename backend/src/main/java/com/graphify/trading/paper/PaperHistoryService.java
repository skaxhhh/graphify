package com.graphify.trading.paper;

import com.graphify.market.SymbolNameService;
import com.graphify.trading.paper.dto.PaperTradeHistoryItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaperHistoryService {

    private final PaperAccountRepository    accountRepo;
    private final PaperTradeRepository      tradeRepo;
    private final PaperSignalLogRepository  signalLogRepo;
    private final SymbolNameService         symbolNameService;

    public PaperHistoryService(PaperAccountRepository accountRepo,
                               PaperTradeRepository tradeRepo,
                               PaperSignalLogRepository signalLogRepo,
                               SymbolNameService symbolNameService) {
        this.accountRepo   = accountRepo;
        this.tradeRepo     = tradeRepo;
        this.signalLogRepo = signalLogRepo;
        this.symbolNameService = symbolNameService;
    }

    public List<PaperTradeHistoryItem> getHistory(Long userId) {
        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        if (accountOpt.isEmpty()) {
            return List.of();
        }
        PaperAccount account = accountOpt.get();
        List<PaperTrade> trades = tradeRepo.findByAccountIdOrderByTradedAtDesc(account.getId());
        return buildHistoryItems(trades);
    }

    /**
     * run-scoped 거래 이력. run_id = :runId (NULL rows 자동 제외).
     * D7 mode 1 — "이 실행만".
     */
    public List<PaperTradeHistoryItem> getHistoryByRun(Long userId, Long runId) {
        List<PaperTrade> trades = tradeRepo.findByRunIdOrderByTradedAtDesc(runId);
        return buildHistoryItems(trades);
    }

    /**
     * rule+기간 범위 거래 이력. rule_id + [from, to] 필터 (NULL run_id 구형 거래 포함).
     * D7 mode 2 — "전략 전체 통합(RULE_AGGREGATE)".
     */
    public List<PaperTradeHistoryItem> getHistoryByRuleAndPeriod(
            Long userId, Long ruleId, Instant from, Instant to) {
        List<PaperTrade> trades =
                tradeRepo.findByRuleIdAndTradedAtBetweenOrderByTradedAtDesc(ruleId, from, to);
        return buildHistoryItems(trades);
    }

    // ─── Private helper ───────────────────────────────────────────────────────

    /**
     * trades 리스트를 PaperTradeHistoryItem 리스트로 변환.
     * 종목명 배치 매핑 + signal_log JOIN(rationale 첨부).
     * JOIN 키: rule_id + symbol + traded_at + side (Pitfall 2: 동일 ts BUY+SELL 방어).
     */
    private List<PaperTradeHistoryItem> buildHistoryItems(List<PaperTrade> trades) {
        // 종목명 배치 매핑 (고유 symbol만 1회 해석 — companies → Naver 폴백)
        Map<String, String> nameBySymbol = symbolNameService.resolveAll(
                trades.stream().map(PaperTrade::getSymbol).distinct().toList());

        return trades.stream()
                .map(t -> {
                    String rationaleJson = null;
                    if (t.getRuleId() != null) {
                        Optional<PaperSignalLog> logOpt = signalLogRepo
                                .findFirstByRuleIdAndSymbolAndTsAndSignal(
                                        t.getRuleId(), t.getSymbol(), t.getTradedAt(), t.getSide());
                        rationaleJson = logOpt.map(PaperSignalLog::getIndicatorSnapshot).orElse(null);
                    }
                    return new PaperTradeHistoryItem(
                            t.getId(),
                            t.getTradedAt(),
                            t.getSymbol(),
                            nameBySymbol.get(t.getSymbol()),
                            t.getSide(),
                            t.getQty().doubleValue(),
                            t.getPrice().doubleValue(),
                            null,  // fee: paper_trades has no fee column
                            t.getPnl() != null ? t.getPnl().doubleValue() : null,
                            rationaleJson);
                })
                .toList();
    }
}
