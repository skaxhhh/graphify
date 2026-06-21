package com.graphify.trading.paper;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperPositionRepository extends JpaRepository<PaperPosition, Long> {
    Optional<PaperPosition> findByAccountIdAndSymbol(Long accountId, String symbol);
    List<PaperPosition> findByAccountId(Long accountId);
    void deleteByAccountIdAndSymbol(Long accountId, String symbol);
}
