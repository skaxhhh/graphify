package com.graphify.company.news;

import com.graphify.home.MarketNews;
import com.graphify.home.MarketNewsRepository;
import com.graphify.home.news.ExternalNewsArticle;
import com.graphify.home.news.NewsApiClient;
import com.graphify.home.news.RssNewsClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CompanyNewsSearchService {

    private static final int MAX_RESULTS = 15;

    private final NewsApiClient newsApiClient;
    private final RssNewsClient rssNewsClient;
    private final MarketNewsRepository marketNewsRepository;

    public CompanyNewsSearchService(
            NewsApiClient newsApiClient,
            RssNewsClient rssNewsClient,
            MarketNewsRepository marketNewsRepository
    ) {
        this.newsApiClient = newsApiClient;
        this.rssNewsClient = rssNewsClient;
        this.marketNewsRepository = marketNewsRepository;
    }

    public List<ExternalNewsArticle> searchForCompany(String companyName, String ticker) {
        List<String> queries = buildQueries(companyName, ticker);
        Map<String, ExternalNewsArticle> deduped = new LinkedHashMap<>();

        for (String query : queries) {
            for (ExternalNewsArticle article : newsApiClient.searchByKeyword(query, MAX_RESULTS)) {
                deduped.putIfAbsent(article.sourceUrl(), article);
            }
            for (MarketNews stored : marketNewsRepository.searchByKeyword(query, PageRequest.of(0, MAX_RESULTS))) {
                deduped.putIfAbsent(stored.getSourceUrl(), toArticle(stored));
            }
            for (ExternalNewsArticle article : rssNewsClient.searchByKeyword(query, MAX_RESULTS)) {
                deduped.putIfAbsent(article.sourceUrl(), article);
            }
        }

        return deduped.values().stream()
                .sorted(Comparator.comparing(ExternalNewsArticle::publishedAt).reversed())
                .limit(MAX_RESULTS)
                .toList();
    }

    private static ExternalNewsArticle toArticle(MarketNews news) {
        return new ExternalNewsArticle(
                news.getTitle(),
                news.getSummary() != null ? news.getSummary() : news.getTitle(),
                news.getSourceName(),
                news.getSourceUrl(),
                null,
                null,
                news.getPublishedAt(),
                news.getFeedSource() != null ? news.getFeedSource() : "MARKET_NEWS"
        );
    }

    private static List<String> buildQueries(String companyName, String ticker) {
        List<String> queries = new ArrayList<>();
        if (companyName != null && !companyName.isBlank()) {
            String trimmed = companyName.trim();
            queries.add(trimmed);
            String withoutLegal = trimmed
                    .replaceAll("\\(주\\)", "")
                    .replace("주식회사", "")
                    .replace("㈜", "")
                    .trim();
            if (!withoutLegal.isBlank() && !withoutLegal.equals(trimmed)) {
                queries.add(withoutLegal);
            }
            if (withoutLegal.length() >= 2) {
                queries.add(withoutLegal.substring(0, Math.min(withoutLegal.length(), 4)));
            }
        }
        if (ticker != null && !ticker.isBlank()) {
            queries.add(ticker.trim());
        }
        return queries.stream().filter(q -> q.length() >= 2).distinct().toList();
    }
}
