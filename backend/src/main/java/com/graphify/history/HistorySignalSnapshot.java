package com.graphify.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "history_signal_snapshots")
public class HistorySignalSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private java.util.UUID sessionId;

    @Column(nullable = false)
    private String label;

    @Column(name = "signal_kind", nullable = false)
    private String signalKind;

    @Column(name = "related_node_ids")
    private String relatedNodeIds;

    private String sources;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public String getLabel() {
        return label;
    }

    public String getSignalKind() {
        return signalKind;
    }

    public String getRelatedNodeIds() {
        return relatedNodeIds;
    }

    public String getSources() {
        return sources;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
