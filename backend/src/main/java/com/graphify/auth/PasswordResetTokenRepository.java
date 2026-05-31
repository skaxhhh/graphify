package com.graphify.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    long countByUserIdAndCreatedAtAfter(Long userId, Instant createdAfter);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
