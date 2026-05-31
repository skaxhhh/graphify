package com.graphify.home.sentiment;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.config.GraphifyMarketProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class YahooIndexSeriesClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GraphifyMarketProperties marketProperties;
    private final RestClient yahooRestClient;

    public YahooIndexSeriesClient(GraphifyMarketProperties marketProperties, RestClient yahooRestClient) {
        this.marketProperties = marketProperties;
        this.yahooRestClient = yahooRestClient;
    }

    public Optional<IndexSeries> fetchDailySeries(String yahooSymbol, String range) {
        if (!marketProperties.isYahooEnabled()) {
            return Optional.empty();
        }
        try {
            JsonNode root = yahooRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v8/finance/chart/{symbol}")
                            .queryParam("interval", "1d")
                            .queryParam("range", range)
                            .queryParam("includePrePost", "false")
                            .build(yahooSymbol))
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null) {
                return Optional.empty();
            }
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = result.get(0);
            JsonNode meta = first.path("meta");
            double latest = meta.path("regularMarketPrice").asDouble(Double.NaN);
            long latestEpoch = meta.path("regularMarketTime").asLong(0);
            if (Double.isNaN(latest) || latestEpoch <= 0) {
                return Optional.empty();
            }
            List<DailyClose> closes = parseCloses(first);
            if (closes.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new IndexSeries(
                    yahooSymbol,
                    latest,
                    Instant.ofEpochSecond(latestEpoch),
                    closes
            ));
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    private static List<DailyClose> parseCloses(JsonNode result) {
        JsonNode timestamps = result.path("timestamp");
        JsonNode closeArray = result.path("indicators").path("quote").get(0).path("close");
        if (!timestamps.isArray() || !closeArray.isArray()) {
            return List.of();
        }
        int len = Math.min(timestamps.size(), closeArray.size());
        List<DailyClose> closes = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            JsonNode closeNode = closeArray.get(i);
            if (closeNode == null || closeNode.isNull()) {
                continue;
            }
            double close = closeNode.asDouble(Double.NaN);
            long epoch = timestamps.get(i).asLong(0);
            if (Double.isNaN(close) || epoch <= 0) {
                continue;
            }
            LocalDate date = Instant.ofEpochSecond(epoch).atZone(KST).toLocalDate();
            closes.add(new DailyClose(date, close));
        }
        return closes;
    }

    public record IndexSeries(
            String symbol,
            double latestPrice,
            Instant quoteTime,
            List<DailyClose> closes
    ) {
        public List<Double> closeValues() {
            return closes.stream().map(DailyClose::close).toList();
        }
    }

    public record DailyClose(LocalDate date, double close) {
    }
}
