package com.graphify.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GraphifyNewsProperties.class)
public class NewsClientConfig {

    @Bean
    RestClient newsRestClient() {
        return RestClient.builder()
                .defaultHeader("User-Agent", "graphify/0.1 (home-market-news)")
                .build();
    }
}
