package com.graphify.admin.openai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record OpenAiConfigUpdateRequest(
        @NotBlank @Size(max = 512) String endpointUrl,
        @Size(max = 512) String apiKey,
        @NotBlank @Size(max = 128) String deploymentName,
        @NotBlank @Size(max = 32) String apiVersion,
        @NotBlank @Size(max = 64) String model,
        @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
        @Min(1) @Max(128000) int maxTokens,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal topP,
        @NotBlank @Size(max = 64) String embeddingModel,
        @NotBlank @Size(max = 128) String embeddingDeployment,
        @Size(max = 512) String fallbackEndpoint,
        @Size(max = 512) String fallbackApiKey,
        @Size(max = 128) String fallbackDeploymentName
) {
}
