package com.graphify.home.sentiment;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.config.GraphifyMarketProperties;
import com.graphify.home.dto.MarketSentimentSnapshotDto;
import com.graphify.home.dto.SentimentIndicatorDto;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * CNN Fear &amp; Greed 공식 데이터 API.
 * @see <a href="https://edition.cnn.com/markets/fear-and-greed">CNN Fear &amp; Greed</a>
 */
@Component
public class CnnFearGreedClient {

    private static final Logger log = LoggerFactory.getLogger(CnnFearGreedClient.class);

    private static final Map<String, String> INDICATOR_LABELS = Map.of(
            "market_momentum_sp500", "시장 모멘텀",
            "stock_price_strength", "주가 강도",
            "stock_price_breadth", "주가 폭",
            "put_call_options", "풋/콜 옵션",
            "market_volatility_vix", "시장 변동성",
            "junk_bond_demand", "정크본드 수요",
            "safe_haven_demand", "안전자산 수요"
    );

    private static final Map<String, String> INDICATOR_DESCRIPTIONS = Map.of(
            "market_momentum_sp500", "S&P 500 vs 125일 이동평균",
            "stock_price_strength", "NYSE 52주 신고가 vs 신저가",
            "stock_price_breadth", "McClellan Volume Summation",
            "put_call_options", "5일 평균 풋/콜 비율",
            "market_volatility_vix", "VIX vs 50일 이동평균",
            "junk_bond_demand", "정크본드 vs 투자등급 스프레드",
            "safe_haven_demand", "주식 vs 채권 20일 수익률 차"
    );

    private final GraphifyMarketProperties marketProperties;
    private final RestClient cnnFearGreedRestClient;

    public CnnFearGreedClient(
            GraphifyMarketProperties marketProperties,
            RestClient cnnFearGreedRestClient
    ) {
        this.marketProperties = marketProperties;
        this.cnnFearGreedRestClient = cnnFearGreedRestClient;
    }

    public Optional<MarketSentimentSnapshotDto> fetchUsSnapshot() {
        if (!marketProperties.isCnnFearGreedEnabled()) {
            return Optional.empty();
        }
        try {
            JsonNode root = cnnFearGreedRestClient.get()
                    .uri("/index/fearandgreed/graphdata")
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null) {
                return Optional.empty();
            }
            return Optional.of(parseSnapshot(root));
        } catch (RestClientException ex) {
            log.warn("CNN Fear & Greed API fetch failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    MarketSentimentSnapshotDto parseSnapshot(JsonNode root) {
        JsonNode fg = root.path("fear_and_greed");
        double score = fg.path("score").asDouble(50.0);
        String rating = textOrEmpty(fg, "rating");
        Instant quoteTime = parseInstant(textOrEmpty(fg, "timestamp"));

        List<SentimentIndicatorDto> indicators = new ArrayList<>();
        for (String key : INDICATOR_LABELS.keySet()) {
            JsonNode block = root.path(key);
            if (block.isMissingNode()) {
                continue;
            }
            double indicatorScore = block.path("score").asDouble(Double.NaN);
            if (Double.isNaN(indicatorScore)) {
                continue;
            }
            String indicatorRating = textOrEmpty(block, "rating");
            indicators.add(new SentimentIndicatorDto(
                    key,
                    INDICATOR_LABELS.get(key),
                    INDICATOR_DESCRIPTIONS.getOrDefault(key, key),
                    round(indicatorScore),
                    ratingToSignal(indicatorRating, indicatorScore)
            ));
        }

        Double vix = latestValueY(root.path("market_volatility_vix"));
        Double vixMa50 = latestValueY(root.path("market_volatility_vix_50"));

        return new MarketSentimentSnapshotDto(
                round(score),
                ratingToZone(rating, score),
                ratingToZoneLabel(rating, score),
                "US (CNN)",
                indicators,
                quoteTime,
                "CNN_OFFICIAL",
                vix,
                vixMa50
        );
    }

    private static Double latestValueY(JsonNode block) {
        if (block.isMissingNode()) {
            return null;
        }
        JsonNode data = block.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return null;
        }
        JsonNode last = data.get(data.size() - 1);
        double y = last.path("y").asDouble(Double.NaN);
        return Double.isNaN(y) ? null : round(y);
    }

    private static String ratingToZone(String rating, double score) {
        if (rating != null && !rating.isBlank()) {
            return switch (rating.toLowerCase(Locale.ROOT)) {
                case "extreme fear" -> "EXTREME_FEAR";
                case "fear" -> "FEAR";
                case "neutral" -> "NEUTRAL";
                case "greed" -> "GREED";
                case "extreme greed" -> "EXTREME_GREED";
                default -> zoneFromScore(score);
            };
        }
        return zoneFromScore(score);
    }

    private static String ratingToZoneLabel(String rating, double score) {
        if (rating != null && !rating.isBlank()) {
            return switch (rating.toLowerCase(Locale.ROOT)) {
                case "extreme fear" -> "극단적 공포";
                case "fear" -> "공포";
                case "neutral" -> "중립";
                case "greed" -> "탐욕";
                case "extreme greed" -> "극단적 탐욕";
                default -> zoneLabelFromScore(score);
            };
        }
        return zoneLabelFromScore(score);
    }

    private static String ratingToSignal(String rating, double score) {
        if (score >= 55) {
            return "GREED";
        }
        if (score <= 45) {
            return "FEAR";
        }
        return "NEUTRAL";
    }

    private static String zoneFromScore(double score) {
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

    private static String zoneLabelFromScore(double score) {
        return switch (zoneFromScore(score)) {
            case "EXTREME_FEAR" -> "극단적 공포";
            case "FEAR" -> "공포";
            case "NEUTRAL" -> "중립";
            case "GREED" -> "탐욕";
            default -> "극단적 탐욕";
        };
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ex) {
            return Instant.now();
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
