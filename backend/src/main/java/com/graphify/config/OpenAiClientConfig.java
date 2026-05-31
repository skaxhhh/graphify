package com.graphify.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphifyOpenAiProperties.class)
public class OpenAiClientConfig {}
