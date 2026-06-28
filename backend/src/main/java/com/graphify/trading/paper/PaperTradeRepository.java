package com.graphify.trading.paper;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperTradeRepository extends JpaRepository<PaperTrade, Long> {
    List<PaperTrade> findByAccountIdOrderByTradedAtDesc(Long accountId);
    List<PaperTrade> findByRuleIdOrderByTradedAtDesc(Long ruleId);

    /**
     * run-scoped query: WHERE run_id = :runId (NULL rows are automatically excluded by Spring Data).
     * Pitfall 6: do NOT use this for RULE_AGGREGATE mode (legacy NULL run_id trades would be excluded).
     */
    List<PaperTrade> findByRunIdOrderByTradedAtDesc(Long runId);

    /**
     * RULE_AGGREGATE mode: WHERE rule_id = :ruleId AND traded_at BETWEEN :from AND :to.
     * Includes legacy trades with run_id = NULL (go-forward only backfill strategy — D7).
     */
    List<PaperTrade> findByRuleIdAndTradedAtBetweenOrderByTradedAtDesc(Long ruleId, Instant from, Instant to);
}
