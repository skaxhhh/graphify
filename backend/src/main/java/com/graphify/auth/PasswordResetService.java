package com.graphify.auth;

import com.graphify.auth.dto.PasswordResetConfirmResponseDto;
import com.graphify.auth.dto.PasswordResetRequestResponseDto;
import com.graphify.auth.dto.PasswordResetValidateResponseDto;
import com.graphify.common.exception.GraphifyException;
import com.graphify.config.GraphifyAuthProperties;
import com.graphify.user.User;
import com.graphify.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final GraphifyAuthProperties authProperties;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            GraphifyAuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional(readOnly = true)
    public PasswordResetValidateResponseDto validateToken(String rawToken) {
        return resolveActiveToken(rawToken)
                .map(token -> new PasswordResetValidateResponseDto(true, token.getExpiresAt()))
                .orElseGet(() -> new PasswordResetValidateResponseDto(false, null));
    }

    @Transactional
    public PasswordResetConfirmResponseDto confirmReset(String rawToken, String newPassword) {
        PasswordResetToken token = resolveActiveToken(rawToken)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_AUTH_012",
                        "재설정 링크가 만료되었거나 유효하지 않습니다.",
                        HttpStatus.BAD_REQUEST
                ));

        User user = token.getUser();
        if (user.getPasswordHash() == null) {
            throw new GraphifyException(
                    "ERR_AUTH_013",
                    "이메일 로그인 계정이 아닙니다. 소셜 로그인을 이용해 주세요.",
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(Instant.now());
        userRepository.save(user);
        passwordResetTokenRepository.save(token);

        return new PasswordResetConfirmResponseDto(
                "비밀번호가 변경되었습니다. 로그인해 주세요."
        );
    }

    private Optional<PasswordResetToken> resolveActiveToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return passwordResetTokenRepository.findByTokenHash(hashToken(rawToken.trim()))
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public PasswordResetRequestResponseDto requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String maskedEmail = maskEmail(email);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            return genericSuccess(maskedEmail);
        }

        User user = userOptional.get();
        if (user.getPasswordHash() == null) {
            return genericSuccess(maskedEmail);
        }

        Instant windowStart = Instant.now()
                .minus(authProperties.getPasswordResetRateLimitWindowMinutes(), ChronoUnit.MINUTES);
        long recentCount = passwordResetTokenRepository.countByUserIdAndCreatedAtAfter(
                user.getId(),
                windowStart
        );
        if (recentCount >= authProperties.getPasswordResetRateLimitMax()) {
            throw new GraphifyException(
                    "ERR_AUTH_011",
                    "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        String rawToken = generateRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(Instant.now().plus(
                authProperties.getPasswordResetExpirationMinutes(),
                ChronoUnit.MINUTES
        ));
        passwordResetTokenRepository.save(token);

        String resetLink = authProperties.getFrontendBaseUrl()
                + "/password-reset/confirm?token="
                + urlEncode(rawToken);
        log.info("[dev] Password reset link for {}: {}", email, resetLink);

        return new PasswordResetRequestResponseDto(
                "입력하신 이메일로 비밀번호 재설정 안내를 발송했습니다.",
                maskedEmail
        );
    }

    private static PasswordResetRequestResponseDto genericSuccess(String maskedEmail) {
        return new PasswordResetRequestResponseDto(
                "입력하신 이메일로 비밀번호 재설정 안내를 발송했습니다.",
                maskedEmail
        );
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0 || at >= email.length() - 1) {
            return "***@***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String visible = local.substring(0, 1);
        return visible + "***@" + domain;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
