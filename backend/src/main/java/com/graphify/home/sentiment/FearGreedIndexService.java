package com.graphify.home.sentiment;

import com.graphify.home.dto.MarketSentimentDto;
import com.graphify.home.dto.MarketSentimentSnapshotDto;
import com.graphify.home.dto.SentimentIndicatorDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * CNN Fear &amp; Greed 방식 (feargreed.co.kr / CNN 참고). KOSPI·NASDAQ 각 7지표 동일 가중.
 */
@Service
public class FearGreedIndexService {

    private static final String KS11 = "^KS11";
    private static final String KQ11 = "^KQ11";
    private static final String VIX = "^VIX";
    private static final String BOND_KR = "148070.KS";
    private final YahooIndexSeriesClient yahooIndexSeriesClient;
    private final CnnFearGreedClient cnnFearGreedClient;

    public FearGreedIndexService(
            YahooIndexSeriesClient yahooIndexSeriesClient,
            CnnFearGreedClient cnnFearGreedClient
    ) {
        this.yahooIndexSeriesClient = yahooIndexSeriesClient;
        this.cnnFearGreedClient = cnnFearGreedClient;
    }

    public Optional<MarketSentimentDto> computeDualMarketSentiment() {
        Optional<MarketSentimentSnapshotDto> kospi = computeKospiSnapshot();
        Optional<MarketSentimentSnapshotDto> us = cnnFearGreedClient.fetchUsSnapshot();
        if (kospi.isEmpty() && us.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MarketSentimentDto(
                kospi.orElse(null),
                us.orElse(null),
                Instant.now()
        ));
    }

    private Optional<MarketSentimentSnapshotDto> computeKospiSnapshot() {
        Optional<YahooIndexSeriesClient.IndexSeries> indexOpt = yahooIndexSeriesClient.fetchDailySeries(KS11, "2y");
        if (indexOpt.isEmpty()) {
            return Optional.empty();
        }
        YahooIndexSeriesClient.IndexSeries index = indexOpt.get();
        List<Double> closes = index.closeValues();

        List<SentimentIndicatorDto> indicators = List.of(
                buildMomentum(index, "KOSPI"),
                buildStrength(closes, "KOSPI"),
                buildBreadth(closes, "KOSPI"),
                buildPutCallVix("CBOE VIX"),
                buildVolatility(closes, "KOSPI"),
                buildJunkKorea(),
                buildSafeHavenKorea(closes)
        );
        Double vix = fetchVixFromYahoo();
        Double vixMa50 = null;
        return Optional.of(toSnapshot(indicators, "KOSPI", index.quoteTime(), "YAHOO_PROXY", vix, vixMa50));
    }

    private Double fetchVixFromYahoo() {
        return yahooIndexSeriesClient.fetchDailySeries(VIX, "3mo")
                .map(YahooIndexSeriesClient.IndexSeries::latestPrice)
                .orElse(null);
    }

    private MarketSentimentSnapshotDto toSnapshot(
            List<SentimentIndicatorDto> indicators,
            String market,
            Instant quoteTime,
            String dataSource,
            Double vix,
            Double vixMa50
    ) {
        double score = indicators.stream()
                .mapToDouble(SentimentIndicatorDto::score)
                .average()
                .orElse(50.0);
        score = clamp(score, 0, 100);
        return new MarketSentimentSnapshotDto(
                Math.round(score * 10.0) / 10.0,
                resolveZone(score),
                resolveZoneLabel(score),
                market,
                indicators,
                quoteTime,
                dataSource,
                vix != null ? round(vix) : null,
                vixMa50 != null ? round(vixMa50) : null
        );
    }

    private SentimentIndicatorDto buildMomentum(YahooIndexSeriesClient.IndexSeries index, String market) {
        List<Double> closes = index.closeValues();
        double ma125 = sma(closes, 125);
        double price = index.latestPrice();
        double score = 50.0;
        if (!Double.isNaN(ma125) && ma125 > 0) {
            double ratio = (price - ma125) / ma125;
            score = clamp(50 + ratio * 500, 0, 100);
        }
        return indicator(
                "market_momentum",
                "시장 모멘텀",
                market + " vs 125일 이동평균",
                score,
                price >= ma125 ? "GREED" : "FEAR"
        );
    }

    private SentimentIndicatorDto buildStrength(List<Double> closes, String market) {
        double high52 = maxLast(closes, 252);
        double price = closes.get(closes.size() - 1);
        double score = high52 > 0 ? clamp((price / high52) * 100, 0, 100) : 50;
        return indicator(
                "stock_price_strength",
                "주가 강도",
                market + " 52주 고점 대비",
                score,
                score >= 50 ? "GREED" : "FEAR"
        );
    }

    private SentimentIndicatorDto buildBreadth(List<Double> closes, String market) {
        int window = Math.min(20, closes.size() - 1);
        int up = 0;
        for (int i = closes.size() - window; i < closes.size(); i++) {
            if (closes.get(i) > closes.get(i - 1)) {
                up++;
            }
        }
        double score = window > 0 ? (up * 100.0) / window : 50;
        return indicator(
                "stock_price_breadth",
                "주가 폭",
                market + " 최근 20거래일 상승일 비율",
                score,
                score >= 50 ? "GREED" : "FEAR"
        );
    }

