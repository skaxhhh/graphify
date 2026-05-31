package com.graphify.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    Optional<UserAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<UserAuthProvider> findFirstByUser_Id(Long userId);
}
