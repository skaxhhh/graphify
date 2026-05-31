package com.graphify.company.registry.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.config.GraphifyDartProperties;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DartFinancialClient {

    private static final Logger log = LoggerFactory.getLogger(DartFinancialClient.class);
    private static final String FNLTT_URL = "https://opendart.fss.or.kr/api/fnlttSinglAcnt.json";
    private static final Set<String> KEY_ACCOUNTS = Set.of(
            "매출액",
            "영업이익",
            "영업이익(손실)",
            "당기순이익",
            "당기순이익(손실)"
    );

    private static final List<ReportPeriod> REPORT_PERIODS = List.of(
            new ReportPeriod("11014", "3분기보고서", 4),
            new ReportPeriod("11012", "반기보고서", 3),
            new ReportPeriod("11013", "1분기보고서", 2),
            new ReportPeriod("11011", "사업보고서", 1)
    );

    private final GraphifyDartProperties dartProperties;
    private final RestClient dartRestClient;

    public DartFinancialClient(GraphifyDartProperties dartProperties, RestClient dartRestClient) {
        this.dartProperties = dartProperties;
        this.dartRestClient = dartRestClient;
    }

    public List<DartFinancialLine> fetchRecentFinancials(String corpCode) {
        if (!dartProperties.hasApiKey() || corpCode == null || corpCode.isBlank()) {
            return List.of();
        }
        int currentYear = Year.now().getValue();
        List<DartFinancialLine> merged = new ArrayList<>();

        for (int year = currentYear; year >= currentYear - 1; year--) {
            String bsnsYear = String.valueOf(year);
            for (ReportPeriod period : REPORT_PERIODS) {
                List<DartFinancialLine> cfs = fetchPeriod(corpCode, bsnsYear, period, "CFS");
                if (!cfs.isEmpty()) {
                    merged.addAll(cfs);
                } else {
                    merged.addAll(fetchPeriod(corpCode, bsnsYear, period, "OFS"));
                }
            }
        }

        return sortLines(dedupeLines(merged));
    }

    private List<DartFinancialLine> fetchPeriod(
            String corpCode,
            String bsnsYear,
            ReportPeriod period,
            String fsDiv
    ) {
        try {
            JsonNode root = dartRestClient.get()
                    .uri(FNLTT_URL + "?crtfc_key={key}&corp_code={corp}&bsns_year={year}&reprt_code={reprt}&fs_div={fs}",
                            dartProperties.getApiKey().trim(),
                            corpCode.trim(),
                            bsnsYear,
                            period.code(),
                            fsDiv)
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || !"000".equals(root.path("status").asText())) {
                return List.of();
            }
            List<DartFinancialLine> lines = new ArrayList<>();
            for (JsonNode node : root.path("list")) {
                String accountName = text(node, "account_nm");
                if (accountName == null || !matchesKeyAccount(accountName)) {
                    continue;
                }
                lines.add(new DartFinancialLine(
                        bsnsYear,
                        period.code(),
                        period.label(),
                        accountName,
                        text(node, "thstrm_amount"),
                        text(node, "frmtrm_amount"),
                        text(node, "currency")
                ));
            }
            return lines;
        } catch (RestClientException ex) {
            log.warn("DART fnlttSinglAcnt 실패 corp={} year={} reprt={}: {}",
                    corpCode, bsnsYear, period.code(), ex.getMessage());
            return List.of();
        }
    }

    private static List<DartFinancialLine> dedupeLines(List<DartFinancialLine> lines) {
        Map<String, DartFinancialLine> unique = new LinkedHashMap<>();
        for (DartFinancialLine line : lines) {
            String key = line.bsnsYear() + "|" + line.reprtCode() + "|" + line.accountName();
            unique.putIfAbsent(key, line);
        }
        return List.copyOf(unique.values());
    }

    private static List<DartFinancialLine> sortLines(List<DartFinancialLine> lines) {
        return lines.stream()
                .sorted(Comparator
                        .comparing(DartFinancialLine::bsnsYear).reversed()
                        .thenComparing(line -> reportOrder(line.reprtCode()), Comparator.reverseOrder())
                        .thenComparing(DartFinancialLine::accountName))
                .toList();
    }

    private static int reportOrder(String reprtCode) {
        return REPORT_PERIODS.stream()
                .filter(p -> p.code().equals(reprtCode))
                .map(ReportPeriod::order)
                .findFirst()
                .orElse(0);
    }

    private static boolean matchesKeyAccount(String accountName) {
        String normalized = accountName.trim();
        if (KEY_ACCOUNTS.contains(normalized)) {
            return true;
        }
        return KEY_ACCOUNTS.stream().anyMatch(normalized::startsWith);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String raw = value.asText().trim();
        return raw.isEmpty() ? null : raw;
    }

    private record ReportPeriod(String code, String label, int order) {}
}
