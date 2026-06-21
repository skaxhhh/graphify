package com.graphify.trading.paper;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperAccountRepository extends JpaRepository<PaperAccount, Long> {
    Optional<PaperAccount> findByUserId(Long userId);
}
