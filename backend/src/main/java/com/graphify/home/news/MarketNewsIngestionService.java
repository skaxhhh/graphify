package com.graphify.home.news;

import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.home.MarketNews;
import com.graphify.home.MarketNewsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketNewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarketNewsIngestionService.class);
    private static final int MAX_STORE = 60;

    private final NewsApiClient newsApiClient;
    private final RssNewsClient rssNewsClient;
    private final MarketNewsRepository marketNewsRepository;
    private final CompanyRepository companyRepository;

    public MarketNewsIngestionService(
            NewsApiClient newsApiClient,
            RssNewsClient rssNewsClient,
            MarketNewsRepository marketNewsRepository,
            CompanyRepository companyRepository
    ) {
        this.newsApiClient = newsApiClient;
        this.rssNewsClient = rssNewsClient;
        this.marketNewsRepository = marketNewsRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int refreshFromProviders() {
        List<ExternalNewsArticle> collected = new ArrayList<>();
        collected.addAll(newsApiClient.fetchBusinessHeadlines());
        collected.addAll(rssNewsClient.fetchFeeds());

        if (collected.isEmpty()) {
            log.warn("수집된 시장 뉴스가 없습니다. RSS URL 또는 NEWS_API_KEY를 확인하세요.");
            return 0;
        }

        List<Company> companies = companyRepository.findAll();
        Map<String, ExternalNewsArticle> deduped = new HashMap<>();
        for (ExternalNewsArticle article : collected) {
            if (article.sourceUrl() == null || article.sourceUrl().isBlank()) {
                continue;
            }
            ExternalNewsArticle enriched = enrichWithCompany(article, companies);
            deduped.putIfAbsent(article.sourceUrl(), enriched);
        }

        List<ExternalNewsArticle> sorted = deduped.values().stream()
                .sorted(Comparator.comparing(ExternalNewsArticle::publishedAt).reversed())
                .limit(MAX_STORE)
                .toList();

        int saved = 0;
        for (ExternalNewsArticle article : sorted) {
            if (upsert(article)) {
                saved++;
            }
        }

        trimOldArticles(MAX_STORE * 2);
        log.info("시장 뉴스 동기화 완료: {}건 저장/갱신 (수집 {}건)", saved, sorted.size());
        return saved;
    }

    private boolean upsert(ExternalNewsArticle article) {
        return marketNewsRepository.findBySourceUrl(article.sourceUrl())
                .map(existing -> {
                    existing.setTitle(article.title());
                    existing.setSummary(article.summary());
                    existing.setSourceName(article.sourceName());
                    existing.setPublishedAt(article.publishedAt());
                    existing.setTicker(article.ticker());
                    existing.setCompanyName(article.companyName());
                    existing.setFeedSource(article.feedSource());
                    marketNewsRepository.save(existing);
                    return true;
                })
                .orElseGet(() -> {
                    MarketNews entity = new MarketNews();
                    entity.setTitle(article.title());
                    entity.setSummary(article.summary());
                    entity.setSourceName(article.sourceName());
                    entity.setSourceUrl(article.sourceUrl());
                    entity.setPublishedAt(article.publishedAt());
                    entity.setTicker(article.ticker());
                    entity.setCompanyName(article.companyName());
                    entity.setFeedSource(article.feedSource());
                    marketNewsRepository.save(entity);
                    return true;
                });
    }

    private void trimOldArticles(int keepCount) {
        List<MarketNews> ordered = marketNewsRepository.findAllByOrderByPublishedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, keepCount + 500)
        );
        if (ordered.size() <= keepCount) {
            return;
        }
        marketNewsRepository.deleteAll(ordered.subList(keepCount, ordered.size()));
    }

    private ExternalNewsArticle enrichWithCompany(ExternalNewsArticle article, List<Company> companies) {
        String haystack = article.title() + " " + article.summary();
        for (Company company : companies) {
            if (haystack.contains(company.getName())) {
                return new ExternalNewsArticle(
                        article.title(),
                        article.summary(),
                        article.sourceName(),
                        article.sourceUrl(),
                        company.getTicker(),
                        company.getName(),
                        article.publishedAt(),
                        article.feedSource()
                );
            }
        }
        return article;
    }
}
