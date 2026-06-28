package com.graphify.trading.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TradingChatRequest(
        @NotBlank @Size(max = 8000) String message,
        @Valid @Size(max = 20) List<TradingChatMessageDto> history
) {
}
