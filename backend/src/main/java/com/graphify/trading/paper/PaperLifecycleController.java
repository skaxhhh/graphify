package com.graphify.trading.paper;

import com.graphify.common.dto.ApiResponse;
import com.graphify.history.HistoryService;
import com.graphify.trading.rule.dto.RuleResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trading/paper/rules")
public class PaperLifecycleController {

    private final PaperLifecycleService lifecycleService;

    public PaperLifecycleController(PaperLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    // ─── 신규 2축 엔드포인트 ────────────────────────────────────────────────────

    /** config축: DRAFT → ACTIVE */
    @PostMapping("/{id}/activate")
    public ApiResponse<RuleResponse> activate(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.activate(userId, id));
    }

    /** config축: ACTIVE/STOPPED → DRAFT (RUNNING 시 차단) */
    @PostMapping("/{id}/deactivate")
    public ApiResponse<RuleResponse> deactivate(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.deactivate(userId, id));
    }

    /** run축: ACTIVE/STOPPED → ACTIVE/RUNNING */
    @PostMapping("/{id}/start")
    public ApiResponse<RuleResponse> start(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.start(userId, id));
    }

    /** run축: ACTIVE/RUNNING → ACTIVE/STOPPED */
    @PostMapping("/{id}/stop")
    public ApiResponse<RuleResponse> stop(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.stop(userId, id));
    }

    /** 모든 상태 → DRAFT 복사본 */
    @PostMapping("/{id}/copy")
    public ApiResponse<RuleResponse> copy(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.copy(userId, id));
    }

    // ─── 기존 엔드포인트 (deprecated — 신규 엔드포인트로 이전 권장) ────────────

    /** @deprecated /activate 사용 권장 */
    @Deprecated
    @PostMapping("/{id}/promote")
    public ApiResponse<RuleResponse> promote(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.promote(userId, id));
    }

    /** @deprecated /stop 사용 권장 */
    @Deprecated
    @PostMapping("/{id}/pause")
    public ApiResponse<RuleResponse> pause(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.pause(userId, id));
    }

    /** @deprecated /start 사용 권장 */
    @Deprecated
    @PostMapping("/{id}/resume")
    public ApiResponse<RuleResponse> resume(@PathVariable Long id) {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(lifecycleService.resume(userId, id));
    }
}
