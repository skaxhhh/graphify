package com.graphify.home;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketNewsRepository extends JpaRepository<MarketNews, Long> {

    List<MarketNews> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Optional<MarketNews> findBySourceUrl(String sourceUrl);

    long count();

    @Query("""
            SELECT m FROM MarketNews m
            WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(m.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY m.publishedAt DESC
            """)
    List<MarketNews> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
