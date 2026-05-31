package com.graphify.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphify.admin.openai.OpenAiSettings;
import com.graphify.admin.openai.OpenAiSettingsRepository;
import com.graphify.common.security.SecretEncryptionService;
import com.graphify.config.GraphifyOpenAiProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AzureChatCompletionClient {

    private static final Logger log = LoggerFactory.getLogger(AzureChatCompletionClient.class);

    private final GraphifyOpenAiProperties envOpenAiProperties;
    private final OpenAiSettingsRepository openAiSettingsRepository;
    private final SecretEncryptionService secretEncryptionService;
    private final RestClient restClient;

    public AzureChatCompletionClient(
            GraphifyOpenAiProperties envOpenAiProperties,
            OpenAiSettingsRepository openAiSettingsRepository,
            SecretEncryptionService secretEncryptionService,
            RestClient.Builder restClientBuilder
    ) {
        this.envOpenAiProperties = envOpenAiProperties;
        this.openAiSettingsRepository = openAiSettingsRepository;
        this.secretEncryptionService = secretEncryptionService;
        this.restClient = restClientBuilder.build();
    }

    public Optional<CompletionResult> complete(String systemPrompt, String userMessage) {
        for (ChatRuntimeConfig config : resolveConfigCandidates()) {
            Optional<CompletionResult> result = callCompletion(config, systemPrompt, userMessage);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private List<ChatRuntimeConfig> resolveConfigCandidates() {
        if (envOpenAiProperties.isConfigured()) {
            Set<String> deployments = new LinkedHashSet<>();
            deployments.add(envOpenAiProperties.resolveDeployment());
            if (envOpenAiProperties.getFallbackDeployment() != null
                    && !envOpenAiProperties.getFallbackDeployment().isBlank()) {
                deployments.add(envOpenAiProperties.getFallbackDeployment().trim());
            }
            deployments.add("gpt-4o");
            deployments.add("gpt-4o-mini");

            List<ChatRuntimeConfig> configs = new ArrayList<>();
            for (String deployment : deployments) {
                if (deployment == null || deployment.isBlank()) {
                    continue;
                }
                configs.add(ChatRuntimeConfig.fromEnv(envOpenAiProperties, deployment));
            }
            return configs;
        }

        OpenAiSettings settings = openAiSettingsRepository.findById(OpenAiSettings.SINGLETON_ID).orElse(null);
        if (settings == null
                || settings.getApiKeyEncrypted() == null
                || settings.getApiKeyEncrypted().isBlank()
                || settings.getEndpointUrl() == null
                || settings.getEndpointUrl().isBlank()
                || settings.getDeploymentName() == null
                || settings.getDeploymentName().isBlank()) {
            return List.of();
        }
        try {
            String apiKey = secretEncryptionService.decrypt(settings.getApiKeyEncrypted());
            return List.of(ChatRuntimeConfig.fromAdmin(settings, apiKey));
        } catch (RuntimeException ex) {
            log.warn("Azure API 키 복호화 실패: {}", ex.getMessage());
            return List.of();
        }
    }

    private Optional<CompletionResult> callCompletion(
            ChatRuntimeConfig config,
            String systemPrompt,
            String userMessage
    ) {
        Map<String, Object> body = Map.of(
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", config.temperature(),
                "max_tokens", config.maxTokens(),
                "top_p", config.topP()
        );

        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(config.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
            if (config.bearerAuth()) {
                request = request.header("Authorization", "Bearer " + config.apiKey());
            } else {
                request = request.header("api-key", config.apiKey());
            }

            JsonNode response = request.retrieve().body(JsonNode.class);
            if (response == null) {
                return Optional.empty();
            }
            String content = response.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                log.warn("Chat completion 빈 응답 deployment={}", config.deploymentLabel());
                return Optional.empty();
            }
            return Optional.of(new CompletionResult(content.trim(), config.deploymentLabel()));
        } catch (RestClientResponseException ex) {
            log.warn("Chat completion HTTP {} deployment={} body={}",
                    ex.getStatusCode().value(),
                    config.deploymentLabel(),
                    truncate(ex.getResponseBodyAsString(), 200));
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Chat completion 실패 deployment={}: {}", config.deploymentLabel(), ex.getMessage());
            return Optional.empty();
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "...";
    }

    private record ChatRuntimeConfig(
            String url,
            String apiKey,
            boolean bearerAuth,
            String deploymentLabel,
            BigDecimal temperature,
            int maxTokens,
            BigDecimal topP
    ) {
        static ChatRuntimeConfig fromEnv(GraphifyOpenAiProperties props, String deployment) {
            String base = props.getBaseUrl().trim().replaceAll("/+$", "");
            String apiVersion = props.getApiVersion() != null ? props.getApiVersion().trim() : "2024-12-01-preview";
            String url = base + "/openai/deployments/" + deployment.trim()
                    + "/chat/completions?api-version=" + apiVersion;
            return new ChatRuntimeConfig(
                    url,
                    props.getApiKey().trim(),
                    props.usesBearerAuth(),
                    deployment.trim(),
                    new BigDecimal("0.30"),
                    Math.min(Math.max(props.getMaxTokens(), 256), 4096),
                    new BigDecimal("1.00")
            );
        }

        static ChatRuntimeConfig fromAdmin(OpenAiSettings settings, String apiKey) {
            String base = settings.getEndpointUrl().trim().replaceAll("/+$", "");
            String deployment = settings.getDeploymentName().trim();
            String apiVersion = settings.getApiVersion() != null ? settings.getApiVersion().trim() : "2024-02-15-preview";
            String url = base + "/openai/deployments/" + deployment + "/chat/completions?api-version=" + apiVersion;
            String model = settings.getModel() != null ? settings.getModel() : deployment;
            return new ChatRuntimeConfig(
                    url,
                    apiKey,
                    false,
                    model,
                    settings.getTemperature() != null ? settings.getTemperature() : new BigDecimal("0.30"),
                    Math.min(Math.max(settings.getMaxTokens(), 256), 4096),
                    settings.getTopP() != null ? settings.getTopP() : new BigDecimal("1.00")
            );
        }
    }

    public record CompletionResult(String content, String modelLabel) {}
}
