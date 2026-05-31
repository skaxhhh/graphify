package com.graphify.home;

import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.config.GraphifyNewsProperties;
import com.graphify.home.dto.BuzzCompanyDto;
import com.graphify.home.dto.MarketNewsItemDto;
import com.graphify.home.dto.MarketSentimentDto;
import com.graphify.home.dto.TrendingCompanyDto;
import com.graphify.home.naver.NaverPopularSearchClient;
import com.graphify.home.naver.NaverPopularSearchClient.NaverPopularStock;
import com.graphify.home.news.MarketNewsIngestionService;
import com.graphify.home.sentiment.FearGreedIndexService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {

    private static final int DEFAULT_TRENDING_LIMIT = 8;
    private static final int DEFAULT_NEWS_LIMIT = 12;
    private static final int MAX_LIMIT = 20;

    private final CompanyViewStatsRepository companyViewStatsRepository;
    private final CompanyRepository companyRepository;
    private final MarketNewsRepository marketNewsRepository;
    private final MarketNewsIngestionService marketNewsIngestionService;
    private final GraphifyNewsProperties newsProperties;
    private final NaverPopularSearchClient naverPopularSearchClient;
    private final FearGreedIndexService fearGreedIndexService;
    private final AtomicReference<Instant> lastNewsRefresh = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<CachedSentiment> sentimentCache = new AtomicReference<>();

    public HomeService(
            CompanyViewStatsRepository companyViewStatsRepository,
            CompanyRepository companyRepository,
            MarketNewsRepository marketNewsRepository,
            MarketNewsIngestionService marketNewsIngestionService,
            GraphifyNewsProperties newsProperties,
            NaverPopularSearchClient naverPopularSearchClient,
            FearGreedIndexService fearGreedIndexService
    ) {
        this.companyViewStatsRepository = companyViewStatsRepository;
        this.companyRepository = companyRepository;
        this.marketNewsRepository = marketNewsRepository;
        this.marketNewsIngestionService = marketNewsIngestionService;
        this.newsProperties = newsProperties;
        this.naverPopularSearchClient = naverPopularSearchClient;
        this.fearGreedIndexService = fearGreedIndexService;
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TrendingCompanyDto>> getTrendingCompanies(Integer limit) {
        int size = normalizeLimit(limit, DEFAULT_TRENDING_LIMIT);
        List<CompanyViewStats> stats = companyViewStatsRepository.findTopByViewCount(
                PageRequest.of(0, size)
        );
        if (stats.isEmpty()) {
            return ApiResponse.ok(List.of());
        }

        List<Long> ids = stats.stream().map(CompanyViewStats::getCompanyId).toList();
        Map<Long, Company> companies = companyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));

        List<TrendingCompanyDto> result = new ArrayList<>();
        int rank = 1;
        for (CompanyViewStats row : stats) {
            Company company = companies.get(row.getCompanyId());
            if (company == null) {
                continue;
            }
            result.add(new TrendingCompanyDto(
                    rank++,
                    company.getId(),
                    company.getName(),
                    company.getTicker(),
                    company.getIndustry(),
                    row.getViewCount()
            ));
        }
        return ApiResponse.ok(result);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<MarketNewsItemDto>> getMarketNews(Integer limit) {
        refreshNewsIfStale();
        int size = normalizeLimit(limit, DEFAULT_NEWS_LIMIT);
        List<MarketNewsItemDto> items = marketNewsRepository
                .findAllByOrderByPublishedAtDesc(PageRequest.of(0, size))
                .stream()
                .map(this::toNewsDto)
                .toList();
        return ApiResponse.ok(items);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<BuzzCompanyDto>> getBuzzCompanies(Integer limit) {
        int size = normalizeLimit(limit, DEFAULT_TRENDING_LIMIT);
        List<NaverPopularStock> popular = naverPopularSearchClient.fetchPopularStocks();
        List<BuzzCompanyDto> result = new ArrayList<>();
        for (NaverPopularStock stock : popular) {
            if (result.size() >= size) {
                break;
            }
            Optional<Company> company = companyRepository.findByTicker(stock.ticker());
            result.add(new BuzzCompanyDto(
                    stock.rank(),
                    company.map(Company::getId).orElse(null),
                    company.map(Company::getName).orElse(stock.name()),
                    stock.ticker(),
                    company.map(Company::getIndustry).orElse(null),
                    stock.price(),
                    stock.priceDirection(),
                    "네이버 금융 인기검색"
            ));
        }
        return ApiResponse.ok(result);
    }

    public ApiResponse<MarketSentimentDto> getMarketSentiment() {
        CachedSentiment cached = sentimentCache.get();
        if (cached != null && Duration.between(cached.fetchedAt(), Instant.now()).toMinutes() < 10) {
            return ApiResponse.ok(cached.payload());
        }
        MarketSentimentDto payload = fearGreedIndexService.computeDualMarketSentiment()
                .filter(dto -> dto.kospi() != null || dto.nasdaq() != null)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_HOME_001",
                        "공탐 지수를 계산하지 못했습니다.",
                        org.springframework.http.HttpStatus.BAD_GATEWAY
                ));
        sentimentCache.set(new CachedSentiment(payload, Instant.now()));
        return ApiResponse.ok(payload);
    }

    private void refreshNewsIfStale() {
        int refreshMinutes = Math.max(5, newsProperties.getRefreshMinutes());
        Instant last = lastNewsRefresh.get();
        boolean stale = Duration.between(last, Instant.now()).toMinutes() >= refreshMinutes;
        boolean empty = marketNewsRepository.count() == 0;

        if (!stale && !empty) {
            return;
        }

        synchronized (this) {
            Instant lockedLast = lastNewsRefresh.get();
            boolean stillStale = Duration.between(lockedLast, Instant.now()).toMinutes() >= refreshMinutes;
            boolean stillEmpty = marketNewsRepository.count() == 0;
            if (!stillStale && !stillEmpty) {
                return;
            }
            marketNewsIngestionService.refreshFromProviders();
            lastNewsRefresh.set(Instant.now());
        }
    }

    private MarketNewsItemDto toNewsDto(MarketNews news) {
        return new MarketNewsItemDto(
                news.getId(),
                news.getTitle(),
                news.getSummary(),
                news.getSourceName(),
                news.getSourceUrl(),
                news.getTicker(),
                news.getCompanyName(),
                news.getPublishedAt()
        );
    }

    private int normalizeLimit(Integer limit, int defaultValue) {
        if (limit == null || limit < 1) {
            return defaultValue;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private record CachedSentiment(MarketSentimentDto payload, Instant fetchedAt) {
    }
}
