package com.graphify.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GraphifyDartProperties.class)
public class DartClientConfig {

    @Bean
    RestClient dartRestClient() {
        return RestClient.builder()
                .defaultHeader("User-Agent", "graphify/0.1 (dart-opendart)")
                .build();
    }
}
