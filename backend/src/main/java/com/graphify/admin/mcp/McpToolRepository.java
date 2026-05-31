package com.graphify.admin.mcp;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface McpToolRepository extends JpaRepository<McpTool, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT t FROM McpTool t
            WHERE (:q IS NULL OR :q = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR t.connectionStatus = :status)
            ORDER BY t.name ASC
            """)
    List<McpTool> search(
            @Param("q") String q,
            @Param("status") String status
    );
}
