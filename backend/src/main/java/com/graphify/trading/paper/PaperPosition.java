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
@Table(name = "paper_positions")
public class PaperPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal qty;

    @Column(name = "avg_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal avgPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaperPosition() {}

    public PaperPosition(Long accountId, String symbol, BigDecimal qty, BigDecimal avgPrice) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.qty = qty;
        this.avgPrice = avgPrice;
        this.updatedAt = Instant.now();
    }

    @PrePersist @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId()             { return id; }
    public Long getAccountId()      { return accountId; }
    public String getSymbol()       { return symbol; }
    public BigDecimal getQty()      { return qty; }
    public BigDecimal getAvgPrice() { return avgPrice; }
}
