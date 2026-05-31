package com.graphify.home.news;

import com.graphify.config.GraphifyNewsProperties;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RssNewsClient {

    private static final Logger log = LoggerFactory.getLogger(RssNewsClient.class);

    private final GraphifyNewsProperties newsProperties;
    private final RestClient restClient;

    public RssNewsClient(GraphifyNewsProperties newsProperties, RestClient newsRestClient) {
        this.newsProperties = newsProperties;
        this.restClient = newsRestClient;
    }

    public List<ExternalNewsArticle> fetchFeeds() {
        List<ExternalNewsArticle> merged = new ArrayList<>();
        for (String feedUrl : newsProperties.getRssFeeds()) {
            merged.addAll(fetchSingleFeed(feedUrl));
        }
        return merged;
    }

    public List<ExternalNewsArticle> searchByKeyword(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        int max = Math.max(limit, 1);
        List<ExternalNewsArticle> matched = new ArrayList<>();
        for (ExternalNewsArticle article : fetchFeeds()) {
            if (containsKeyword(article, needle)) {
                matched.add(article);
                if (matched.size() >= max) {
                    break;
                }
            }
        }
        return matched;
    }

    private static boolean containsKeyword(ExternalNewsArticle article, String needle) {
        return contains(article.title(), needle)
                || contains(article.summary(), needle)
                || contains(article.companyName(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private List<ExternalNewsArticle> fetchSingleFeed(String feedUrl) {
        try {
            byte[] xml = restClient.get().uri(feedUrl).retrieve().body(byte[].class);
            if (xml == null || xml.length == 0) {
                return List.of();
            }
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed;
            try (InputStream stream = new ByteArrayInputStream(xml)) {
                feed = input.build(new XmlReader(stream));
            }
            String sourceName = feed.getTitle() != null ? feed.getTitle() : hostName(feedUrl);
            List<ExternalNewsArticle> articles = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                ExternalNewsArticle article = mapEntry(entry, sourceName, feedUrl);
                if (article != null) {
                    articles.add(article);
                }
            }
            return articles;
        } catch (Exception ex) {
            log.warn("RSS 수집 실패 {}: {}", feedUrl, ex.getMessage());
            return List.of();
        }
    }

    private ExternalNewsArticle mapEntry(SyndEntry entry, String sourceName, String feedUrl) {
        String title = entry.getTitle();
        if (title == null || title.isBlank()) {
            return null;
        }
        String link = entry.getLink();
        if (link == null || link.isBlank()) {
            return null;
        }
        String summary = entry.getDescription() != null
                ? entry.getDescription().getValue()
                : title;
        summary = stripHtml(summary);
        if (summary.length() > 500) {
            summary = summary.substring(0, 497) + "...";
        }
        Instant publishedAt = toInstant(entry.getPublishedDate());
        return new ExternalNewsArticle(
                title.trim(),
                summary.trim(),
                sourceName,
                link.trim(),
                null,
                null,
                publishedAt,
                "RSS:" + hostName(feedUrl)
        );
    }

    private Instant toInstant(Date date) {
        if (date == null) {
            return Instant.now();
        }
        return date.toInstant();
    }

    private String stripHtml(String raw) {
        return raw.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String hostName(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception ex) {
            return "RSS";
        }
    }
}
