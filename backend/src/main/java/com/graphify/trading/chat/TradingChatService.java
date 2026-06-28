package com.graphify.trading.chat;

import com.graphify.agent.AzureChatCompletionClient;
import com.graphify.common.dto.ApiResponse;
import com.graphify.common.exception.GraphifyException;
import com.graphify.trading.chat.dto.TradingChatMessageDto;
import com.graphify.trading.chat.dto.TradingChatRequest;
import com.graphify.trading.chat.dto.TradingChatResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Trading 화면의 DDS Agent 챗 처리.
 * /admin/openai 에서 저장한 Azure OpenAI 설정(DB)을 사용하는
 * {@link AzureChatCompletionClient} 를 통해 LLM 응답을 생성한다.
 */
@Service
public class TradingChatService {

    private static final String SYSTEM_PROMPT = """
            당신은 'DDS Agent'입니다. 자동매매(트레이딩) 봇을 운영하는 사용자를 돕는
            한국어 어시스턴트입니다.

            역할:
            - 트레이딩 봇 상태 조회, 거래 이력 요약, 매매 룰 설명, 리포팅을 돕습니다.
            - 사용자의 질문에 간결하고 정확하게 답합니다.
            - 확실하지 않은 수치나 사실은 추측하지 말고, 모른다고 답하거나 확인 방법을 안내합니다.
            - 투자 권유나 단정적 수익 보장은 하지 않습니다. 필요한 경우 리스크를 함께 설명합니다.

            답변은 한국어로, 핵심부터 명확하게 작성하세요.
            """;

    private static final int MAX_HISTORY_TURNS = 12;

    private final AzureChatCompletionClient chatClient;

    public TradingChatService(AzureChatCompletionClient chatClient) {
        this.chatClient = chatClient;
    }

    public ApiResponse<TradingChatResponse> chat(TradingChatRequest request) {
        String userMessage = buildUserMessage(request.message(), request.history());

        AzureChatCompletionClient.CompletionResult completion = chatClient
                .complete(SYSTEM_PROMPT, userMessage)
                .orElseThrow(() -> new GraphifyException(
                        "ERR_TRADING_CHAT_001",
                        "Agent 응답을 생성하지 못했습니다. 관리자 페이지(Azure OpenAI 설정)에서 연결 정보를 확인해 주세요.",
                        HttpStatus.SERVICE_UNAVAILABLE
                ));

        return ApiResponse.ok(new TradingChatResponse(completion.content(), completion.modelLabel()));
    }

    private static String buildUserMessage(String message, List<TradingChatMessageDto> history) {
        if (history == null || history.isEmpty()) {
            return message.trim();
        }

        List<TradingChatMessageDto> recent = history.size() > MAX_HISTORY_TURNS
                ? history.subList(history.size() - MAX_HISTORY_TURNS, history.size())
                : history;

        StringBuilder sb = new StringBuilder();
        sb.append("아래는 지금까지의 대화입니다.\n\n");
        for (TradingChatMessageDto turn : recent) {
            String content = turn.content() != null ? turn.content().trim() : "";
            if (content.isEmpty()) {
                continue;
            }
            String label = "assistant".equalsIgnoreCase(turn.role()) ? "Agent" : "사용자";
            sb.append(label).append(": ").append(content).append("\n");
        }
        sb.append("\n위 맥락을 참고하여 아래 사용자의 새 질문에 답하세요.\n");
        sb.append("사용자: ").append(message.trim());
        return sb.toString();
    }
}
