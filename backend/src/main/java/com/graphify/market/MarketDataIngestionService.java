package com.graphify.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import com.graphify.company.market.IntradayBar;
import com.graphify.company.market.OhlcvBar;
import com.graphify.company.market.YahooFinanceChartClient;
import com.graphify.company.market.YahooSymbolResolver;
import com.graphify.trading.rule.TradingRule;
import com.graphify.trading.rule.TradingRuleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시세 적재. 룰 유니버스에 등장하는 종목만 수집해 market_bars(_intraday)에 upsert.
 * Cloud Scheduler 가 InternalMarketController 를 통해 주기별로 호출한다.
 */
@Service
public class MarketDataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataIngestionService.class);

    private final TradingRuleRepository ruleRepository;
    private final CompanyRepository companyRepository;
    private final YahooFinanceChartClient yahooClient;
    private final MarketBarRepository barRepository;
    private final MarketBarIntradayRepository intradayRepository;
    private final ObjectMapper objectMapper;

    public MarketDataIngestionService(
            TradingRuleRepository ruleRepository,
            CompanyRepository companyRepository,
            YahooFinanceChartClient yahooClient,
            MarketBarRepository barRepository,
            MarketBarIntradayRepository intradayRepository,
            ObjectMapper objectMapper
    ) {
        this.ruleRepository = ruleRepository;
        this.companyRepository = companyRepository;
        this.yahooClient = yahooClient;
        this.barRepository = barRepository;
        this.intradayRepository = intradayRepository;
        this.objectMapper = objectMapper;
    }

    /** 룰에 등장하는 전 종목 일봉 적재. 적재된 종목 수 반환. */
    public int ingestDailyForActiveSymbols() {
        Set<String> symbols = activeSymbols();
        int count = 0;
        for (String symbol : symbols) {
            if (ingestDaily(symbol) > 0) {
                count++;
            }
        }
        log.info("Daily ingestion done: {} symbols", count);
        return count;
    }

    /**
     * KOSPI 200 전체 종목 일봉 적재.
     * companies 테이블의 in_kospi200=TRUE 종목을 순회하며 ingestDaily() 호출.
     * 적재된 종목 수 반환 (ingestDaily() > 0인 종목만 카운트).
     * 스케줄 자동화는 Phase 2에서 추가 예정 — 현재는 HTTP 엔드포인트로 수동 트리거.
     */
    public int ingestDailyForKospi200() {
        List<Company> kospi200 = companyRepository.findByInKospi200True();
        int count = 0;
        int failed = 0;
        for (Company company : kospi200) {
            if (company.getTicker() == null) {
                log.debug("Skip KOSPI 200 company without ticker: {}", company.getId());
                continue;
            }
            // 종목 단위 격리: 한 종목의 외부호출/저장 실패가 전체 적재를 중단/500화하지 않게 한다.
            try {
                if (ingestDaily(company.getTicker()) > 0) {
                    count++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("KOSPI 200 ingest: ticker={} 처리 실패 — 건너뜀: {}",
                        company.getTicker(), e.toString());
            }
        }
        log.info("KOSPI 200 daily ingestion done: {} / {} symbols ingested, {} failed",
                count, kospi200.size(), failed);
        return count;
    }

    /** 청크 단위 적재 결과. nextOffset부터 이어서 호출, done=true면 종료. */
    public record BatchResult(
            int processed, int ingested, int failed, int total, int nextOffset, boolean done) {
    }

    /**
     * KOSPI200 일봉을 청크(offset~offset+size)로 적재한다. Cloud Run 기본 CPU 정책에서도
     * 각 청크가 독립 HTTP 요청이라 요청 처리 중 CPU를 받아 안전하게 완주한다(프론트가 nextOffset으로 순회).
     * 종목 정렬은 id 오름차순으로 고정해 호출 간 슬라이스가 일관되게 한다.
     */
    public BatchResult ingestDailyForKospi200Batch(int offset, int size) {
        int safeOffset = Math.max(0, offset);
        int safeSize = Math.max(1, size);
        java.util.List<Company> all = new java.util.ArrayList<>(companyRepository.findByInKospi200True());
        all.sort(java.util.Comparator.comparing(Company::getId));
        int total = all.size();
        int end = Math.min(safeOffset + safeSize, total);

        int processed = 0;
        int ingested = 0;
        int failed = 0;
        for (int i = safeOffset; i < end; i++) {
            Company company = all.get(i);
            processed++;
            if (company.getTicker() == null) {
                continue;
            }
            try {
                if (ingestDaily(company.getTicker()) > 0) {
                    ingested++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("KOSPI 200 ingest batch: ticker={} 처리 실패 — 건너뜀: {}",
                        company.getTicker(), e.toString());
            }
        }
        boolean done = end >= total;
        return new BatchResult(processed, ingested, failed, total, end, done);
    }

    /** 룰에 등장하는 전 종목 분봉 적재(interval/range). 적재된 종목 수 반환. */
    public int ingestIntradayForActiveSymbols(String interval, String range) {
        Set<String> symbols = activeSymbols();
        int count = 0;
        for (String symbol : symbols) {
            if (ingestIntraday(symbol, interval, range) > 0) {
                count++;
            }
        }
        log.info("Intraday({}) ingestion done: {} symbols", interval, count);
        return count;
    }

    /**
     * 단일 종목 일봉을 독립 트랜잭션(REQUIRES_NEW)으로 적재한다.
     * {@link #ingestIntradayInNewTx}와 동일 목적 — 호출측 트랜잭션 격리.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int ingestDailyInNewTx(String symbol) {
        return ingestDaily(symbol);
    }

    /** 단일 종목 일봉 적재(없는 봉 insert, 있으면 update). 적재 행 수 반환. */
    @Transactional
    public int ingestDaily(String symbol) {
        Optional<String> yahoo = resolveYahoo(symbol);
        if (yahoo.isEmpty()) {
            return 0;
        }
        List<OhlcvBar> bars = yahooClient.fetchDailyOhlcv(yahoo.get());
        int n = 0;
        for (OhlcvBar b : bars) {
            barRepository.findBySymbolAndTradingDate(symbol, b.tradingDate())
                    .ifPresentOrElse(
                            existing -> existing.update(b.open(), b.high(), b.low(), b.close(), b.volume()),
                            () -> barRepository.save(new MarketBar(
                                    symbol, b.tradingDate(), b.open(), b.high(), b.low(),
                                    b.close(), b.volume(), "YAHOO")));
            n++;
        }
        return n;
    }

    /**
     * 단일 종목 분봉을 독립 트랜잭션(REQUIRES_NEW)으로 적재한다.
     * 호출측(예: PaperLifecycleService.start)이 자체 트랜잭션 안에서 여러 종목을 즉시 수집할 때,
     * 한 종목의 실패가 호출측 트랜잭션을 rollback-only로 오염시키지 않도록 격리한다.
     * 호출측은 예외를 catch해 다른 종목 수집을 계속할 수 있다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int ingestIntradayInNewTx(String symbol, String interval, String range) {
        return ingestIntraday(symbol, interval, range);
    }

    @Transactional
    public int ingestIntraday(String symbol, String interval, String range) {
        Optional<String> yahoo = resolveYahoo(symbol);
        if (yahoo.isEmpty()) {
            return 0;
        }
        List<IntradayBar> bars = yahooClient.fetchIntraday(yahoo.get(), interval, range);
        int n = 0;
        for (IntradayBar b : bars) {
            intradayRepository.findBySymbolAndIntervalAndTs(symbol, interval, b.ts())
                    .ifPresentOrElse(
                            existing -> existing.update(b.open(), b.high(), b.low(), b.close(), b.volume()),
                            () -> intradayRepository.save(new MarketBarIntraday(
                                    symbol, b.ts(), interval, b.open(), b.high(), b.low(),
                                    b.close(), b.volume(), "YAHOO")));
            n++;
        }
        return n;
    }

    private Optional<String> resolveYahoo(String symbol) {
        Optional<Company> company = companyRepository.findByTicker(symbol);
        String market = company.map(Company::getMarket).orElse(null);
        return YahooSymbolResolver.resolve(symbol, market);
    }

    /**
     * 모든 룰 definition 의 universe 합집합.
     * - symbols 타입: universe.symbols 배열을 그대로 사용
     * - volume_top_n 타입: universe.symbols 가 없으므로 KOSPI200 전체 종목을 후보로 추가
     *   (거래일별 상위 N 선정은 백테스트/평가 단계에서 수행 — 수집은 후보 전체가 필요).
     *   universe.additionalSymbols 도 함께 포함.
     */
    private Set<String> activeSymbols() {
        Set<String> symbols = new LinkedHashSet<>();
        for (TradingRule rule : ruleRepository.findAll()) {
            try {
                JsonNode universe = objectMapper.readTree(rule.getDefinition()).path("universe");
                String type = universe.path("type").asText("");

                if ("volume_top_n".equals(type)) {
                    companyRepository.findByInKospi200True().forEach(c -> {
                        if (c.getTicker() != null && !c.getTicker().isBlank()) {
                            symbols.add(c.getTicker().trim());
                        }
                    });
                    addSymbolsFrom(universe.path("additionalSymbols"), symbols);
                } else {
                    addSymbolsFrom(universe.path("symbols"), symbols);
                }
            } catch (Exception e) {
                log.debug("Skip rule {} symbol parse: {}", rule.getId(), e.getMessage());
            }
        }
        return symbols;
    }

    private static void addSymbolsFrom(JsonNode arr, Set<String> symbols) {
        if (arr.isArray()) {
            arr.forEach(n -> {
                String s = n.asText(null);
                if (s != null && !s.isBlank()) {
                    symbols.add(s.trim());
                }
            });
        }
    }
}
