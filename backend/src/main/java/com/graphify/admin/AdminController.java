package com.graphify.admin;

import com.graphify.admin.dto.AdminAgentStatsDto;
import com.graphify.admin.dto.UserUsageDataDto;
import com.graphify.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    public AdminController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/agent/stats")
    public ApiResponse<AdminAgentStatsDto> agentStats(
            @RequestParam(value = "period", defaultValue = "day") String period
    ) {
        return adminDashboardService.getAgentStats(period);
    }

    @GetMapping("/users/usage")
    public ApiResponse<UserUsageDataDto> userUsage() {
        return adminDashboardService.getUserUsage();
    }
}
