package com.graphify.trading.paper;

import com.graphify.common.dto.ApiResponse;
import com.graphify.history.HistoryService;
import com.graphify.trading.paper.dto.PaperDashboardDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trading/paper/dashboard")
public class PaperDashboardController {

    private final PaperDashboardService dashboardService;

    public PaperDashboardController(PaperDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<PaperDashboardDto> getDashboard() {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(dashboardService.getDashboard(userId));
    }
}
