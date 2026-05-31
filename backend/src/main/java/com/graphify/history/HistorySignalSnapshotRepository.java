package com.graphify.history;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorySignalSnapshotRepository extends JpaRepository<HistorySignalSnapshot, Long> {
    List<HistorySignalSnapshot> findBySessionIdOrderBySortOrderAsc(UUID sessionId);
}
