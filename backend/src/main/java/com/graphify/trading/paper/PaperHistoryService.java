package com.graphify.trading.paper;

import com.graphify.trading.paper.dto.PaperTradeHistoryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaperHistoryService {

    private final PaperAccountRepository    accountRepo;
    private final PaperTradeRepository      tradeRepo;
    private final PaperSignalLogRepository  signalLogRepo;

    public PaperHistoryService(PaperAccountRepository accountRepo,
                               PaperTradeRepository tradeRepo,
                               PaperSignalLogRepository signalLogRepo) {
        this.accountRepo   = accountRepo;
        this.tradeRepo     = tradeRepo;
        this.signalLogRepo = signalLogRepo;
    }

    public List<PaperTradeHistoryItem> getHistory(Long userId) {
        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        if (accountOpt.isEmpty()) {
            return List.of();
        }
        PaperAccount account = accountOpt.get();
        return tradeRepo.findByAccountIdOrderByTradedAtDesc(account.getId()).stream()
                .map(t -> {
                    // JOIN: rule_id + symbol + ts(=traded_at) + signal(=side)
                    // Pitfall 2: signal=side 조건 포함으로 동일 ts BUY+SELL 방어
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
