import { apiPost } from "@/lib/apiClient";

export interface TradingChatTurn {
  role: "user" | "assistant";
  content: string;
}

export interface TradingChatRequestPayload {
  message: string;
  history?: TradingChatTurn[];
}

export interface TradingChatResponse {
  reply: string;
  model: string;
}

export async function sendTradingChat(payload: TradingChatRequestPayload) {
  return apiPost<TradingChatResponse, TradingChatRequestPayload>(
    "/api/v1/trading/chat",
    payload
  );
}
