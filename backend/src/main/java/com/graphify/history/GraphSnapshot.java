package com.graphify.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "graph_snapshots")
public class GraphSnapshot {

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "nodes_json", nullable = false, columnDefinition = "jsonb")
    private String nodesJson;

    @Column(name = "edges_json", nullable = false, columnDefinition = "jsonb")
    private String edgesJson;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    public UUID getSessionId() {
        return sessionId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getNodesJson() {
        return nodesJson;
    }

    public String getEdgesJson() {
        return edgesJson;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }
}
