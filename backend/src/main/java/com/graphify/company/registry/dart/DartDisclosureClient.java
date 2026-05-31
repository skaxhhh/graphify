package com.graphify.company.registry.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.config.GraphifyDartProperties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DartDisclosureClient {

    private static final Logger log = LoggerFactory.getLogger(DartDisclosureClient.class);
    private static final String LIST_URL = "https://opendart.fss.or.kr/api/list.json";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final GraphifyDartProperties dartProperties;
    private final RestClient dartRestClient;

    public DartDisclosureClient(GraphifyDartProperties dartProperties, RestClient dartRestClient) {
        this.dartProperties = dartProperties;
        this.dartRestClient = dartRestClient;
    }

    public List<DartDisclosureItem> fetchRecentDisclosures(String corpCode, int pageCount) {
        return fetchRecentDisclosures(corpCode, pageCount, 3);
    }

    public List<DartDisclosureItem> fetchRecentDisclosures(String corpCode, int pageCount, int monthsBack) {
        if (!dartProperties.hasApiKey() || corpCode == null || corpCode.isBlank()) {
            return List.of();
        }
        LocalDate end = LocalDate.now();
        int months = Math.min(Math.max(monthsBack, 1), 12);
        LocalDate begin = end.minusMonths(months);
        try {
            JsonNode root = dartRestClient.get()
                    .uri(LIST_URL + "?crtfc_key={key}&corp_code={corp}&bgn_de={begin}&end_de={end}&page_no=1&page_count={count}",
                            dartProperties.getApiKey().trim(),
                            corpCode.trim(),
                            begin.format(DATE_FMT),
                            end.format(DATE_FMT),
                            Math.min(Math.max(pageCount, 1), 100))
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || !"000".equals(root.path("status").asText())) {
                return List.of();
            }
            List<DartDisclosureItem> items = new ArrayList<>();
            for (JsonNode node : root.path("list")) {
                String reportName = text(node, "report_nm");
                if (reportName == null) {
                    continue;
                }
                items.add(new DartDisclosureItem(
                        text(node, "rcept_no"),
                        text(node, "rcept_dt"),
                        reportName,
                        text(node, "flr_nm")
                ));
            }
            return items;
        } catch (RestClientException ex) {
            log.warn("DART list.json 실패 corp_code={}: {}", corpCode, ex.getMessage());
            return List.of();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String raw = value.asText().trim();
        return raw.isEmpty() ? null : raw;
    }
}
