package com.graphify.history;

import com.graphify.common.dto.ApiResponse;
import com.graphify.history.dto.HistoryListDataDto;
import com.graphify.history.dto.HistoryDetailDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/me")
    public ApiResponse<HistoryListDataDto> myHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to
    ) {
        return historyService.getMyHistory(page, size, q, from, to);
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<HistoryDetailDto> sessionDetail(@PathVariable String sessionId) {
        return historyService.getSessionDetail(sessionId);
    }
}
