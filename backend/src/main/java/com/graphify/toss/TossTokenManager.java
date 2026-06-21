package com.graphify.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphify.common.exception.GraphifyException;
import com.graphify.common.security.SecretEncryptionService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class TossTokenManager {

    private static final Logger log = LoggerFactory.getLogger(TossTokenManager.class);
    private static final String TOKEN_URL = "https://openapi.tossinvest.com/api/v1/oauth2/token";
    private static final int REFRESH_AHEAD_MINUTES = 10;

    private final TossCredentialRepository credentialRepo;
    private final TossCredentialService credentialService;
    private final SecretEncryptionService encryptionService;
    private final RestClient restClient;

    public TossTokenManager(
            TossCredentialRepository credentialRepo,
            TossCredentialService credentialService,
            SecretEncryptionService encryptionService,
            RestClient.Builder restClientBuilder) {
        this.credentialRepo = credentialRepo;
        this.credentialService = credentialService;
        this.encryptionService = encryptionService;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Returns a valid decrypted access token for the given user.
     * Issues a new token if none exists or token is expired/expiring soon.
     */
    @Transactional
    public String ensureValidToken(Long userId) {
        TossCredential cred = credentialRepo.findByUserId(userId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_TOSS_002", "토스증권 자격증명이 설정되지 않았습니다.", HttpStatus.NOT_FOUND));

        if (TossCredentialService.isTokenValid(cred)) {
            return encryptionService.decrypt(cred.getAccessTokenEncrypted());
        }

        return issueToken(cred);
    }

    /**
     * Issues a new access token by calling Toss OAuth endpoint.
     * Saves encrypted token + expiry to DB. Returns decrypted token.
     */
    @Transactional
    public String issueToken(TossCredential cred) {
        String clientId = encryptionService.decrypt(cred.getClientIdEncrypted());
        String clientSecret = encryptionService.decrypt(cred.getClientSecretEncrypted());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        TossTokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TossTokenResponse.class);
        } catch (RestClientException ex) {
            log.error("Toss token issuance failed for userId={}: {}", cred.getUserId(), ex.getMessage());
            throw new GraphifyException("ERR_TOSS_003",
                    "토스증권 토큰 발급에 실패했습니다. client_id/secret을 확인하세요.", HttpStatus.BAD_GATEWAY);
        }

        if (response == null || response.accessToken() == null) {
            throw new GraphifyException("ERR_TOSS_003",
                    "토스증권 토큰 응답이 비어 있습니다.", HttpStatus.BAD_GATEWAY);
        }

        long expiresIn = response.expiresIn() != null ? response.expiresIn() : 86400L;
        Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

        cred.setAccessTokenEncrypted(encryptionService.encrypt(response.accessToken()));
        cred.setTokenExpiresAt(expiresAt);
        credentialRepo.save(cred);

        log.info("Toss token issued for userId={}, expiresAt={}", cred.getUserId(), expiresAt);
        return response.accessToken();
    }

    /**
     * Scheduled: every 5 minutes, refresh tokens expiring within REFRESH_AHEAD_MINUTES.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void refreshExpiringSoon() {
        Instant now = Instant.now();
        Instant window = now.plus(REFRESH_AHEAD_MINUTES, ChronoUnit.MINUTES);

        List<TossCredential> expiring = credentialRepo.findExpiringSoon(now, window);
        if (expiring.isEmpty()) return;

        log.info("Refreshing {} expiring Toss token(s)", expiring.size());
        for (TossCredential cred : expiring) {
            try {
                issueToken(cred);
            } catch (Exception ex) {
                log.warn("Failed to refresh Toss token for userId={}: {}", cred.getUserId(), ex.getMessage());
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn
    ) {}
}
