package com.graphify.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "admin_metrics_daily")
public class AdminMetricsDaily {

    @Id
    @Column(name = "metric_date")
    private LocalDate metricDate;

    @Column(name = "run_count", nullable = false)
    private int runCount;

    @Column(name = "avg_duration_ms", nullable = false)
    private long avgDurationMs;

    @Column(name = "token_usage", nullable = false)
    private long tokenUsage;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public int getRunCount() {
        return runCount;
    }

    public long getAvgDurationMs() {
        return avgDurationMs;
    }

    public long getTokenUsage() {
        return tokenUsage;
    }

    public int getErrorCount() {
        return errorCount;
    }
}
