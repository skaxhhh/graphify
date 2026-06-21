package com.graphify.trading.paper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "paper_equity_snapshots")
public class PaperEquitySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Instant ts;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal equity;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal cash;

    protected PaperEquitySnapshot() {}

    public PaperEquitySnapshot(Long accountId, Instant ts, BigDecimal equity, BigDecimal cash) {
        this.accountId = accountId;
        this.ts = ts;
        this.equity = equity;
        this.cash = cash;
    }

    public Long getId()           { return id; }
    public Long getAccountId()    { return accountId; }
    public Instant getTs()        { return ts; }
    public BigDecimal getEquity() { return equity; }
    public BigDecimal getCash()   { return cash; }
}
