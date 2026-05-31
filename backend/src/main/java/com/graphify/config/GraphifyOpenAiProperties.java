package com.graphify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "graphify.openai")
public class GraphifyOpenAiProperties {

    private String baseUrl = "";
    private String apiKey = "";
    private String apiVersion = "2024-12-01-preview";
    private String model = "gpt-4o";
    /** Azure 배포명 (미설정 시 model 사용) */
    private String deployment = "";
    /** api-key (Azure) | bearer */
    private String authMode = "api-key";
    private int maxTokens = 4096;
    private String fallbackDeployment = "gpt-4o";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getFallbackDeployment() {
        return fallbackDeployment;
    }

    public void setFallbackDeployment(String fallbackDeployment) {
        this.fallbackDeployment = fallbackDeployment;
    }

    public String resolveDeployment() {
        if (deployment != null && !deployment.isBlank()) {
            return deployment.trim();
        }
        return model != null ? model.trim() : "";
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && resolveDeployment() != null
                && !resolveDeployment().isBlank();
    }

    public boolean usesBearerAuth() {
        return authMode != null && authMode.trim().equalsIgnoreCase("bearer");
    }
}
