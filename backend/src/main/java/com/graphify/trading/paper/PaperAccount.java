package com.graphify.trading.paper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "paper_accounts")
public class PaperAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "base_cash", nullable = false, precision = 20, scale = 4)
    private BigDecimal baseCash;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal cash;

    @Column(name = "eval_interval", nullable = false, length = 8)
    private String evalInterval = "EOD";

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaperAccount() {}

    public PaperAccount(Long userId, BigDecimal initialCash) {
        this.userId = userId;
        this.baseCash = initialCash;
        this.cash = initialCash;
    }

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }

    public Long getId()             { return id; }
    public Long getUserId()         { return userId; }
    public BigDecimal getBaseCash() { return baseCash; }
    public BigDecimal getCash()     { return cash; }
    public String getStatus()       { return status; }
    public void setCash(BigDecimal cash) { this.cash = cash; }
}
