package com.graphify.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "history_insight_snapshots")
public class HistoryInsightSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @Column(name = "session_id", nullable = false)
    private java.util.UUID sessionId;

    @Column(name = "card_type", nullable = false)
    private String cardType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private String confidence;

    private String evidence;

    @Column(name = "highlight_node_ids")
    private String highlightNodeIds;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public String getCardType() {
        return cardType;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getHighlightNodeIds() {
        return highlightNodeIds;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
