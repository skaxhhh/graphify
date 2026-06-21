package com.graphify.trading.paper;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperEquitySnapshotRepository extends JpaRepository<PaperEquitySnapshot, Long> {
    List<PaperEquitySnapshot> findByAccountIdOrderByTsDesc(Long accountId);
}
