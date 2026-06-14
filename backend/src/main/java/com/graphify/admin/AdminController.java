package com.graphify.admin;

import com.graphify.admin.dto.AdminAgentStatsDto;
import com.graphify.admin.dto.AdminCreateUserRequest;
import com.graphify.admin.dto.AdminUserDto;
import com.graphify.admin.dto.TradingAccessRequest;
import com.graphify.admin.dto.UserUsageDataDto;
import com.graphify.common.dto.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;

    public AdminController(AdminDashboardService adminDashboardService, AdminUserService adminUserService) {
        this.adminDashboardService = adminDashboardService;
        this.adminUserService = adminUserService;
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

    @GetMapping("/users")
    public ApiResponse<List<AdminUserDto>> listUsers() {
        return adminUserService.getAllUsers();
    }

    @PostMapping("/users")
    public ApiResponse<AdminUserDto> createUser(@RequestBody AdminCreateUserRequest request) {
        return adminUserService.createUser(request);
    }

    @PutMapping("/users/{id}/trading-access")
    public ApiResponse<AdminUserDto> setTradingAccess(
            @PathVariable Long id,
            @RequestBody TradingAccessRequest request
    ) {
        return adminUserService.setTradingAccess(id, request.tradingEnabled());
    }
}
