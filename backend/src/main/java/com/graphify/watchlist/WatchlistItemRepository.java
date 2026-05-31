package com.graphify.watchlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<WatchlistItem> findByUserIdAndCompanyId(Long userId, Long companyId);

    void deleteByUserIdAndCompanyId(Long userId, Long companyId);

    long countByUserId(Long userId);
}
