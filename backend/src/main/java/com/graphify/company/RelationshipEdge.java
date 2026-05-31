package com.graphify.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "relationship_edges")
public class RelationshipEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "source_node_id", nullable = false)
    private String sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private String targetNodeId;

    @Column(name = "relation_type", nullable = false)
    private String relationType;

    @Column(nullable = false)
    private double strength;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public String getRelationType() {
        return relationType;
    }

    public double getStrength() {
        return strength;
    }

    public String getEvidence() {
        return evidence;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
