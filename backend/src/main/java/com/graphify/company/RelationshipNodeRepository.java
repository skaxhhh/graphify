package com.graphify.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationshipNodeRepository extends JpaRepository<RelationshipNode, RelationshipNodeId> {

    List<RelationshipNode> findByIdCompanyIdOrderByDepthLevelAsc(Long companyId);

    List<RelationshipNode> findByIdCompanyIdAndDepthLevelLessThanEqualOrderByDepthLevelAsc(
            Long companyId,
            int depthLevel
    );
}
