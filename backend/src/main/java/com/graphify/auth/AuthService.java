package com.graphify.auth;

import com.graphify.auth.dto.LoginResponseDto;
import com.graphify.auth.dto.OAuthUrlDto;
import com.graphify.auth.dto.UserDto;
import com.graphify.common.exception.GraphifyException;
import com.graphify.config.GraphifyAuthProperties;
import com.graphify.user.User;
import com.graphify.user.UserAuthProvider;
import com.graphify.user.UserAuthProviderRepository;
import com.graphify.user.UserRepository;
import com.graphify.user.UserRole;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("google", "naver", "kakao");

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final OAuthStateStore oauthStateStore;
    private final GraphifyAuthProperties authProperties;

    public AuthService(
            UserRepository userRepository,
            UserAuthProviderRepository userAuthProviderRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            OAuthStateStore oauthStateStore,
            GraphifyAuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.userAuthProviderRepository = userAuthProviderRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.oauthStateStore = oauthStateStore;
        this.authProperties = authProperties;
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> invalidCredentials());

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return toLoginResponse(user, "email");
    }

    public OAuthUrlDto oauthAuthorizationUrl(String provider) {
        String normalized = normalizeProvider(provider);
        String state = oauthStateStore.issue(normalized);
        String authorizationUrl = authProperties.getApiPublicBaseUrl()
                + "/api/v1/auth/oauth/"
                + normalized
                + "/authorize?state="
                + state;
        return new OAuthUrlDto(authorizationUrl, state);
    }

    @Transactional
    public LoginResponseDto completeOAuthAuthorize(String provider, String state) {
        String normalized = normalizeProvider(provider);
        String storedProvider = oauthStateStore.consume(state)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_AUTH_002",
                        "OAuth state가 유효하지 않거나 만료되었습니다.",
                        HttpStatus.BAD_REQUEST
                ));

        if (!storedProvider.equals(normalized)) {
            throw new GraphifyException(
                    "ERR_AUTH_003",
                    "OAuth provider가 일치하지 않습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        String providerUserId = normalized + "_dev_" + state.substring(0, 8);
        User user = userAuthProviderRepository
                .findByProviderAndProviderUserId(normalized, providerUserId)
                .map(UserAuthProvider::getUser)
                .orElseGet(() -> createOAuthUser(normalized, providerUserId));

        return toLoginResponse(user, normalized);
    }

    public String buildOAuthCallbackRedirect(LoginResponseDto response) {
        UserDto user = response.user();
        return authProperties.getFrontendBaseUrl()
                + "/auth/callback#accessToken="
                + urlEncode(response.accessToken())
                + "&refreshToken="
                + urlEncode(response.refreshToken())
                + "&userId="
                + user.id()
                + "&email="
                + urlEncode(user.email())
                + "&displayName="
                + urlEncode(user.displayName())
                + "&termsAccepted="
                + user.termsAccepted()
                + "&isNewUser="
                + user.isNewUser()
                + "&authProvider="
                + urlEncode(user.authProvider())
                + "&role="
                + urlEncode(user.role());
    }

    private User createOAuthUser(String provider, String providerUserId) {
        User user = new User();
        user.setEmail(provider + "_" + providerUserId + "@oauth.graphify.dev");
        user.setDisplayName(capitalize(provider) + " 사용자");
        user.setRole(UserRole.USER);
        user.setTermsAccepted(false);
        user = userRepository.save(user);
        userAuthProviderRepository.save(UserAuthProvider.of(user, provider, providerUserId));
        return user;
    }

    private LoginResponseDto toLoginResponse(User user, String authProvider) {
        String accessToken = jwtTokenService.createAccessToken(user);
        String refreshToken = jwtTokenService.createRefreshToken(user);
        boolean isNewUser = !user.isTermsAccepted();
        UserDto userDto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isTermsAccepted(),
                isNewUser,
                authProvider,
                user.getRole().name()
        );
        return new LoginResponseDto(accessToken, refreshToken, userDto);
    }

    private static GraphifyException invalidCredentials() {
        return new GraphifyException(
                "ERR_AUTH_001",
                "이메일 또는 비밀번호가 올바르지 않습니다.",
                HttpStatus.UNAUTHORIZED
        );
    }

    private static String normalizeProvider(String provider) {
        if (provider == null) {
            throw new GraphifyException(
                    "ERR_AUTH_004",
                    "지원하지 않는 OAuth provider입니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new GraphifyException(
                    "ERR_AUTH_004",
                    "지원하지 않는 OAuth provider입니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
