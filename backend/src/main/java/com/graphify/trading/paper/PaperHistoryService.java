package com.graphify.trading.paper;

import com.graphify.trading.paper.dto.PaperTradeHistoryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaperHistoryService {

    private final PaperAccountRepository accountRepo;
    private final PaperTradeRepository   tradeRepo;

    public PaperHistoryService(PaperAccountRepository accountRepo,
                               PaperTradeRepository tradeRepo) {
        this.accountRepo = accountRepo;
        this.tradeRepo   = tradeRepo;
    }

    public List<PaperTradeHistoryItem> getHistory(Long userId) {
        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        if (accountOpt.isEmpty()) {
            return List.of();
        }
        PaperAccount account = accountOpt.get();
        return tradeRepo.findByAccountIdOrderByTradedAtDesc(account.getId()).stream()
                .map(t -> new PaperTradeHistoryItem(
                        t.getId(),
                        t.getTradedAt(),
                        t.getSymbol(),
                        t.getSide(),
                        t.getQty().doubleValue(),
                        t.getPrice().doubleValue(),
                        null,  // fee: paper_trades has no fee column
                        t.getPnl() != null ? t.getPnl().doubleValue() : null))
                .toList();
    }
}
