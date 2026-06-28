package com.graphify.admin.openai;

import com.graphify.admin.openai.dto.OpenAiConfigDto;
import com.graphify.admin.openai.dto.OpenAiConfigUpdateRequest;
import com.graphify.admin.openai.dto.OpenAiStatusDto;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.common.security.SecretEncryptionService;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OpenAiSettingsService {

    private static final Set<String> MODELS = Set.of(
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4.1",
            "gpt-4.1-mini"
    );

    private final OpenAiSettingsRepository openAiSettingsRepository;
    private final SecretEncryptionService secretEncryptionService;

    public OpenAiSettingsService(
            OpenAiSettingsRepository openAiSettingsRepository,
            SecretEncryptionService secretEncryptionService
    ) {
        this.openAiSettingsRepository = openAiSettingsRepository;
        this.secretEncryptionService = secretEncryptionService;
    }

    @Transactional(readOnly = true)
    public ApiResponse<OpenAiConfigDto> getConfig() {
        return ApiResponse.ok(toConfigDto(requireSettings()));
    }

    public ApiResponse<OpenAiConfigDto> updateConfig(OpenAiConfigUpdateRequest request) {
        validateConfig(request);
        OpenAiSettings settings = requireSettings();

        settings.setEndpointUrl(request.endpointUrl().trim());
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            settings.setApiKeyEncrypted(secretEncryptionService.encrypt(request.apiKey().trim()));
        }
        settings.setDeploymentName(request.deploymentName().trim());
        settings.setApiVersion(request.apiVersion().trim());
        settings.setModel(normalizeModel(request.model()));
        settings.setTemperature(request.temperature());
        settings.setMaxTokens(request.maxTokens());
        settings.setTopP(request.topP());
        settings.setEmbeddingModel(request.embeddingModel().trim());
        settings.setEmbeddingDeployment(request.embeddingDeployment().trim());

        String fallbackEndpoint = trimToNull(request.fallbackEndpoint());
        settings.setFallbackEndpointUrl(fallbackEndpoint);
        settings.setFallbackDeploymentName(trimToNull(request.fallbackDeploymentName()));
        if (request.fallbackApiKey() != null && !request.fallbackApiKey().isBlank()) {
            settings.setFallbackApiKeyEncrypted(
                    secretEncryptionService.encrypt(request.fallbackApiKey().trim())
            );
        } else if (fallbackEndpoint == null) {
            settings.setFallbackApiKeyEncrypted(null);
        }

        openAiSettingsRepository.save(settings);
        return ApiResponse.ok(toConfigDto(settings));
    }

    @Transactional(readOnly = true)
    public ApiResponse<OpenAiStatusDto> getStatus() {
        OpenAiSettings settings = requireSettings();
        StatusEvaluation evaluation = evaluateConnection(settings);
        return ApiResponse.ok(new OpenAiStatusDto(
                evaluation.connection(),
                settings.getTokensUsed(),
                settings.getRateLimitRemaining() != null
                        ? settings.getRateLimitRemaining()
                        : 100_000,
                settings.getLastCheckedAt(),
                evaluation.message()
        ));
    }

    public ApiResponse<OpenAiStatusDto> refreshStatus() {
        OpenAiSettings settings = requireSettings();
        StatusEvaluation evaluation = evaluateConnection(settings);
        settings.setLastStatus(evaluation.connection());
        settings.setLastCheckedAt(Instant.now());
        if (settings.getRateLimitRemaining() == null) {
            settings.setRateLimitRemaining(100_000);
        }
        if ("OK".equals(evaluation.connection())) {
            settings.setRateLimitRemaining(Math.max(0, settings.getRateLimitRemaining() - 1));
        }
        openAiSettingsRepository.save(settings);
        return ApiResponse.ok(new OpenAiStatusDto(
                evaluation.connection(),
                settings.getTokensUsed(),
                settings.getRateLimitRemaining(),
                settings.getLastCheckedAt(),
                evaluation.message()
        ));
    }

    private OpenAiSettings requireSettings() {
        return openAiSettingsRepository.findById(OpenAiSettings.SINGLETON_ID)
                .orElseGet(this::createDefaultSettings);
    }

    private OpenAiSettings createDefaultSettings() {
        OpenAiSettings settings = new OpenAiSettings();
        settings.setLastStatus("NOT_CONFIGURED");
        return openAiSettingsRepository.save(settings);
    }

    private OpenAiConfigDto toConfigDto(OpenAiSettings settings) {
        boolean hasApiKey = settings.getApiKeyEncrypted() != null && !settings.getApiKeyEncrypted().isBlank();
        boolean hasFallbackApiKey = settings.getFallbackApiKeyEncrypted() != null
                && !settings.getFallbackApiKeyEncrypted().isBlank();
        boolean configured = !settings.getEndpointUrl().isBlank() && hasApiKey;

        return new OpenAiConfigDto(
                configured,
                settings.getEndpointUrl(),
                settings.getDeploymentName(),
                settings.getApiVersion(),
                settings.getModel(),
                settings.getTemperature(),
                settings.getMaxTokens(),
                settings.getTopP(),
                settings.getEmbeddingModel(),
                settings.getEmbeddingDeployment(),
                settings.getFallbackEndpointUrl(),
                settings.getFallbackDeploymentName(),
                hasApiKey,
                hasFallbackApiKey
        );
    }

    private StatusEvaluation evaluateConnection(OpenAiSettings settings) {
        if (settings.getEndpointUrl() == null || settings.getEndpointUrl().isBlank()) {
            return new StatusEvaluation("NOT_CONFIGURED", "엔드포인트 URL을 설정하세요.");
        }
        if (settings.getApiKeyEncrypted() == null || settings.getApiKeyEncrypted().isBlank()) {
            return new StatusEvaluation("NOT_CONFIGURED", "API 키를 설정하세요.");
        }
        try {
            URI uri = URI.create(settings.getEndpointUrl().trim());
            String host = uri.getHost();
            if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("https")) {
                return new StatusEvaluation("ERROR", "https 엔드포인트만 지원합니다.");
            }
            if (host == null
                    || (!host.contains("openai.azure.com")
                        && !host.contains("cognitiveservices.azure.com")
                        && !host.contains("ai-talentlab.com"))) {
                return new StatusEvaluation("ERROR", "Azure OpenAI 호스트 형식이 아닙니다.");
            }
            secretEncryptionService.decrypt(settings.getApiKeyEncrypted());
            return new StatusEvaluation("OK", "연결 설정이 유효합니다. (dev: 형식 검증만 수행)");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return new StatusEvaluation("ERROR", "설정 값을 확인하세요.");
        }
    }

    private void validateConfig(OpenAiConfigUpdateRequest request) {
        try {
            URI uri = URI.create(request.endpointUrl().trim());
            if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("https")) {
                throw badRequest("endpointUrl은 https로 시작해야 합니다.");
            }
        } catch (IllegalArgumentException ex) {
            throw badRequest("endpointUrl 형식이 올바르지 않습니다.");
        }

        String fallback = trimToNull(request.fallbackEndpoint());
        if (fallback != null) {
            try {
                URI fb = URI.create(fallback);
                if (fb.getScheme() == null || !fb.getScheme().equalsIgnoreCase("https")) {
                    throw badRequest("fallbackEndpoint는 https여야 합니다.");
                }
            } catch (IllegalArgumentException ex) {
                throw badRequest("fallbackEndpoint 형식이 올바르지 않습니다.");
            }
        }

        if (request.apiKey() == null || request.apiKey().isBlank()) {
            OpenAiSettings current = requireSettings();
            if (current.getApiKeyEncrypted() == null || current.getApiKeyEncrypted().isBlank()) {
                throw badRequest("최초 저장 시 apiKey는 필수입니다.");
            }
        }
    }

    private static String normalizeModel(String model) {
        String normalized = model.trim().toLowerCase(Locale.ROOT);
        if (!MODELS.contains(normalized)) {
            throw new GraphifyException(
                    "ERR_ADMIN_OPENAI_002",
                    "model은 gpt-4o, gpt-4o-mini, gpt-4.1, gpt-4.1-mini 중 하나여야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static GraphifyException badRequest(String message) {
        return new GraphifyException("ERR_ADMIN_OPENAI_002", message, HttpStatus.BAD_REQUEST);
    }

    private record StatusEvaluation(String connection, String message) {
    }
}
