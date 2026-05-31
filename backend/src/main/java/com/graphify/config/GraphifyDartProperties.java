package com.graphify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "graphify.dart")
public class GraphifyDartProperties {

    private String apiKey = "";
    private int corpCodeCacheHours = 24;
    private int searchMaxResults = 20;
    private int enrichThreshold = 3;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getCorpCodeCacheHours() {
        return corpCodeCacheHours;
    }

    public void setCorpCodeCacheHours(int corpCodeCacheHours) {
        this.corpCodeCacheHours = corpCodeCacheHours;
    }

    public int getSearchMaxResults() {
        return searchMaxResults;
    }

    public void setSearchMaxResults(int searchMaxResults) {
        this.searchMaxResults = searchMaxResults;
    }

    public int getEnrichThreshold() {
        return enrichThreshold;
    }

    public void setEnrichThreshold(int enrichThreshold) {
        this.enrichThreshold = enrichThreshold;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
