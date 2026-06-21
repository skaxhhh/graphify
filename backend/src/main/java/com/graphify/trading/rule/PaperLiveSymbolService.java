package com.graphify.trading.rule;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PAPER_LIVE 활성 룰의 종목 목록을 관리한다.
 * 룰 승격 시 assignSymbols() 호출, 룰 비활성화 시 deactivateRule() 호출.
 * 스케줄러 틱마다 activeSymbolsUnion()으로 현재 수집 대상 합집합을 조회한다.
 */
@Service
public class PaperLiveSymbolService {

    private static final Logger log = LoggerFactory.getLogger(PaperLiveSymbolService.class);

    private final PaperLiveSymbolRepository symbolRepository;
    private final TradingRuleRepository ruleRepository;

    public PaperLiveSymbolService(
            PaperLiveSymbolRepository symbolRepository,
            TradingRuleRepository ruleRepository) {
        this.symbolRepository = symbolRepository;
        this.ruleRepository = ruleRepository;
    }

    /** 룰 승격(PAPER_LIVE) 시 종목 목록 저장. 기존 목록은 교체(delete + insert). */
    @Transactional
    public void assignSymbols(Long ruleId, Collection<String> symbols) {
        symbolRepository.deleteByRuleId(ruleId);
        for (String symbol : symbols) {
            symbolRepository.save(new PaperLiveSymbol(ruleId, symbol));
        }
        log.info("Assigned {} symbols to PAPER_LIVE rule {}", symbols.size(), ruleId);
    }

    /** 룰 비활성화(PAUSED/종료) 시 종목 목록 제거. */
    @Transactional
    public void deactivateRule(Long ruleId) {
        symbolRepository.deleteByRuleId(ruleId);
        log.info("Removed paper_live_symbols for rule {}", ruleId);
    }

    /**
     * 현재 PAPER_LIVE 상태인 모든 룰의 종목 합집합 반환.
     * 스케줄러 틱마다 호출 — DB 조회이므로 캐시 없음 (틱 간격 5분, DB 부하 미미).
     */
    @Transactional(readOnly = true)
    public Set<String> activeSymbolsUnion() {
        List<Long> activeRuleIds = ruleRepository.findAll().stream()
            .filter(r -> RuleStatus.isLiveActive(r.getStatus()))
            .map(TradingRule::getId)
            .toList();
        if (activeRuleIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(
            symbolRepository.findDistinctSymbolsByRuleIds(activeRuleIds)
        );
    }
}
