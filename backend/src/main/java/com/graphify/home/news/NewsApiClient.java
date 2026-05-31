package com.graphify.home.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.config.GraphifyNewsProperties;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NewsApiClient {

    private static final Logger log = LoggerFactory.getLogger(NewsApiClient.class);
    private static final String NEWS_API_URL =
            "https://newsapi.org/v2/top-headlines?country=kr&category=business&pageSize=30";

    private final GraphifyNewsProperties newsProperties;
    private final RestClient restClient;

    public NewsApiClient(GraphifyNewsProperties newsProperties, RestClient newsRestClient) {
        this.newsProperties = newsProperties;
        this.restClient = newsRestClient;
    }

    public List<ExternalNewsArticle> searchByKeyword(String query, int limit) {
        if (!newsProperties.hasNewsApiKey() || query == null || query.isBlank()) {
            return List.of();
        }
        int pageSize = Math.min(Math.max(limit, 1), 30);
        try {
            JsonNode root = restClient.get()
                    .uri("https://newsapi.org/v2/everything?q={q}&language=ko&sortBy=publishedAt&pageSize={size}",
                            query.trim(),
                            pageSize)
                    .header("X-Api-Key", newsProperties.getApiKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || !"ok".equals(root.path("status").asText())) {
                log.warn("NewsAPI 검색 응답 상태가 ok가 아닙니다. query={}", query);
                return List.of();
            }
            List<ExternalNewsArticle> articles = new ArrayList<>();
            for (JsonNode node : root.path("articles")) {
                ExternalNewsArticle article = mapArticle(node);
                if (article != null) {
                    articles.add(article);
                }
            }
            return articles;
        } catch (RestClientException ex) {
            log.warn("NewsAPI 검색 실패 query={}: {}", query, ex.getMessage());
            return List.of();
        }
    }

    public List<ExternalNewsArticle> fetchBusinessHeadlines() {
        if (!newsProperties.hasNewsApiKey()) {
            return List.of();
        }
        try {
            JsonNode root = restClient.get()
                    .uri(NEWS_API_URL)
                    .header("X-Api-Key", newsProperties.getApiKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || !"ok".equals(root.path("status").asText())) {
                log.warn("NewsAPI 응답 상태가 ok가 아닙니다.");
                return List.of();
            }
            List<ExternalNewsArticle> articles = new ArrayList<>();
            for (JsonNode node : root.path("articles")) {
                ExternalNewsArticle article = mapArticle(node);
                if (article != null) {
                    articles.add(article);
                }
            }
            return articles;
        } catch (RestClientException ex) {
            log.warn("NewsAPI 호출 실패: {}", ex.getMessage());
            return List.of();
        }
    }

    private ExternalNewsArticle mapArticle(JsonNode node) {
        String title = text(node, "title");
        String url = text(node, "url");
        if (title == null || url == null) {
            return null;
        }
        String description = text(node, "description");
        if (description == null || description.isBlank()) {
            description = title;
        }
        String sourceName = node.path("source").path("name").asText("NewsAPI");
        Instant publishedAt = parseInstant(text(node, "publishedAt"));
        return new ExternalNewsArticle(
                title.trim(),
                description.trim(),
                sourceName,
                url.trim(),
                null,
                null,
                publishedAt,
                "NEWSAPI"
        );
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String raw = value.asText().trim();
        return raw.isEmpty() ? null : raw;
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ex) {
            return Instant.now();
        }
    }
}
