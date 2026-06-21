package com.graphify.trading.rule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperLiveSymbolRepository extends JpaRepository<PaperLiveSymbol, Long> {

    List<PaperLiveSymbol> findByRuleId(Long ruleId);

    void deleteByRuleId(Long ruleId);

    @Query("SELECT DISTINCT p.symbol FROM PaperLiveSymbol p WHERE p.ruleId IN :ruleIds")
    List<String> findDistinctSymbolsByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
