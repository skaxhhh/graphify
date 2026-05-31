package com.graphify.company.market;

import com.graphify.config.GraphifyMarketProperties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 네이버 금융 종목 메인 페이지 HTML 파싱 (비공식).
 * KRX 호가 영역({@code #rate_info_krx})의 접근성 텍스트를 우선 사용합니다.
 */
@Component
public class NaverFinanceQuoteClient {

    private static final Logger log = LoggerFactory.getLogger(NaverFinanceQuoteClient.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Pattern KRX_BLIND_BLOCK = Pattern.compile(
            "id=\"rate_info_krx\"[^>]*>.*?<dl class=\"blind\">(.*?)</dl>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PRICE_IN_BLIND = Pattern.compile("오늘의시세\\s*([\\d,]+)");
    private static final Pattern CHANGE_PCT = Pattern.compile("([\\d.]+)\\s*%");
    private static final Pattern CHANGE_AMOUNT = Pattern.compile(
            "([\\d,]+)\\s*포인트\\s*(상승|하락)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HEADER_TIME = Pattern.compile(
            "(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*(\\d{1,2})시\\s*(\\d{1,2})분"
    );

    private final GraphifyMarketProperties properties;
    private final RestClient naverRestClient;

    public NaverFinanceQuoteClient(GraphifyMarketProperties properties, RestClient naverRestClient) {
        this.properties = properties;
        this.naverRestClient = naverRestClient;
    }

    public Optional<NaverFinanceQuote> fetchQuote(String stockCode) {
        if (!properties.isNaverEnabled() || stockCode == null || stockCode.isBlank()) {
            return Optional.empty();
        }
        String code = stockCode.trim();
        try {
            String html = naverRestClient.get()
                    .uri("/item/main.naver?code={code}", code)
                    .retrieve()
                    .body(String.class);
            if (html == null || html.isBlank()) {
                return Optional.empty();
            }
            return parseHtml(html);
        } catch (RestClientException ex) {
            log.warn("Naver Finance quote fetch failed code={}: {}", code, ex.getMessage());
            return Optional.empty();
        }
    }

    Optional<NaverFinanceQuote> parseHtml(String html) {
        Matcher blindMatcher = KRX_BLIND_BLOCK.matcher(html);
        if (!blindMatcher.find()) {
            log.warn("Naver Finance: rate_info_krx blind block not found");
            return Optional.empty();
        }
        String blind = blindMatcher.group(1);
        Matcher priceMatcher = PRICE_IN_BLIND.matcher(blind);
        if (!priceMatcher.find()) {
            return Optional.empty();
        }
        double price = parseNumber(priceMatcher.group(1));
        if (price <= 0) {
            return Optional.empty();
        }

        double changePercent = 0;
        Matcher pctMatcher = CHANGE_PCT.matcher(blind);
        if (pctMatcher.find()) {
            changePercent = Double.parseDouble(pctMatcher.group(1));
            if (blind.contains("하락") || blind.contains("마이너스")) {
                changePercent = -Math.abs(changePercent);
            }
        }

        double changeAmount = 0;
        Matcher amtMatcher = CHANGE_AMOUNT.matcher(blind);
        if (amtMatcher.find()) {
            changeAmount = parseNumber(amtMatcher.group(1));
            if ("하락".equalsIgnoreCase(amtMatcher.group(2))) {
                changeAmount = -Math.abs(changeAmount);
            }
        } else if (changePercent != 0) {
            changeAmount = Math.round(price * (changePercent / 100.0));
        }

        Instant quoteTime = parseHeaderTime(html).orElse(Instant.now());
        return Optional.of(new NaverFinanceQuote(price, changePercent, changeAmount, quoteTime, "KRX"));
    }

    private static Optional<Instant> parseHeaderTime(String html) {
        Matcher matcher = HEADER_TIME.matcher(html);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        int minute = Integer.parseInt(matcher.group(5));
        LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute);
        return Optional.of(ldt.atZone(KST).toInstant());
    }

    private static double parseNumber(String raw) {
        return Double.parseDouble(raw.replace(",", "").trim());
    }
}
