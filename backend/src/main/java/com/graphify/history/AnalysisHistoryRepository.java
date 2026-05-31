package com.graphify.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisHistoryRepository
        extends JpaRepository<AnalysisHistory, Long>, JpaSpecificationExecutor<AnalysisHistory> {

    Optional<AnalysisHistory> findBySessionIdAndUserId(UUID sessionId, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);
}
