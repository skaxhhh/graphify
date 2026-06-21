package com.graphify.trading.paper;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperTradeRepository extends JpaRepository<PaperTrade, Long> {
    List<PaperTrade> findByAccountIdOrderByTradedAtDesc(Long accountId);
    List<PaperTrade> findByRuleIdOrderByTradedAtDesc(Long ruleId);
}
