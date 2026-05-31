package com.graphify.company.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.config.GraphifyMarketProperties;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * KRX Data Marketplace Open API — 일별매매(시장 전체) 조회 후 종목 필터.
 * API별 활용 신청·승인 필요: https://openapi.krx.co.kr/
 */
@Component
public class KrxOpenApiClient {

    private static final Logger log = LoggerFactory.getLogger(KrxOpenApiClient.class);

    private final GraphifyMarketProperties properties;
    private final RestClient krxRestClient;

    public KrxOpenApiClient(GraphifyMarketProperties properties, RestClient krxRestClient) {
        this.properties = properties;
        this.krxRestClient = krxRestClient;
    }

    public Optional<KrxDailyTradeRow> findStockOnDate(String market, String basDd, String stockCode) {
        if (!properties.hasKrxApiKey() || basDd == null || stockCode == null) {
            return Optional.empty();
        }
        String endpoint = dailyTradeEndpoint(market);
        try {
            JsonNode root = krxRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("basDd", basDd)
                            .build())
                    .header("AUTH_KEY", properties.getKrxApiKey().trim())
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null) {
                return Optional.empty();
            }
            JsonNode rows = root.path("OutBlock_1");
            if (!rows.isArray()) {
                return Optional.empty();
            }
            String target = stockCode.trim();
            for (JsonNode row : rows) {
                String code = text(row, "ISU_SRT_CD");
                if (code != null && target.equals(code.trim())) {
                    return Optional.of(new KrxDailyTradeRow(
                            basDd,
                            code,
                            text(row, "ISU_NM"),
                            parseDouble(row, "TDD_CLSPRC"),
                            parseDouble(row, "FLUC_RT"),
                            parseDouble(row, "CMPPREVDD_PRC")
                    ));
                }
            }
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("KRX daily trade fetch failed market={} basDd={}: {}", market, basDd, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String dailyTradeEndpoint(String market) {
        if (market != null && market.toUpperCase(Locale.ROOT).contains("KOSDAQ")) {
            return "/ksq_bydd_trd";
        }
        return "/stk_bydd_trd";
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String raw = value.asText().trim();
        return raw.isEmpty() ? null : raw;
    }

    private static double parseDouble(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw.replace(",", ""));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    public record KrxDailyTradeRow(
            String basDd,
            String stockCode,
            String stockName,
            double closePrice,
            double fluctuationRate,
            double compareToPrevious
    ) {
    }
}
