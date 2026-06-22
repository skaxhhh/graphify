package com.graphify.trading.paper;

import com.graphify.common.exception.GraphifyException;
import com.graphify.trading.engine.MarketDataPort;
import com.graphify.trading.rule.PaperLiveSymbolService;
import com.graphify.trading.rule.TradingRule;
import com.graphify.trading.rule.TradingRuleRepository;
import com.graphify.trading.rule.definition.RuleDefinition;
import com.graphify.trading.rule.dto.RuleResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 룰 생애주기 상태 머신 (Phase 6.5: 2축 상태 모델).
 * config_status: DRAFT ↔ ACTIVE  (설정축)
 * run_status:    STOPPED ↔ RUNNING (운영축)
 *
 * 전이 규칙:
 *   activate:   DRAFT → ACTIVE (백테스트 게이트 없음)
 *   deactivate: ACTIVE/STOPPED → DRAFT (RUNNING이면 차단)
 *   start:      ACTIVE/STOPPED → ACTIVE/RUNNING (paperLiveSymbols 재할당)
 *   stop:       ACTIVE/RUNNING → ACTIVE/STOPPED (config ACTIVE 유지)
 *   copy:        모든 상태 → DRAFT 복사본 생성 (변경 없음)
 */
@Service
@Transactional
public class PaperLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(PaperLifecycleService.class);

    private final TradingRuleRepository ruleRepo;
    private final ObjectMapper objectMapper;
    private final PaperLiveSymbolService paperLiveSymbolService;
    private final MarketDataPort marketData;

    public PaperLifecycleService(
            TradingRuleRepository ruleRepo,
            ObjectMapper objectMapper,
            PaperLiveSymbolService paperLiveSymbolService,
            MarketDataPort marketData) {
        this.ruleRepo = ruleRepo;
        this.objectMapper = objectMapper;
        this.paperLiveSymbolService = paperLiveSymbolService;
        this.marketData = marketData;
    }

    // ─── 신규 2축 메서드 ────────────────────────────────────────────────────────

    /** config축: DRAFT → ACTIVE. 백테스트 게이트 없음 (RULE-01 폐지). */
    public RuleResponse activate(Long userId, Long ruleId) {
        TradingRule rule = findOwned(userId, ruleId);
        if (!"DRAFT".equals(rule.getConfigStatus())) {
            throw new GraphifyException("ERR_LIFECYCLE_001",
                "DRAFT 상태인 룰만 ACTIVE로 전환할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        rule.setConfigStatus("ACTIVE");
        return toResponse(ruleRepo.save(rule));
    }

    /** config축: ACTIVE/STOPPED → DRAFT. RUNNING이면 먼저 stop 필요. */
    public RuleResponse deactivate(Long userId, Long ruleId) {
        TradingRule rule = findOwned(userId, ruleId);
        if (!"ACTIVE".equals(rule.getConfigStatus())) {
            throw new GraphifyException("ERR_LIFECYCLE_001",
                "ACTIVE 상태인 룰만 DRAFT로 하향할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        if ("RUNNING".equals(rule.getRunStatus())) {
            throw new GraphifyException("ERR_LIFECYCLE_006",
                "RUNNING 중인 룰은 하향할 수 없습니다. 먼저 중지하세요.", HttpStatus.BAD_REQUEST);
        }
        rule.setConfigStatus("DRAFT");
        return toResponse(ruleRepo.save(rule));
    }

    /** run축: ACTIVE/STOPPED → ACTIVE/RUNNING. paperLiveSymbols 재할당. */
    public RuleResponse start(Long userId, Long ruleId) {
        TradingRule rule = findOwned(userId, ruleId);
        if (!"ACTIVE".equals(rule.getConfigStatus())) {
            throw new GraphifyException("ERR_LIFECYCLE_007",
                "ACTIVE 상태인 룰만 시작할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        if ("RUNNING".equals(rule.getRunStatus())) {
            throw new GraphifyException("ERR_LIFECYCLE_008",
                "이미 RUNNING 상태인 룰입니다.", HttpStatus.BAD_REQUEST);
        }
        List<String> symbols = resolveSymbols(rule);
        if (symbols.isEmpty()) {
            throw new GraphifyException("ERR_LIFECYCLE_005",
                "룰의 유니버스 종목을 확인할 수 없습니다. 먼저 종목 데이터를 수집하세요.", HttpStatus.BAD_REQUEST);
        }
        rule.setRunStatus("RUNNING");
        TradingRule saved = ruleRepo.save(rule);
        paperLiveSymbolService.assignSymbols(saved.getId(), symbols);
        return toResponse(saved);
    }

    /** run축: ACTIVE/RUNNING → ACTIVE/STOPPED. config ACTIVE 유지 (PAUSED 없음). */
    public RuleResponse stop(Long userId, Long ruleId) {
        TradingRule rule = findOwned(userId, ruleId);
        if (!"RUNNING".equals(rule.getRunStatus())) {
            throw new GraphifyException("ERR_LIFECYCLE_009",
                "RUNNING 상태인 룰만 중지할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        rule.setRunStatus("STOPPED");
        TradingRule saved = ruleRepo.save(rule);
        paperLiveSymbolService.deactivateRule(saved.getId());
        return toResponse(saved);
    }

    // ─── 기존 메서드 (deprecated — 신규 메서드로 위임) ──────────────────────────

    /**
     * @deprecated promote → activate 사용 권장.
     */
    @Deprecated
    public RuleResponse promote(Long userId, Long ruleId) {
        return activate(userId, ruleId);
    }

    /**
     * @deprecated pause → stop 사용 권장.
     */
    @Deprecated
    public RuleResponse pause(Long userId, Long ruleId) {
        return stop(userId, ruleId);
    }

    /**
     * @deprecated resume → start 사용 권장.
     */
    @Deprecated
    public RuleResponse resume(Long userId, Long ruleId) {
        return start(userId, ruleId);
    }

    /** 모든 상태 → DRAFT 복사본 생성. 원본 symbols는 유지 (copy는 비파괴적). */
    public RuleResponse copy(Long userId, Long ruleId) {
        TradingRule original = findOwned(userId, ruleId);
        TradingRule copy = new TradingRule(
            userId,
            "복사본 - " + original.getName(),
            original.getMode(),
            "DRAFT",
            original.getDefinition()
        );
        return toResponse(ruleRepo.save(copy));
    }

    /**
     * 룰의 유니버스 정의로부터 종목 목록을 결정한다. mode/status 무관 (SEAM 3).
     * Phase 8 LIVE 승격도 이 메서드를 재사용한다.
     *
     * volume_top_n: symbolsByMarket(market) ∪ additionalSymbols 전체 후보군 반환
     * symbols/watchlist: u.symbols() 그대로 반환
     */
    private List<String> resolveSymbols(TradingRule rule) {
        try {
            RuleDefinition def = objectMapper.readValue(rule.getDefinition(), RuleDefinition.class);
            RuleDefinition.Universe u = def.universe();
            if (u == null) return List.of();
            if ("volume_top_n".equals(u.type())) {
                String market = u.market() != null ? u.market() : "KOSPI";
                LinkedHashSet<String> all = new LinkedHashSet<>(marketData.symbolsByMarket(market));
                if (u.additionalSymbols() != null) {
                    all.addAll(u.additionalSymbols());
                }
                return List.copyOf(all);
            }
            // type="symbols" or "watchlist"
            return u.symbols() != null ? u.symbols() : List.of();
        } catch (Exception e) {
            log.warn("Cannot parse rule definition for rule {}: {}", rule.getId(), e.getMessage());
            return List.of();
        }
    }

    private TradingRule findOwned(Long userId, Long ruleId) {
        return ruleRepo.findByIdAndUserId(ruleId, userId)
            .orElseThrow(() -> new GraphifyException(
                "ERR_RULE_002", "룰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private RuleResponse toResponse(TradingRule rule) {
        com.fasterxml.jackson.databind.JsonNode definition;
        try {
            definition = objectMapper.readTree(rule.getDefinition());
        } catch (JsonProcessingException e) {
            definition = objectMapper.createObjectNode();
        }
        return new RuleResponse(
            rule.getId(), rule.getName(), rule.getMode(), rule.getStatus(),
            rule.isBacktested(), definition, rule.getPromotedFrom(),
            rule.getCreatedAt(), rule.getUpdatedAt(),
            rule.getConfigStatus(), rule.getRunStatus()
        );
    }
}
