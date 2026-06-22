package com.graphify.trading.paper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperSignalLogRepository extends JpaRepository<PaperSignalLog, Long> {
    List<PaperSignalLog> findByRuleIdOrderByTsDesc(Long ruleId);
    List<PaperSignalLog> findTop50ByOrderByTsDesc();

    /**
     * paper_trades ↔ paper_signal_log JOIN 키 (RESEARCH Discretion 5 + Pitfall 2):
     * rule_id + symbol + ts(=traded_at) + signal(=side) — 동일 ts BUY+SELL 방어.
     */
    Optional<PaperSignalLog> findFirstByRuleIdAndSymbolAndTsAndSignal(
            Long ruleId, String symbol, Instant ts, String signal);
}
