package com.graphify.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "graphify.news")
public class GraphifyNewsProperties {

    private String apiKey = "";
    private int refreshMinutes = 15;
    private List<String> rssFeeds = new ArrayList<>(List.of(
            "https://www.hankyung.com/feed/economy",
            "https://www.mk.co.kr/rss/30000001/",
            "https://www.yna.co.kr/rss/economy.xml"
    ));

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getRefreshMinutes() {
        return refreshMinutes;
    }

    public void setRefreshMinutes(int refreshMinutes) {
        this.refreshMinutes = refreshMinutes;
    }

    public List<String> getRssFeeds() {
        return rssFeeds;
    }

    public void setRssFeeds(List<String> rssFeeds) {
        this.rssFeeds = rssFeeds;
    }

    public boolean hasNewsApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
