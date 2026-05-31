package com.graphify.history;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraphSnapshotRepository extends JpaRepository<GraphSnapshot, UUID> {
    Optional<GraphSnapshot> findBySessionId(UUID sessionId);
}
