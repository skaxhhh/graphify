package com.graphify.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationshipEdgeRepository extends JpaRepository<RelationshipEdge, Long> {

    List<RelationshipEdge> findByCompanyId(Long companyId);
}
