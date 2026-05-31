package com.graphify.company;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "relationship_nodes")
public class RelationshipNode {

    @EmbeddedId
    private RelationshipNodeId id;

    @Column(nullable = false)
    private String label;

    @Column(name = "node_type", nullable = false)
    private String nodeType = "COMPANY";

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private int degree;

    @Column(name = "cluster_id")
    private String clusterId;

    @Column(name = "depth_level", nullable = false)
    private int depthLevel;

    public RelationshipNodeId getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getSummary() {
        return summary;
    }

    public int getDegree() {
        return degree;
    }

    public String getClusterId() {
        return clusterId;
    }

    public int getDepthLevel() {
        return depthLevel;
    }
}
