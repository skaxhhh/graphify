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
@Table(name = "paper_trades")
public class PaperTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(nullable = false, length = 4)
    private String side;   // BUY | SELL

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal qty;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal price;

    @Column(precision = 20, scale = 4)
    private BigDecimal pnl;

    @Column(name = "run_id")
    private Long runId;  // nullable — legacy rows and no-active-run fallback

    @Column(name = "traded_at", nullable = false)
    private Instant tradedAt;

    protected PaperTrade() {}

    /** 9-arg constructor: includes runId (nullable). Primary constructor. */
    public PaperTrade(Long accountId, Long ruleId, Long runId, String symbol, String side,
                      BigDecimal qty, BigDecimal price, BigDecimal pnl, Instant tradedAt) {
        this.accountId = accountId;
        this.ruleId = ruleId;
        this.runId = runId;
        this.symbol = symbol;
        this.side = side;
        this.qty = qty;
        this.price = price;
        this.pnl = pnl;
        this.tradedAt = tradedAt;
    }

    /** 8-arg backward-compat constructor: delegates with runId=null. */
    public PaperTrade(Long accountId, Long ruleId, String symbol, String side,
                      BigDecimal qty, BigDecimal price, BigDecimal pnl, Instant tradedAt) {
        this(accountId, ruleId, null, symbol, side, qty, price, pnl, tradedAt);
    }

    public Long getId()          { return id; }
    public Long getAccountId()   { return accountId; }
    public Long getRuleId()      { return ruleId; }
    public Long getRunId()       { return runId; }
    public String getSymbol()    { return symbol; }
    public String getSide()      { return side; }
    public BigDecimal getQty()   { return qty; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getPnl()   { return pnl; }
    public Instant getTradedAt() { return tradedAt; }
}
