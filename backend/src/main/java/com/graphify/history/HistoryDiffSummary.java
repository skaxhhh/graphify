package com.graphify.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "history_diff_summaries")
public class HistoryDiffSummary {

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "summary_text", nullable = false)
    private String summaryText;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public String getSummaryText() {
        return summaryText;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
