package com.graphify.admin.openai.dto;

import java.math.BigDecimal;

public record OpenAiConfigDto(
        boolean configured,
        String endpointUrl,
        String deploymentName,
        String apiVersion,
        String model,
        BigDecimal temperature,
        int maxTokens,
        BigDecimal topP,
        String embeddingModel,
        String embeddingDeployment,
        String fallbackEndpoint,
        String fallbackDeploymentName,
        boolean hasApiKey,
        boolean hasFallbackApiKey
) {
}
