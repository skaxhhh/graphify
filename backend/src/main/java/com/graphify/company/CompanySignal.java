package com.graphify.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "company_signals")
public class CompanySignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String label;

    @Column(name = "signal_kind", nullable = false)
    private String signalKind;

    @Column(name = "related_node_ids")
    private String relatedNodeIds;

    private String sources;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

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
}
