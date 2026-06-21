package com.graphify.toss;

import com.graphify.common.exception.GraphifyException;
import com.graphify.common.security.SecretEncryptionService;
import com.graphify.toss.dto.TossCredentialRequest;
import com.graphify.toss.dto.TossCredentialStatusDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TossCredentialService {

    private final TossCredentialRepository credentialRepo;
    private final SecretEncryptionService encryptionService;

    public TossCredentialService(
            TossCredentialRepository credentialRepo,
            SecretEncryptionService encryptionService) {
        this.credentialRepo = credentialRepo;
        this.encryptionService = encryptionService;
    }

    /** Save (upsert) encrypted credentials; clears existing token to force re-issue. */
    public TossCredentialStatusDto saveCredentials(Long userId, TossCredentialRequest request) {
        if (request.clientId() == null || request.clientId().isBlank()) {
            throw new GraphifyException("ERR_TOSS_001", "client_id는 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (request.clientSecret() == null || request.clientSecret().isBlank()) {
            throw new GraphifyException("ERR_TOSS_001", "client_secret은 필수입니다.", HttpStatus.BAD_REQUEST);
        }

        TossCredential cred = credentialRepo.findByUserId(userId)
                .orElseGet(() -> new TossCredential(
                        userId,
                        encryptionService.encrypt(request.clientId().trim()),
                        encryptionService.encrypt(request.clientSecret().trim())
                ));

        cred.setClientIdEncrypted(encryptionService.encrypt(request.clientId().trim()));
        cred.setClientSecretEncrypted(encryptionService.encrypt(request.clientSecret().trim()));
        // Clear token so it will be re-issued on next ensureValidToken call
        cred.setAccessTokenEncrypted(null);
        cred.setTokenExpiresAt(null);

        credentialRepo.save(cred);
        return getStatus(userId);
    }

    @Transactional(readOnly = true)
    public TossCredentialStatusDto getStatus(Long userId) {
        return credentialRepo.findByUserId(userId)
                .map(cred -> new TossCredentialStatusDto(
                        true,
                        isTokenValid(cred),
                        cred.getTokenExpiresAt()
                ))
                .orElse(new TossCredentialStatusDto(false, false, null));
    }

    @Transactional(readOnly = true)
    public String getDecryptedClientId(Long userId) {
        return encryptionService.decrypt(findOrThrow(userId).getClientIdEncrypted());
    }

    @Transactional(readOnly = true)
    public String getDecryptedClientSecret(Long userId) {
        return encryptionService.decrypt(findOrThrow(userId).getClientSecretEncrypted());
    }

    private TossCredential findOrThrow(Long userId) {
        return credentialRepo.findByUserId(userId)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_TOSS_002", "토스증권 자격증명이 설정되지 않았습니다.", HttpStatus.NOT_FOUND));
    }

    /** Token is valid if it has a non-null token and expires more than 1 minute from now. */
    static boolean isTokenValid(TossCredential cred) {
        return cred.getAccessTokenEncrypted() != null
                && cred.getTokenExpiresAt() != null
                && cred.getTokenExpiresAt().isAfter(Instant.now().plus(1, ChronoUnit.MINUTES));
    }
}
