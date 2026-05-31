package com.graphify.history;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryDiffSummaryRepository extends JpaRepository<HistoryDiffSummary, UUID> {
    Optional<HistoryDiffSummary> findBySessionId(UUID sessionId);
}
