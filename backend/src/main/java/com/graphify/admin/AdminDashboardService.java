package com.graphify.admin;

import com.graphify.admin.dto.AdminAgentStatsDto;
import com.graphify.admin.dto.AdminAlertDto;
import com.graphify.admin.dto.StatsPointDto;
import com.graphify.admin.dto.UserUsageDataDto;
import com.graphify.admin.dto.UserUsageRowDto;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.history.AnalysisHistoryRepository;
import com.graphify.user.User;
import com.graphify.user.UserRepository;
import com.graphify.user.UserRole;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminMetricsDailyRepository metricsRepository;
    private final AdminAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final AnalysisHistoryRepository analysisHistoryRepository;

    public AdminDashboardService(
            AdminMetricsDailyRepository metricsRepository,
            AdminAlertRepository alertRepository,
            UserRepository userRepository,
            AnalysisHistoryRepository analysisHistoryRepository
    ) {
        this.metricsRepository = metricsRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.analysisHistoryRepository = analysisHistoryRepository;
    }

    public ApiResponse<AdminAgentStatsDto> getAgentStats(String periodParam) {
        String period = normalizePeriod(periodParam);
        int days = "week".equals(period) ? 56 : 14;
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);

        List<AdminMetricsDaily> rows = metricsRepository
                .findByMetricDateBetweenOrderByMetricDateAsc(from, to);

        List<StatsPointDto> series = buildSeries(rows, period);
        long totalRuns = series.stream().mapToLong(StatsPointDto::runCount).sum();
        long totalTokens = series.stream().mapToLong(StatsPointDto::tokenUsage).sum();
        long totalErrors = series.stream().mapToLong(StatsPointDto::errorCount).sum();
        long avgDuration = rows.isEmpty()
                ? 0
                : Math.round(rows.stream().mapToLong(AdminMetricsDaily::getAvgDurationMs).average().orElse(0));
        double errorRate = totalRuns == 0 ? 0 : (double) totalErrors / totalRuns;

        List<AdminAlertDto> alerts = alertRepository.findTop10ByOrderByDetectedAtDesc()
                .stream()
                .map(a -> new AdminAlertDto(a.getSeverity(), a.getMessage(), a.getDetectedAt()))
                .toList();

        return ApiResponse.ok(new AdminAgentStatsDto(
                totalRuns,
                avgDuration,
                totalTokens,
                Math.round(errorRate * 1000) / 1000.0,
                period,
                series,
                alerts
        ));
    }

    public ApiResponse<UserUsageDataDto> getUserUsage() {
        List<UserUsageRowDto> usageRows = userRepository.findAll().stream()
                .filter(u -> u.getRole() != UserRole.ADMIN)
                .map(this::toUsageRow)
                .filter(row -> row.requests() > 0)
                .sorted(Comparator.comparingLong(UserUsageRowDto::tokens).reversed())
                .toList();

        return ApiResponse.ok(new UserUsageDataDto(usageRows));
    }

    private UserUsageRowDto toUsageRow(User user) {
        long requests = analysisHistoryRepository.countByUserId(user.getId());
        long errors = analysisHistoryRepository.countByUserIdAndStatus(user.getId(), "FAILED");
        long tokens = requests * 48_000L + user.getId() * 1_000L;
        return new UserUsageRowDto(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                requests,
                tokens,
                errors
        );
    }

    private List<StatsPointDto> buildSeries(List<AdminMetricsDaily> rows, String period) {
        if ("week".equals(period)) {
            return aggregateByWeek(rows);
        }
        return rows.stream()
                .map(r -> new StatsPointDto(
                        r.getMetricDate(),
                        r.getRunCount(),
                        r.getTokenUsage(),
                        r.getErrorCount()
                ))
                .toList();
    }

    private List<StatsPointDto> aggregateByWeek(List<AdminMetricsDaily> rows) {
        List<StatsPointDto> weekly = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += 7) {
            int end = Math.min(i + 7, rows.size());
            List<AdminMetricsDaily> chunk = rows.subList(i, end);
            LocalDate date = chunk.getFirst().getMetricDate();
            int runs = chunk.stream().mapToInt(AdminMetricsDaily::getRunCount).sum();
            long tokens = chunk.stream().mapToLong(AdminMetricsDaily::getTokenUsage).sum();
            int errors = chunk.stream().mapToInt(AdminMetricsDaily::getErrorCount).sum();
            weekly.add(new StatsPointDto(date, runs, tokens, errors));
        }
        return weekly;
    }

    private static String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "day";
        }
        String normalized = period.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("day") && !normalized.equals("week")) {
            throw new GraphifyException(
                    "ERR_ADMIN_002",
                    "period는 day 또는 week 이어야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }
}
