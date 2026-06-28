package com.graphify.trading.chat;

import com.graphify.common.dto.ApiResponse;
import com.graphify.trading.chat.dto.TradingChatRequest;
import com.graphify.trading.chat.dto.TradingChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trading/chat")
public class TradingChatController {

    private final TradingChatService tradingChatService;

    public TradingChatController(TradingChatService tradingChatService) {
        this.tradingChatService = tradingChatService;
    }

    @PostMapping
    public ApiResponse<TradingChatResponse> chat(@Valid @RequestBody TradingChatRequest request) {
        return tradingChatService.chat(request);
    }
}
