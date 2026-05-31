package com.graphify.company.registry.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.company.registry.CompanyRegistryClient;
import com.graphify.company.registry.ExternalCompanyCandidate;
import com.graphify.company.registry.ExternalCompanyProfile;
import com.graphify.config.GraphifyDartProperties;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DartCompanyRegistryClient implements CompanyRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(DartCompanyRegistryClient.class);
    private static final String SOURCE = "DART";
    private static final String COMPANY_URL = "https://opendart.fss.or.kr/api/company.json";

    private final GraphifyDartProperties dartProperties;
    private final RestClient dartRestClient;
    private final DartCorpCodeIndex corpCodeIndex;

    public DartCompanyRegistryClient(
            GraphifyDartProperties dartProperties,
            RestClient dartRestClient,
            DartCorpCodeIndex corpCodeIndex
    ) {
        this.dartProperties = dartProperties;
        this.dartRestClient = dartRestClient;
        this.corpCodeIndex = corpCodeIndex;
    }

    @Override
    public List<ExternalCompanyCandidate> searchByKeyword(String query, int limit) {
        if (!dartProperties.hasApiKey()) {
            return List.of();
        }
        int max = limit > 0 ? limit : dartProperties.getSearchMaxResults();
        return corpCodeIndex.search(query, max).stream()
                .map(this::toCandidate)
                .toList();
    }

    @Override
    public Optional<ExternalCompanyProfile> findByTicker(String ticker) {
        if (!dartProperties.hasApiKey() || ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        String normalizedTicker = ticker.trim();
        return corpCodeIndex.search(normalizedTicker, 5).stream()
                .filter(entry -> normalizedTicker.equals(entry.stockCode()))
                .findFirst()
                .flatMap(entry -> fetchProfile(entry.corpCode()));
    }

    @Override
    public Optional<ExternalCompanyProfile> findByExternalId(String source, String externalId) {
        if (!dartProperties.hasApiKey()) {
            return Optional.empty();
        }
        if (!SOURCE.equalsIgnoreCase(source) || externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        return fetchProfile(externalId.trim());
    }

    public Optional<ExternalCompanyProfile> fetchProfile(String corpCode) {
        return fetchCompanyJson(corpCode).flatMap(root -> mapProfile(root, corpCode));
    }

    public Optional<JsonNode> fetchCompanyJson(String corpCode) {
        if (!dartProperties.hasApiKey() || corpCode == null || corpCode.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = dartRestClient.get()
                    .uri(COMPANY_URL + "?crtfc_key={key}&corp_code={corp}", dartProperties.getApiKey().trim(), corpCode.trim())
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || !"000".equals(root.path("status").asText())) {
                log.warn("DART company.json 실패 corp_code={} status={}", corpCode, root != null ? root.path("status").asText() : "null");
                return Optional.empty();
            }
            return Optional.of(root);
        } catch (RestClientException ex) {
            log.warn("DART company.json 호출 실패 corp_code={}: {}", corpCode, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ExternalCompanyProfile> mapProfile(JsonNode root, String corpCode) {
        if (root == null || root.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(mapProfileInternal(root, corpCode));
    }

    private ExternalCompanyCandidate toCandidate(DartCorpCodeEntry entry) {
        String ticker = entry.stockCode() == null || entry.stockCode().isBlank() ? null : entry.stockCode().trim();
        return new ExternalCompanyCandidate(
                SOURCE,
                entry.corpCode(),
                entry.corpName(),
                ticker,
                null,
                entry.isListed()
        );
    }

    private ExternalCompanyProfile mapProfileInternal(JsonNode root, String corpCode) {
        String name = text(root, "corp_name");
        String ticker = text(root, "stock_code");
        String industryCode = text(root, "induty_code");
        String market = mapMarket(text(root, "corp_cls"));
        boolean listed = ticker != null && !ticker.isBlank();
        String ceo = text(root, "ceo_nm");
        String homepage = text(root, "hm_url");
        String summary = buildSummary(name, ceo, homepage, industryCode);

        return new ExternalCompanyProfile(
                SOURCE,
                corpCode,
                name != null ? name : "",
                ticker,
                industryCode,
                market,
                listed,
                summary
        );
    }

    private static String buildSummary(String name, String ceo, String homepage, String industryCode) {
        StringBuilder builder = new StringBuilder();
        if (name != null && !name.isBlank()) {
            builder.append(name.trim());
        }
        if (ceo != null && !ceo.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" · ");
            }
            builder.append("대표 ").append(ceo.trim());
        }
        if (industryCode != null && !industryCode.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" · ");
            }
            builder.append("업종코드 ").append(industryCode.trim());
        }
        if (homepage != null && !homepage.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" · ");
            }
            builder.append(homepage.trim());
        }
        return builder.isEmpty() ? "" : builder.toString();
    }

    private static String mapMarket(String corpCls) {
        if (corpCls == null || corpCls.isBlank()) {
            return null;
        }
        return switch (corpCls.trim().toUpperCase(Locale.ROOT)) {
            case "Y" -> "KOSPI";
            case "K" -> "KOSDAQ";
            case "N" -> "KONEX";
            case "E" -> "ETC";
            default -> corpCls;
        };
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