    private SentimentIndicatorDto buildPutCallVix(String label) {
        double score = 50;
        Optional<YahooIndexSeriesClient.IndexSeries> vixOpt = yahooIndexSeriesClient.fetchDailySeries(VIX, "1y");
        if (vixOpt.isPresent()) {
            double vix = vixOpt.get().latestPrice();
            score = clamp(100 - ((vix - 12) / 28) * 100, 0, 100);
        }
        return indicator(
                "put_call_options",
                "풋/콜 옵션",
                label,
                score,
                score >= 50 ? "GREED" : "FEAR"
        );
    }

    private SentimentIndicatorDto buildVolatility(List<Double> closes, String market) {
        double vol20 = realizedVolatility(closes, 20);
        List<Double> rolling = rollingVolatility(closes, 20, 125);
        double min = rolling.stream().mapToDouble(v -> v).min().orElse(vol20);
        double max = rolling.stream().mapToDouble(v -> v).max().orElse(vol20);
        double normalized = max > min ? (vol20 - min) / (max - min) * 100 : 50;
        double score = clamp(100 - normalized, 0, 100);
        return indicator(
                "market_volatility",
                "시장 변동성",
                market + " 20일 실현변동성",
                score,
                score >= 50 ? "GREED" : "FEAR"
        );
    }

    private SentimentIndicatorDto buildJunkKorea() {
        double score = 50;
        Optional<YahooIndexSeriesClient.IndexSeries> kosdaqOpt = yahooIndexSeriesClient.fetchDailySeries(KQ11, "3mo");
        Optional<YahooIndexSeriesClient.IndexSeries> kospiOpt = yahooIndexSeriesClient.fetchDailySeries(KS11, "3mo");
        if (kosdaqOpt.isPresent() && kospiOpt.isPresent()) {
            double spread = returnOver(kosdaqOpt.get().closeValues(), 20)
                    - returnOver(kospiOpt.get().closeValues(), 20);
            score = clamp(50 + spread * 500, 0, 100);
        }
        return indicator(
                "junk_bond_demand",
                "정크본드 수요",
                "KOSDAQ−KOSPI 20일 수익률 차",
                score,
                score >= 50 ? "GREED" : "FEAR"
        );
    }

    private SentimentIndicatorDto buildSafeHavenKorea(List<Double> kospiCloses) {
        double score = 50;
        Optional<YahooIndexSeriesClient.IndexSeries> bondOpt = yahooIndexSeriesClient.fetchDailySeries(BOND_KR, "3mo");
        if (bondOpt.isPresent()) {
            double diff = returnOver(kospiCloses, 20) - returnOver(bondOpt.get().closeValues(), 20);
            score = clamp(50 + diff * 400, 0, 100);
        }
        return indicator(
                "safe_haven_demand",
                "안전자산 수요",
                "KOSPI vs 국고채 ETF 20일 수익률 차",
                score,
                score >= 50 ? "GREED" : "FEAR"
        );
    }

    private static SentimentIndicatorDto indicator(
            String id,
            String name,
            String description,
            double score,
            String signal
    ) {
        return new SentimentIndicatorDto(
                id,
                name,
                description,
                Math.round(score * 10.0) / 10.0,
                signal
        );
    }

    private static String resolveZone(double score) {
        if (score <= 24) {
            return "EXTREME_FEAR";
        }
        if (score <= 44) {
            return "FEAR";
        }
        if (score <= 54) {
            return "NEUTRAL";
        }
        if (score <= 74) {
            return "GREED";
        }
        return "EXTREME_GREED";
    }

    private static String resolveZoneLabel(double score) {
        return switch (resolveZone(score)) {
            case "EXTREME_FEAR" -> "극단적 공포";
            case "FEAR" -> "공포";
            case "NEUTRAL" -> "중립";
            case "GREED" -> "탐욕";
            default -> "극단적 탐욕";
        };
    }

    private static double sma(List<Double> values, int period) {
        if (values.size() < period) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = values.size() - period; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum / period;
    }

    private static double maxLast(List<Double> values, int period) {
        int from = Math.max(0, values.size() - period);
        return values.subList(from, values.size()).stream().mapToDouble(v -> v).max().orElse(Double.NaN);
    }

    private static double returnOver(List<Double> values, int days) {
        if (values.size() <= days) {
            return 0;
        }
        double end = values.get(values.size() - 1);
        double start = values.get(values.size() - 1 - days);
        if (start == 0) {
            return 0;
        }
        return (end - start) / start;
    }

    private static double realizedVolatility(List<Double> closes, int window) {
        if (closes.size() <= window) {
            return 0;
        }
        List<Double> returns = new ArrayList<>();
        for (int i = closes.size() - window; i < closes.size(); i++) {
            double prev = closes.get(i - 1);
            if (prev != 0) {
                returns.add((closes.get(i) - prev) / prev);
            }
        }
        if (returns.isEmpty()) {
            return 0;
        }
        double mean = returns.stream().mapToDouble(v -> v).average().orElse(0);
        double variance = returns.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance) * Math.sqrt(252) * 100;
    }

    private static List<Double> rollingVolatility(List<Double> closes, int window, int maxPoints) {
        List<Double> result = new ArrayList<>();
        int start = Math.max(window + 1, closes.size() - maxPoints);
        for (int i = start; i < closes.size(); i++) {
            result.add(realizedVolatility(closes.subList(0, i + 1), window));
        }
        return result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
