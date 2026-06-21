package com.graphify.market;

import com.graphify.trading.engine.Bar;
import com.graphify.trading.engine.MarketDataPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 엔진의 1차 시세 포트: market_bars(DB)를 조회. 비어 있으면 즉석 적재 후 재조회(self-healing).
 */
@Primary
@Component
public class DbMarketDataAdapter implements MarketDataPort {

    private final MarketBarRepository barRepository;
    private final MarketDataIngestionService ingestionService;
    private final MarketBarIntradayRepository intradayRepository;

    public DbMarketDataAdapter(
            MarketBarRepository barRepository,
            MarketDataIngestionService ingestionService,
            MarketBarIntradayRepository intradayRepository
    ) {
        this.barRepository = barRepository;
        this.ingestionService = ingestionService;
        this.intradayRepository = intradayRepository;
    }

    @Override
    public List<Bar> historicalDailyBars(String symbol) {
        List<MarketBar> bars = barRepository.findBySymbolOrderByTradingDateAsc(symbol);
        if (bars.isEmpty()) {
            ingestionService.ingestDaily(symbol);
            bars = barRepository.findBySymbolOrderByTradingDateAsc(symbol);
        }
        return bars.stream()
                .map(b -> new Bar(
                        b.getTradingDate(),
                        b.getClose(),
                        b.getVolume() == null ? null : b.getVolume().doubleValue()))
                .toList();
    }

    @Override
    public List<String> topVolumeSymbols(LocalDate date, int topN) {
        return barRepository.findTopVolumeSymbolsOnDate(date, PageRequest.of(0, topN));
    }

    @Override
    public List<String> symbolsByMarket(String market) {
        return barRepository.findDistinctKospi200Symbols(market);
    }

    @Override
    public List<MarketBarIntraday> recentIntradayBars(String symbol) {
        return intradayRepository.findBySymbolAndIntervalOrderByTsAsc(symbol, "5m");
    }
}
