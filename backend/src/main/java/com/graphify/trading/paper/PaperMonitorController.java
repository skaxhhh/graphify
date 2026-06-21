package com.graphify.trading.paper;

import com.graphify.common.dto.ApiResponse;
import com.graphify.history.HistoryService;
import com.graphify.trading.paper.dto.MonitorDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trading/paper/monitor")
public class PaperMonitorController {

    private final PaperMonitorService monitorService;

    public PaperMonitorController(PaperMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    public ApiResponse<MonitorDto> getMonitor() {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(monitorService.getMonitor(userId));
    }
}
