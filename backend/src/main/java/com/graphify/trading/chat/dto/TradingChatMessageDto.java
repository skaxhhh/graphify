package com.graphify.trading.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 대화 맥락 유지를 위한 이전 메시지 한 턴.
 * role 은 "user" 또는 "assistant".
 */
public record TradingChatMessageDto(
        @NotBlank @Size(max = 16) String role,
        @NotBlank @Size(max = 8000) String content
) {
}
