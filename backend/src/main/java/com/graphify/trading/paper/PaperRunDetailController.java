package com.graphify.trading.paper;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.history.HistoryService;
import com.graphify.trading.paper.dto.PaperTradeHistoryItem;
import com.graphify.trading.paper.dto.ReportDto;
import com.graphify.trading.paper.dto.RunDashboardDto;
import com.graphify.trading.rule.TradingRuleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/trading/paper/runs/{runId} — 실행 상세 3탭.
 *
 *  /dashboard — run 기여분 + account 전체 equity (RunDashboardDto)
 *  /history   — run-scoped 또는 RULE_AGGREGATE 거래 이력 (mode 분기)
 *  /report    — run-scoped 또는 RULE_AGGREGATE 성과 리포트 (mode 분기)
 *
 * runId 미존재 → ERR_PAPER_RUN_001 / 404.
 */
@RestController
@RequestMapping("/api/v1/trading/paper/runs/{runId}")
public class PaperRunDetailController {

    private final PaperRunRepository           runRepo;
    private final TradingRuleRepository        ruleRepo;
    private final PaperRunContributionService  contributionService;
    private final PaperHistoryService          historyService;
    private final PaperReportService           reportService;

    public PaperRunDetailController(
            PaperRunRepository runRepo,
            TradingRuleRepository ruleRepo,
            PaperRunContributionService contributionService,
            PaperHistoryService historyService,
            PaperReportService reportService) {
        this.runRepo            = runRepo;
        this.ruleRepo           = ruleRepo;
        this.contributionService = contributionService;
        this.historyService     = historyService;
        this.reportService      = reportService;
    }

    /**
     * 대시보드 탭: run 기여분(실현·미실현·거래수·포지션) + account 전체 totalEquity/availableCash.
     */
    @GetMapping("/dashboard")
    public ApiResponse<RunDashboardDto> getDashboard(@PathVariable Long runId) {
        Long userId = HistoryService.requireCurrentUserId();
        PaperRun run = findRunOrThrow(runId);

        var contribution = contributionService.dashboardContribution(userId, runId);

        String ruleName = ruleRepo.findById(run.getRuleId())
                .map(r -> r.getName())
                .orElse("(삭제된 전략)");
        int runIndex = computeRunIndex(run);

        RunDashboardDto dto = new RunDashboardDto(
                runId,
                runIndex,
                ruleName,
                run.getStatus(),
                contribution.totalEquity(),
                contribution.availableCash(),
                contribution.realizedPnl(),
                contribution.unrealizedPnl(),
                contribution.tradeCount(),
                contribution.positions());

        return ApiResponse.ok(dto);
    }

    /**
     * 거래 이력 탭.
     * mode 없음(또는 기타): run_id-scoped (D7 mode 1 — "이 실행만").
     * mode=RULE_AGGREGATE&from=YYYY-MM-DD&to=YYYY-MM-DD: rule+기간 통합 (D7 mode 2).
     */
    @GetMapping("/history")
    public ApiResponse<List<PaperTradeHistoryItem>> getHistory(
            @PathVariable Long runId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Long userId = HistoryService.requireCurrentUserId();
        PaperRun run = findRunOrThrow(runId);

        List<PaperTradeHistoryItem> history;
        if ("RULE_AGGREGATE".equals(mode) && from != null && to != null) {
            Instant fromInstant = parseStartOfDay(from);
            Instant toInstant   = parseEndOfDay(to);
            history = historyService.getHistoryByRuleAndPeriod(userId, run.getRuleId(), fromInstant, toInstant);
        } else {
            history = historyService.getHistoryByRun(userId, runId);
        }
        return ApiResponse.ok(history);
    }

    /**
     * 성과 리포트 탭.
     * mode 없음: run 기간 [startedAt, endedAt|now] 내 account equity curve.
     * mode=RULE_AGGREGATE: 선택 기간 내 account equity curve.
     */
    @GetMapping("/report")
    public ApiResponse<ReportDto> getReport(
            @PathVariable Long runId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Long userId = HistoryService.requireCurrentUserId();
        PaperRun run = findRunOrThrow(runId);

        ReportDto report;
        if ("RULE_AGGREGATE".equals(mode) && from != null && to != null) {
            Instant fromInstant = parseStartOfDay(from);
            Instant toInstant   = parseEndOfDay(to);
            report = reportService.getReportByPeriod(userId, fromInstant, toInstant);
        } else {
            report = reportService.getReportByRun(userId, run.getStartedAt(), run.getEndedAt());
        }
        return ApiResponse.ok(report);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private PaperRun findRunOrThrow(Long runId) {
        return runRepo.findById(runId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_PAPER_RUN_001", "Run not found: " + runId, HttpStatus.NOT_FOUND));
    }

    /**
     * run 회차 번호: 동일 rule의 run들을 startedAt ASC로 정렬 시 몇 번째(1-based).
     */
    private int computeRunIndex(PaperRun run) {
        List<Long> orderedIds = runRepo.findByRuleIdOrderByStartedAtDesc(run.getRuleId())
                .stream()
                .sorted(Comparator.comparing(PaperRun::getStartedAt))
                .map(PaperRun::getId)
                .toList();
        int idx = orderedIds.indexOf(run.getId());
        return idx >= 0 ? idx + 1 : 1;
    }

    /** "YYYY-MM-DD" → 당일 00:00:00 UTC Instant */
    private static Instant parseStartOfDay(String dateStr) {
        return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** "YYYY-MM-DD" → 당일 23:59:59 UTC Instant */
    private static Instant parseEndOfDay(String dateStr) {
        return LocalDate.parse(dateStr).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }
}
