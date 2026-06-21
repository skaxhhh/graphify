package com.graphify.toss;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TossCredentialRepository extends JpaRepository<TossCredential, Long> {

    Optional<TossCredential> findByUserId(Long userId);

    /** Find credentials whose token expires within the given window (for pre-emptive refresh) */
    @Query("SELECT c FROM TossCredential c WHERE c.tokenExpiresAt IS NOT NULL AND c.tokenExpiresAt BETWEEN :from AND :to")
    List<TossCredential> findExpiringSoon(Instant from, Instant to);
}
