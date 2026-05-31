package com.graphify.auth;

import com.graphify.auth.dto.ConsentRequestDto;
import com.graphify.auth.dto.ConsentResponseDto;
import com.graphify.auth.dto.LoginRequestDto;
import com.graphify.auth.dto.LoginResponseDto;
import com.graphify.auth.dto.OAuthUrlDto;
import com.graphify.auth.dto.PasswordResetConfirmRequestDto;
import com.graphify.auth.dto.PasswordResetConfirmResponseDto;
import com.graphify.auth.dto.PasswordResetRequestDto;
import com.graphify.auth.dto.PasswordResetRequestResponseDto;
import com.graphify.auth.dto.PasswordResetValidateResponseDto;
import com.graphify.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final ConsentService consentService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            ConsentService consentService,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.consentService = consentService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request.email(), request.password());
        return ApiResponse.ok(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }

    @GetMapping("/oauth/{provider}/url")
    public ApiResponse<OAuthUrlDto> oauthUrl(@PathVariable String provider) {
        return ApiResponse.ok(authService.oauthAuthorizationUrl(provider));
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<PasswordResetRequestResponseDto> passwordResetRequest(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        return ApiResponse.ok(passwordResetService.requestReset(request.email()));
    }

    @GetMapping("/password-reset/validate")
    public ApiResponse<PasswordResetValidateResponseDto> passwordResetValidate(
            @RequestParam String token
    ) {
        return ApiResponse.ok(passwordResetService.validateToken(token));
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<PasswordResetConfirmResponseDto> passwordResetConfirm(
            @Valid @RequestBody PasswordResetConfirmRequestDto request
    ) {
        return ApiResponse.ok(
                passwordResetService.confirmReset(request.token(), request.newPassword())
        );
    }

    @PostMapping("/consent")
    public ApiResponse<ConsentResponseDto> consent(
            @Valid @RequestBody ConsentRequestDto request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(consentService.submitConsent(userId, request));
    }

    @GetMapping("/oauth/{provider}/authorize")
    public void oauthAuthorize(
            @PathVariable String provider,
            @RequestParam String state,
            HttpServletResponse response
    ) throws IOException {
        LoginResponseDto loginResponse = authService.completeOAuthAuthorize(provider, state);
        response.sendRedirect(authService.buildOAuthCallbackRedirect(loginResponse));
    }
}
