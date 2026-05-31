package com.graphify.admin.vectordb;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VectorIndexStatsRepository extends JpaRepository<VectorIndexStats, Long> {
}
