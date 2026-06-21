package com.graphify.trading.paper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "paper_signal_log")
public class PaperSignalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(nullable = false)
    private Instant ts;

    @Column(nullable = false, length = 8)
    private String signal;   // BUY | SELL | HOLD

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "indicator_snapshot", columnDefinition = "jsonb")
    private String indicatorSnapshot;

    @Column(nullable = false)
    private boolean executed = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaperSignalLog() {}

    public PaperSignalLog(Long ruleId, String symbol, Instant ts, String signal,
                          String indicatorSnapshot, boolean executed) {
        this.ruleId = ruleId;
        this.symbol = symbol;
        this.ts = ts;
        this.signal = signal;
        this.indicatorSnapshot = indicatorSnapshot;
        this.executed = executed;
    }

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public Long getId()                  { return id; }
    public Long getRuleId()              { return ruleId; }
    public String getSymbol()            { return symbol; }
    public Instant getTs()               { return ts; }
    public String getSignal()            { return signal; }
    public String getIndicatorSnapshot() { return indicatorSnapshot; }
    public boolean isExecuted()          { return executed; }
}
