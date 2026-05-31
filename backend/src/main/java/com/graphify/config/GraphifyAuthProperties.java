package com.graphify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "graphify.auth")
public class GraphifyAuthProperties {

    private String jwtSecret = "dev-only-change-me-graphify-jwt-secret-key-256bits-minimum!!";
    private long accessExpirationMinutes = 15;
    private long refreshExpirationDays = 7;
    private String frontendBaseUrl = "http://localhost:5173";
    private String apiPublicBaseUrl = "http://localhost:8081";
    private int passwordResetExpirationMinutes = 60;
    private int passwordResetRateLimitMax = 3;
    private int passwordResetRateLimitWindowMinutes = 15;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getAccessExpirationMinutes() {
        return accessExpirationMinutes;
    }

    public void setAccessExpirationMinutes(long accessExpirationMinutes) {
        this.accessExpirationMinutes = accessExpirationMinutes;
    }

    public long getRefreshExpirationDays() {
        return refreshExpirationDays;
    }

    public void setRefreshExpirationDays(long refreshExpirationDays) {
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getApiPublicBaseUrl() {
        return apiPublicBaseUrl;
    }

    public void setApiPublicBaseUrl(String apiPublicBaseUrl) {
        this.apiPublicBaseUrl = apiPublicBaseUrl;
    }

    public int getPasswordResetExpirationMinutes() {
        return passwordResetExpirationMinutes;
    }

    public void setPasswordResetExpirationMinutes(int passwordResetExpirationMinutes) {
        this.passwordResetExpirationMinutes = passwordResetExpirationMinutes;
    }

    public int getPasswordResetRateLimitMax() {
        return passwordResetRateLimitMax;
    }

    public void setPasswordResetRateLimitMax(int passwordResetRateLimitMax) {
        this.passwordResetRateLimitMax = passwordResetRateLimitMax;
    }

    public int getPasswordResetRateLimitWindowMinutes() {
        return passwordResetRateLimitWindowMinutes;
    }

    public void setPasswordResetRateLimitWindowMinutes(int passwordResetRateLimitWindowMinutes) {
        this.passwordResetRateLimitWindowMinutes = passwordResetRateLimitWindowMinutes;
    }
}
