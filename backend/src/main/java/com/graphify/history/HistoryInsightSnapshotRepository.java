package com.graphify.history;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryInsightSnapshotRepository extends JpaRepository<HistoryInsightSnapshot, Long> {
    List<HistoryInsightSnapshot> findBySessionIdOrderBySortOrderAsc(UUID sessionId);
}
