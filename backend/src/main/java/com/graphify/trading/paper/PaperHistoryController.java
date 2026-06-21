package com.graphify.trading.paper;

import com.graphify.common.dto.ApiResponse;
import com.graphify.history.HistoryService;
import com.graphify.trading.paper.dto.PaperTradeHistoryItem;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trading/paper/history")
public class PaperHistoryController {

    private final PaperHistoryService historyService;

    public PaperHistoryController(PaperHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ApiResponse<List<PaperTradeHistoryItem>> getHistory() {
        Long userId = HistoryService.requireCurrentUserId();
        return ApiResponse.ok(historyService.getHistory(userId));
    }
}
