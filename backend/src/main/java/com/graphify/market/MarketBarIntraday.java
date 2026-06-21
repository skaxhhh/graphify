package com.graphify.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "market_bars_intraday")
public class MarketBarIntraday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private Instant ts;

    @Column(name = "\"interval\"", nullable = false)
    private String interval = "5m";

    @Column(columnDefinition = "numeric(20,4)")
    private Double open;
    @Column(columnDefinition = "numeric(20,4)")
    private Double high;
    @Column(columnDefinition = "numeric(20,4)")
    private Double low;

    @Column(nullable = false, columnDefinition = "numeric(20,4)")
    private Double close;

    private Long volume;

    @Column(nullable = false)
    private String source = "YAHOO";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MarketBarIntraday() {
    }

    public MarketBarIntraday(String symbol, Instant ts, String interval, Double open, Double high,
                             Double low, double close, Long volume, String source) {
        this.symbol = symbol;
        this.ts = ts;
        this.interval = interval;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.source = source;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public Instant getTs() {
        return ts;
    }

    public Double getClose() {
        return close;
    }

    public Long getVolume() {
        return volume;
    }

    public void update(Double open, Double high, Double low, double close, Long volume) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
}
