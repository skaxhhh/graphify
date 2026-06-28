package com.graphify.trading.paper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.trading.rule.TradingRuleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실행 이력 리스트 집계 서비스.
 *
 * 컬럼 계산: realizedPnl / returnPct / tradeCount / finalEquity / runIndex(회차).
 * Wave 3 컨트롤러에서 RunSummaryDto로 매핑한다.
 */
@Service
@Transactional(readOnly = true)
public class PaperRunListService {

    private static final Logger log = LoggerFactory.getLogger(PaperRunListService.class);

    private final PaperRunRepository           runRepo;
    private final TradingRuleRepository        ruleRepo;
    private final PaperTradeRepository         tradeRepo;
    private final PaperAccountRepository       accountRepo;
    private final PaperEquitySnapshotRepository snapshotRepo;
    private final ObjectMapper                 objectMapper;

    public PaperRunListService(
            PaperRunRepository runRepo,
            TradingRuleRepository ruleRepo,
            PaperTradeRepository tradeRepo,
            PaperAccountRepository accountRepo,
            PaperEquitySnapshotRepository snapshotRepo,
            ObjectMapper objectMapper) {
        this.runRepo      = runRepo;
        this.ruleRepo     = ruleRepo;
        this.tradeRepo    = tradeRepo;
        this.accountRepo  = accountRepo;
        this.snapshotRepo = snapshotRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * 내부 집계 결과 레코드. Wave 3 컨트롤러에서 RunSummaryDto로 변환한다.
     */
    public record RunListItem(
            Long    runId,
            Long    ruleId,
            String  ruleName,
            String  status,
            Instant startedAt,
            Instant endedAt,
            List<String> universe,
            double  realizedPnl,
            double  returnPct,
            int     tradeCount,
            double  finalEquity,
            int     runIndex     // 동일 전략 내 회차 번호 (1-based, startedAt ASC 순)
    ) {}

    /**
     * userId의 전체 실행 이력을 집계하여 반환한다 (최신 순).
     */
    public List<RunListItem> listRuns(Long userId) {
        List<PaperRun> runs = runRepo.findByUserIdOrderByStartedAtDesc(userId);
        if (runs.isEmpty()) {
            return List.of();
        }

        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        Long accountId = accountOpt.map(PaperAccount::getId).orElse(null);

        // Current equity via latest snapshot (used for RUNNING runs)
        double currentEquity = resolveCurrentEquity(accountId, accountOpt);

        // Pre-compute run order per rule (startedAt ASC → 1-based index)
        Map<Long, List<Long>> ruleRunOrder = computeRunOrder(runs);

        List<RunListItem> result = new ArrayList<>();
        for (PaperRun run : runs) {
            String ruleName = ruleRepo.findById(run.getRuleId())
                    .map(r -> r.getName())
                    .orElse("(삭제된 전략)");

            List<PaperTrade> trades = tradeRepo.findByRunIdOrderByTradedAtDesc(run.getId());

            double realizedPnl = trades.stream()
                    .filter(t -> "SELL".equals(t.getSide()) && t.getPnl() != null)
                    .mapToDouble(t -> t.getPnl().doubleValue())
                    .sum();

            double denominator = trades.stream()
                    .filter(t -> "BUY".equals(t.getSide()))
                    .mapToDouble(t -> t.getQty().doubleValue() * t.getPrice().doubleValue())
                    .sum();

            double returnPct = denominator > 0 ? realizedPnl / denominator * 100.0 : 0.0;

            int tradeCount = (int) trades.stream()
                    .filter(t -> "BUY".equals(t.getSide()))
                    .count();

            List<String> universe = parseUniverse(run.getUniverseSnapshot());

            double finalEquity = "RUNNING".equals(run.getStatus())
                    ? currentEquity
                    : resolveStoppedEquity(accountId, run.getEndedAt());

            List<Long> orderedIds = ruleRunOrder.getOrDefault(run.getRuleId(), List.of());
            int runIndex = orderedIds.indexOf(run.getId()) + 1;

            result.add(new RunListItem(
                    run.getId(), run.getRuleId(), ruleName, run.getStatus(),
                    run.getStartedAt(), run.getEndedAt(), universe,
                    realizedPnl, returnPct, tradeCount, finalEquity, runIndex));
        }

        return result;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /** 계좌 현재 자산 = 최신 equity snapshot. 없으면 cash fallback. */
    private double resolveCurrentEquity(Long accountId, Optional<PaperAccount> accountOpt) {
        if (accountId != null) {
            List<PaperEquitySnapshot> snapshots = snapshotRepo.findByAccountIdOrderByTsDesc(accountId);
            if (!snapshots.isEmpty()) {
                return snapshots.get(0).getEquity().doubleValue();
            }
        }
        return accountOpt.map(a -> a.getCash().doubleValue()).orElse(0.0);
    }

    /** STOPPED run: ended_at 이전 마지막 스냅샷 equity. */
    private double resolveStoppedEquity(Long accountId, Instant endedAt) {
        if (accountId == null || endedAt == null) return 0.0;
        return snapshotRepo.findByAccountIdOrderByTsDesc(accountId).stream()
                .filter(s -> !s.getTs().isAfter(endedAt))
                .findFirst()
                .map(s -> s.getEquity().doubleValue())
                .orElse(0.0);
    }

    /** rule_id별 run id 목록 (startedAt ASC) — 회차 계산용. */
    private Map<Long, List<Long>> computeRunOrder(List<PaperRun> runs) {
        Map<Long, List<Long>> result = new HashMap<>();
        runs.stream()
                .collect(Collectors.groupingBy(PaperRun::getRuleId))
                .forEach((ruleId, group) -> {
                    List<Long> ordered = group.stream()
                            .sorted(Comparator.comparing(PaperRun::getStartedAt))
                            .map(PaperRun::getId)
                            .collect(Collectors.toList());
                    result.put(ruleId, ordered);
                });
        return result;
    }

    /** universe_snapshot JSON → List<String>. 파싱 실패 시 빈 리스트 반환. */
    private List<String> parseUniverse(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse universe_snapshot: {}", e.getMessage());
            return List.of();
        }
    }
}
