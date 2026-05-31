package com.graphify.history;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {
    List<TimelineEvent> findBySessionIdOrderBySortOrderAsc(UUID sessionId);
}
