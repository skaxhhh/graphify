package com.graphify.trading.paper;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperSignalLogRepository extends JpaRepository<PaperSignalLog, Long> {
    List<PaperSignalLog> findByRuleIdOrderByTsDesc(Long ruleId);
    List<PaperSignalLog> findTop50ByOrderByTsDesc();
}
