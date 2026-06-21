package com.graphify.trading.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "paper_live_symbols")
public class PaperLiveSymbol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaperLiveSymbol() {}

    public PaperLiveSymbol(Long ruleId, String symbol) {
        this.ruleId = ruleId;
        this.symbol = symbol;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId()       { return id; }
    public Long getRuleId()   { return ruleId; }
    public String getSymbol() { return symbol; }
}
