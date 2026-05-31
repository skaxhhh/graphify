package com.graphify.home.naver;

import com.graphify.config.GraphifyMarketProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NaverPopularSearchClient {

    private static final Logger log = LoggerFactory.getLogger(NaverPopularSearchClient.class);
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "<li><em>(\\d+)\\.</em><a href=\"/item/main\\.naver\\?code=(\\d{6})\"[^>]*>([^<]+)</a>"
                    + "<span class=\"(up|down|same)\">([^<]*)</span>",
            Pattern.CASE_INSENSITIVE
    );

    private final GraphifyMarketProperties marketProperties;
    private final RestClient naverRestClient;

    public NaverPopularSearchClient(GraphifyMarketProperties marketProperties, RestClient naverRestClient) {
        this.marketProperties = marketProperties;
        this.naverRestClient = naverRestClient;
    }

    public List<NaverPopularStock> fetchPopularStocks() {
        if (!marketProperties.isNaverEnabled()) {
            return List.of();
        }
        try {
            byte[] body = naverRestClient.get()
                    .uri("/sise/")
                    .retrieve()
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                return List.of();
            }
            String html = new String(body, java.nio.charset.Charset.forName("EUC-KR"));
            return parsePopularList(html);
        } catch (RestClientException ex) {
            log.warn("Naver popular search fetch failed: {}", ex.getMessage());
            return List.of();
        }
    }

    List<NaverPopularStock> parsePopularList(String html) {
        int start = html.indexOf("id=\"popularItemList\"");
        if (start < 0) {
            return List.of();
        }
        int end = html.indexOf("</ul>", start);
        if (end < 0) {
            return List.of();
        }
        String block = html.substring(start, end);
        List<NaverPopularStock> items = new ArrayList<>();
        Matcher matcher = ITEM_PATTERN.matcher(block);
        while (matcher.find()) {
            int rank = Integer.parseInt(matcher.group(1));
            String ticker = matcher.group(2);
            String name = matcher.group(3).trim();
            String direction = matcher.group(4).toLowerCase();
            String priceText = matcher.group(5).replace(",", "").trim();
            Double price = parsePrice(priceText);
            items.add(new NaverPopularStock(rank, ticker, name, price, direction));
        }
        return items;
    }

    private static Double parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record NaverPopularStock(
            int rank,
            String ticker,
            String name,
            Double price,
            String priceDirection
    ) {
    }
}
