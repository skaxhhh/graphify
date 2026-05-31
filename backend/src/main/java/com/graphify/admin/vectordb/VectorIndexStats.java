package com.graphify.admin.vectordb;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vector_index_stats")
public class VectorIndexStats {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "total_vectors", nullable = false)
    private long totalVectors;

    @Column(name = "index_size_bytes", nullable = false)
    private long indexSizeBytes;

    @Column(name = "vectors_by_type", nullable = false, columnDefinition = "jsonb")
    private String vectorsByType = "{}";

    @Column(name = "avg_latency_ms", nullable = false, precision = 10, scale = 2)
    private BigDecimal avgLatencyMs = BigDecimal.ZERO;

    @Column(name = "avg_similarity", nullable = false, precision = 6, scale = 4)
    private BigDecimal avgSimilarity = BigDecimal.ZERO;

    @Column(name = "request_count_24h", nullable = false)
    private long requestCount24h;

    @Column(name = "latency_series", nullable = false, columnDefinition = "jsonb")
    private String latencySeries = "[]";

    @Column(name = "similarity_series", nullable = false, columnDefinition = "jsonb")
    private String similaritySeries = "[]";

    @Column(name = "request_series", nullable = false, columnDefinition = "jsonb")
    private String requestSeries = "[]";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public long getTotalVectors() {
        return totalVectors;
    }

    public void setTotalVectors(long totalVectors) {
        this.totalVectors = totalVectors;
    }

    public long getIndexSizeBytes() {
        return indexSizeBytes;
    }

    public void setIndexSizeBytes(long indexSizeBytes) {
        this.indexSizeBytes = indexSizeBytes;
    }

    public String getVectorsByType() {
        return vectorsByType;
    }

    public void setVectorsByType(String vectorsByType) {
        this.vectorsByType = vectorsByType;
    }

    public BigDecimal getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(BigDecimal avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public BigDecimal getAvgSimilarity() {
        return avgSimilarity;
    }

    public void setAvgSimilarity(BigDecimal avgSimilarity) {
        this.avgSimilarity = avgSimilarity;
    }

    public long getRequestCount24h() {
        return requestCount24h;
    }

    public void setRequestCount24h(long requestCount24h) {
        this.requestCount24h = requestCount24h;
    }

    public String getLatencySeries() {
        return latencySeries;
    }

    public void setLatencySeries(String latencySeries) {
        this.latencySeries = latencySeries;
    }

    public String getSimilaritySeries() {
        return similaritySeries;
    }

    public void setSimilaritySeries(String similaritySeries) {
        this.similaritySeries = similaritySeries;
    }

    public String getRequestSeries() {
        return requestSeries;
    }

    public void setRequestSeries(String requestSeries) {
        this.requestSeries = requestSeries;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
