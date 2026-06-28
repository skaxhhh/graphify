package com.graphify.trading.paper;

import com.graphify.trading.paper.dto.ReportDto;
import com.graphify.trading.paper.dto.ReportDto.EquityPoint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaperReportService {

    private static final int MIN_RATIO_POINTS = 5;
    private static final double ANNUALIZE = Math.sqrt(252.0);

    private final PaperAccountRepository accountRepo;
    private final PaperEquitySnapshotRepository snapshotRepo;
    private final PaperTradeRepository tradeRepo;

    public PaperReportService(
            PaperAccountRepository accountRepo,
            PaperEquitySnapshotRepository snapshotRepo,
            PaperTradeRepository tradeRepo) {
        this.accountRepo = accountRepo;
        this.snapshotRepo = snapshotRepo;
        this.tradeRepo = tradeRepo;
    }

    public ReportDto getReport(Long userId) {
        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        if (accountOpt.isEmpty()) {
            return ReportDto.empty();
        }
        Long accountId = accountOpt.get().getId();

        // Equity curve: snapshots in ascending ts order, last 30 days
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        List<PaperEquitySnapshot> rawSnapshots =
                snapshotRepo.findByAccountIdOrderByTsDesc(accountId);

        // Reverse to ascending, filter last 30 days
        List<PaperEquitySnapshot> snapshots = new ArrayList<>();
        for (int i = rawSnapshots.size() - 1; i >= 0; i--) {
            PaperEquitySnapshot s = rawSnapshots.get(i);
            if (!s.getTs().isBefore(cutoff)) {
                snapshots.add(s);
            }
        }

        if (snapshots.isEmpty()) {
            return buildTradeStatsOnly(accountId);
        }

        // Build equity curve
        List<EquityPoint> equityCurve = snapshots.stream()
                .map(s -> new EquityPoint(s.getTs().toString(), s.getEquity().doubleValue()))
                .toList();

        double[] equities = snapshots.stream()
                .mapToDouble(s -> s.getEquity().doubleValue())
                .toArray();

        double firstEquity = equities[0];
        double lastEquity = equities[equities.length - 1];
        double totalReturn = firstEquity > 0 ? (lastEquity - firstEquity) / firstEquity * 100.0 : 0.0;
        double maxDrawdownPct = computeMdd(equities);

        // Daily returns for Sharpe/Sortino
        double[] dailyReturns = computeDailyReturns(equities);
        double sharpeRatio = dailyReturns.length >= MIN_RATIO_POINTS ? computeSharpe(dailyReturns) : 0.0;
        double sortinoRatio = dailyReturns.length >= MIN_RATIO_POINTS ? computeSortino(dailyReturns) : 0.0;

        // Trade stats from SELL trades
        int[] tradeStats = computeTradeStats(accountId);
        int totalTrades = tradeStats[0];
        int winTrades = tradeStats[1];
        double winRate = totalTrades > 0 ? (double) winTrades / totalTrades * 100.0 : 0.0;

        Instant periodFrom = snapshots.get(0).getTs();
        Instant periodTo = snapshots.get(snapshots.size() - 1).getTs();

        return new ReportDto(equityCurve, totalReturn, maxDrawdownPct, winRate,
                totalTrades, winTrades, sharpeRatio, sortinoRatio, periodFrom, periodTo);
    }

    // Build report with only trade stats (no snapshots yet)
    private ReportDto buildTradeStatsOnly(Long accountId) {
        int[] tradeStats = computeTradeStats(accountId);
        int totalTrades = tradeStats[0];
        int winTrades = tradeStats[1];
        double winRate = totalTrades > 0 ? (double) winTrades / totalTrades * 100.0 : 0.0;
        return new ReportDto(List.of(), 0.0, 0.0, winRate, totalTrades, winTrades, 0.0, 0.0, null, null);
    }

    /** Max drawdown as percentage from peak */
    private double computeMdd(double[] equities) {
        double peak = equities[0];
        double maxDd = 0.0;
        for (double eq : equities) {
            if (eq > peak) peak = eq;
            double dd = peak > 0 ? (peak - eq) / peak * 100.0 : 0.0;
            if (dd > maxDd) maxDd = dd;
        }
        return maxDd;
    }

    /** Simple daily returns: (eq[i] - eq[i-1]) / eq[i-1] */
    private double[] computeDailyReturns(double[] equities) {
        if (equities.length < 2) return new double[0];
        double[] returns = new double[equities.length - 1];
        for (int i = 1; i < equities.length; i++) {
            returns[i - 1] = equities[i - 1] > 0
                    ? (equities[i] - equities[i - 1]) / equities[i - 1]
                    : 0.0;
        }
        return returns;
    }

    private double computeSharpe(double[] returns) {
        double mean = mean(returns);
        double std = std(returns);
        return std > 0 ? mean / std * ANNUALIZE : 0.0;
    }

    private double computeSortino(double[] returns) {
        double mean = mean(returns);
        // Downside std: only negative returns
        double sumSq = 0.0;
        int count = 0;
        for (double r : returns) {
            if (r < 0) { sumSq += r * r; count++; }
        }
        double downsideStd = count > 0 ? Math.sqrt(sumSq / count) : 0.0;
        return downsideStd > 0 ? mean / downsideStd * ANNUALIZE : 0.0;
    }

    private double mean(double[] arr) {
        if (arr.length == 0) return 0.0;
        double sum = 0.0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private double std(double[] arr) {
        if (arr.length < 2) return 0.0;
        double m = mean(arr);
        double sumSq = 0.0;
        for (double v : arr) sumSq += (v - m) * (v - m);
        return Math.sqrt(sumSq / arr.length);
    }

    /**
     * run 기간 내 리포트 (D7 mode 1 — "이 실행만").
     * equity curve = run.startedAt ~ run.endedAt (진행중이면 now) 내 account 전체 자산 흐름.
     * Open Q3 / Pitfall 2: "기간 내 계좌 전체 자산 흐름"으로 명시.
     *
     * @param userId    사용자 id
     * @param startedAt run 시작 시각
     * @param endedAt   run 종료 시각 (null = 진행중 → Instant.now() 사용)
     */
    public ReportDto getReportByRun(Long userId, Instant startedAt, Instant endedAt) {
        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        if (accountOpt.isEmpty()) return ReportDto.empty();
        Long accountId = accountOpt.get().getId();
        Instant to = endedAt != null ? endedAt : Instant.now();
        return buildReportForPeriod(accountId, startedAt, to);
    }

    /**
     * 지정 기간 내 리포트 (D7 mode 2 — RULE_AGGREGATE).
     * equity curve = [from, to] 내 account 전체 자산 흐름.
     *
     * @param userId 사용자 id
     * @param from   기간 시작 (inclusive)
     * @param to     기간 종료 (inclusive)
     */
    public ReportDto getReportByPeriod(Long userId, Instant from, Instant to) {
        Optional<PaperAccount> accountOpt = accountRepo.findByUserId(userId);
        if (accountOpt.isEmpty()) return ReportDto.empty();
        Long accountId = accountOpt.get().getId();
        return buildReportForPeriod(accountId, from, to);
    }

    /** Returns [totalSellTrades, winTrades] */
    private int[] computeTradeStats(Long accountId) {
        List<PaperTrade> trades = tradeRepo.findByAccountIdOrderByTradedAtDesc(accountId);
        int total = 0;
        int win = 0;
        for (PaperTrade t : trades) {
            if ("SELL".equals(t.getSide())) {
                total++;
                if (t.getPnl() != null && t.getPnl().doubleValue() > 0) win++;
            }
        }
        return new int[]{total, win};
    }

    /** Returns [totalSellTrades, winTrades] filtered to [from, to] period. */
    private int[] computeTradeStatsForPeriod(Long accountId, Instant from, Instant to) {
        List<PaperTrade> trades = tradeRepo.findByAccountIdOrderByTradedAtDesc(accountId);
        int total = 0;
        int win = 0;
        for (PaperTrade t : trades) {
            if ("SELL".equals(t.getSide())
                    && !t.getTradedAt().isBefore(from)
                    && !t.getTradedAt().isAfter(to)) {
                total++;
                if (t.getPnl() != null && t.getPnl().doubleValue() > 0) win++;
            }
        }
        return new int[]{total, win};
    }

    /**
     * 기간 [from, to] 내 스냅샷으로 리포트 생성.
     * equity curve = account 전체 자산 흐름 (단일 계좌 공유 — D5).
     * trade stats = 기간 내 SELL trades 기반.
     */
    private ReportDto buildReportForPeriod(Long accountId, Instant from, Instant to) {
        List<PaperEquitySnapshot> rawSnapshots = snapshotRepo.findByAccountIdOrderByTsDesc(accountId);

        // Filter to [from, to] and reverse to ascending
        List<PaperEquitySnapshot> snapshots = new ArrayList<>();
        for (int i = rawSnapshots.size() - 1; i >= 0; i--) {
            PaperEquitySnapshot s = rawSnapshots.get(i);
            if (!s.getTs().isBefore(from) && !s.getTs().isAfter(to)) {
                snapshots.add(s);
            }
        }

        int[] tradeStats = computeTradeStatsForPeriod(accountId, from, to);
        int totalTrades = tradeStats[0];
        int winTrades = tradeStats[1];
        double winRate = totalTrades > 0 ? (double) winTrades / totalTrades * 100.0 : 0.0;

        if (snapshots.isEmpty()) {
            return new ReportDto(List.of(), 0.0, 0.0, winRate, totalTrades, winTrades, 0.0, 0.0, null, null);
        }

        List<EquityPoint> equityCurve = snapshots.stream()
                .map(s -> new EquityPoint(s.getTs().toString(), s.getEquity().doubleValue()))
                .toList();

        double[] equities = snapshots.stream()
                .mapToDouble(s -> s.getEquity().doubleValue())
                .toArray();

        double firstEquity = equities[0];
        double lastEquity = equities[equities.length - 1];
        double totalReturn = firstEquity > 0 ? (lastEquity - firstEquity) / firstEquity * 100.0 : 0.0;
        double maxDrawdownPct = computeMdd(equities);

        double[] dailyReturns = computeDailyReturns(equities);
        double sharpeRatio = dailyReturns.length >= MIN_RATIO_POINTS ? computeSharpe(dailyReturns) : 0.0;
        double sortinoRatio = dailyReturns.length >= MIN_RATIO_POINTS ? computeSortino(dailyReturns) : 0.0;

        Instant periodFrom = snapshots.get(0).getTs();
        Instant periodTo = snapshots.get(snapshots.size() - 1).getTs();

        return new ReportDto(equityCurve, totalReturn, maxDrawdownPct, winRate,
                totalTrades, winTrades, sharpeRatio, sortinoRatio, periodFrom, periodTo);
    }
}
