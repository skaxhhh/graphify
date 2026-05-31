package com.graphify.home;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "market_news")
public class MarketNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "source_name", nullable = false, length = 128)
    private String sourceName;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(length = 32)
    private String ticker;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "feed_source", length = 32)
    private String feedSource;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getTicker() {
        return ticker;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getFeedSource() {
        return feedSource;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setFeedSource(String feedSource) {
        this.feedSource = feedSource;
    }
}
