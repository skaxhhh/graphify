package com.graphify.home;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyViewStatsRepository extends JpaRepository<CompanyViewStats, Long> {

    @Query("""
            SELECT s FROM CompanyViewStats s
            ORDER BY s.viewCount DESC, s.updatedAt DESC
            """)
    List<CompanyViewStats> findTopByViewCount(org.springframework.data.domain.Pageable pageable);
}
