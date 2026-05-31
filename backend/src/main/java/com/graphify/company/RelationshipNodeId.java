package com.graphify.company;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RelationshipNodeId implements Serializable {

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "node_id")
    private String nodeId;

    protected RelationshipNodeId() {
    }

    public RelationshipNodeId(Long companyId, String nodeId) {
        this.companyId = companyId;
        this.nodeId = nodeId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RelationshipNodeId that)) {
            return false;
        }
        return Objects.equals(companyId, that.companyId) && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, nodeId);
    }
}
