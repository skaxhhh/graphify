package com.graphify.toss;

import com.graphify.common.dto.ApiResponse;
import com.graphify.history.HistoryService;
import com.graphify.toss.dto.TossCredentialRequest;
import com.graphify.toss.dto.TossCredentialStatusDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/toss/credentials")
public class TossCredentialController {

    private final TossCredentialService credentialService;
    private final TossTokenManager tokenManager;

    public TossCredentialController(
            TossCredentialService credentialService,
            TossTokenManager tokenManager) {
        this.credentialService = credentialService;
        this.tokenManager = tokenManager;
    }

    /** GET /api/v1/toss/credentials/status */
    @GetMapping("/status")
    public ApiResponse<TossCredentialStatusDto> getStatus() {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(credentialService.getStatus(userId));
    }

    /** POST /api/v1/toss/credentials — save client_id + client_secret */
    @PostMapping
    public ApiResponse<TossCredentialStatusDto> saveCredentials(
            @RequestBody TossCredentialRequest request) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(credentialService.saveCredentials(userId, request));
    }

    /** POST /api/v1/toss/credentials/token/refresh — manually trigger token issue */
    @PostMapping("/token/refresh")
    public ApiResponse<TossCredentialStatusDto> refreshToken() {
        Long userId = HistoryService.requireCurrentUserId();
        tokenManager.ensureValidToken(userId);  // forces refresh if needed
        return ApiResponse.ok(credentialService.getStatus(userId));
    }
}
