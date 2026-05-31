package com.graphify.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "watchlist_items")
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    protected WatchlistItem() {
    }

    public WatchlistItem(Long userId, Long companyId) {
        this.userId = userId;
        this.companyId = companyId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
