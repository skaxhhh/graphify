package com.graphify.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GraphifyMarketProperties.class)
public class MarketClientConfig {

    @Bean
    RestClient yahooRestClient(GraphifyMarketProperties marketProperties) {
        return RestClient.builder()
                .baseUrl(marketProperties.getYahooChartBaseUrl())
                .defaultHeader("User-Agent", "graphify/0.1 (yahoo-chart)")
                .build();
    }

    @Bean
    RestClient naverRestClient(GraphifyMarketProperties marketProperties) {
        return RestClient.builder()
                .baseUrl(marketProperties.getNaverFinanceBaseUrl())
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; graphify/0.1)")
                .defaultHeader("Accept-Language", "ko-KR,ko;q=0.9")
                .build();
    }

    @Bean
    RestClient krxRestClient(GraphifyMarketProperties marketProperties) {
        return RestClient.builder()
                .baseUrl(marketProperties.getKrxApiBaseUrl())
                .defaultHeader("User-Agent", "graphify/0.1 (krx-openapi)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    RestClient cnnFearGreedRestClient(GraphifyMarketProperties marketProperties) {
        return RestClient.builder()
                .baseUrl(marketProperties.getCnnFearGreedBaseUrl())
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json, text/plain, */*")
                .defaultHeader("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                .defaultHeader("Referer", "https://edition.cnn.com/markets/fear-and-greed")
                .defaultHeader("Origin", "https://edition.cnn.com")
                .build();
    }
}
