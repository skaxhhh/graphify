package com.graphify.bootstrap;

import com.graphify.common.dto.ApiResponse;
import com.graphify.config.GraphifyDartProperties;
import com.graphify.config.GraphifyMarketProperties;
import com.graphify.config.GraphifyNewsProperties;
import com.graphify.config.GraphifyOpenAiProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bootstrap")
public class BootstrapController {

    private final GraphifyDartProperties dartProperties;
    private final GraphifyNewsProperties newsProperties;
    private final GraphifyOpenAiProperties openAiProperties;
    private final GraphifyMarketProperties marketProperties;

    public BootstrapController(
            GraphifyDartProperties dartProperties,
            GraphifyNewsProperties newsProperties,
            GraphifyOpenAiProperties openAiProperties,
            GraphifyMarketProperties marketProperties
    ) {
        this.dartProperties = dartProperties;
        this.newsProperties = newsProperties;
        this.openAiProperties = openAiProperties;
        this.marketProperties = marketProperties;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, String>> status() {
        return ApiResponse.ok(Map.of(
                "service", "graphify-api",
                "phase", "T01_BOOT"
        ));
    }

    @GetMapping("/integrations")
    public ApiResponse<Map<String, Object>> integrations() {
        Map<String, Object> integrations = new LinkedHashMap<>();
        integrations.put("dart", Map.of(
                "configured", dartProperties.hasApiKey(),
                "enrichThreshold", dartProperties.getEnrichThreshold()
        ));
        integrations.put("newsApi", Map.of(
                "configured", newsProperties.hasNewsApiKey()
        ));
        integrations.put("openAi", Map.of(
                "configured", openAiProperties.isConfigured(),
                "authMode", openAiProperties.getAuthMode(),
                "deployment", openAiProperties.resolveDeployment(),
                "model", openAiProperties.getModel()
        ));
        integrations.put("yahooMarket", Map.of(
                "enabled", marketProperties.isYahooEnabled(),
                "provider", "yahoo-finance-chart"
        ));
        return ApiResponse.ok(integrations);
    }
}
