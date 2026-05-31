package com.graphify.admin.openai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "openai_settings")
public class OpenAiSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "endpoint_url", nullable = false, length = 512)
    private String endpointUrl = "";

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "deployment_name", nullable = false, length = 128)
    private String deploymentName = "gpt-4o";

    @Column(name = "api_version", nullable = false, length = 32)
    private String apiVersion = "2024-02-15";

    @Column(nullable = false, length = 64)
    private String model = "gpt-4o";

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal temperature = new BigDecimal("0.30");

    @Column(name = "max_tokens", nullable = false)
    private int maxTokens = 4096;

    @Column(name = "top_p", nullable = false, precision = 4, scale = 2)
    private BigDecimal topP = new BigDecimal("1.00");

    @Column(name = "embedding_model", nullable = false, length = 64)
    private String embeddingModel = "text-embedding-3-large";

    @Column(name = "embedding_deployment", nullable = false, length = 128)
    private String embeddingDeployment = "text-embedding-3-large";

    @Column(name = "fallback_endpoint_url", length = 512)
    private String fallbackEndpointUrl;

    @Column(name = "fallback_api_key_encrypted", columnDefinition = "TEXT")
    private String fallbackApiKeyEncrypted;

    @Column(name = "fallback_deployment_name", length = 128)
    private String fallbackDeploymentName;

    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed;

    @Column(name = "rate_limit_remaining")
    private Integer rateLimitRemaining;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public BigDecimal getTopP() {
        return topP;
    }

    public void setTopP(BigDecimal topP) {
        this.topP = topP;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getEmbeddingDeployment() {
        return embeddingDeployment;
    }

    public void setEmbeddingDeployment(String embeddingDeployment) {
        this.embeddingDeployment = embeddingDeployment;
    }

    public String getFallbackEndpointUrl() {
        return fallbackEndpointUrl;
    }

    public void setFallbackEndpointUrl(String fallbackEndpointUrl) {
        this.fallbackEndpointUrl = fallbackEndpointUrl;
    }

    public String getFallbackApiKeyEncrypted() {
        return fallbackApiKeyEncrypted;
    }

    public void setFallbackApiKeyEncrypted(String fallbackApiKeyEncrypted) {
        this.fallbackApiKeyEncrypted = fallbackApiKeyEncrypted;
    }

    public String getFallbackDeploymentName() {
        return fallbackDeploymentName;
    }

    public void setFallbackDeploymentName(String fallbackDeploymentName) {
        this.fallbackDeploymentName = fallbackDeploymentName;
    }

    public long getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(long tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public Integer getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    public void setRateLimitRemaining(Integer rateLimitRemaining) {
        this.rateLimitRemaining = rateLimitRemaining;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
