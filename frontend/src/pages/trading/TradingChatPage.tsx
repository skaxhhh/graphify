import { useEffect, useRef, useState } from "react";
import { TradeButton } from "@/components/trading/ui";
import { ApiRequestError } from "@/lib/apiClient";
import { sendTradingChat, type TradingChatTurn } from "@/lib/tradingChatApi";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  createdAt: Date;
}

function TypingIndicator() {
  return (
    <div className="flex items-end gap-2">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-trade-elevated font-trade-mono text-xs font-bold text-trade-primary">
        D
      </div>
      <div className="flex items-center gap-1 rounded-2xl rounded-bl-sm bg-trade-surface px-4 py-3">
        <span className="h-2 w-2 animate-bounce rounded-full bg-trade-muted [animation-delay:-0.3s]" />
        <span className="h-2 w-2 animate-bounce rounded-full bg-trade-muted [animation-delay:-0.15s]" />
        <span className="h-2 w-2 animate-bounce rounded-full bg-trade-muted" />
      </div>
    </div>
  );
}

function ChatMessage({ message }: { message: Message }) {
  const isUser = message.role === "user";
  const ts = message.createdAt.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
  return (
    <div className={`flex items-end gap-2 ${isUser ? "flex-row-reverse" : ""}`}>
      {!isUser ? (
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-trade-elevated font-trade-mono text-xs font-bold text-trade-primary">
          D
        </div>
      ) : (
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-trade-elevated font-trade-mono text-xs font-bold text-trade-muted-strong">
          나
        </div>
      )}
      <div className={`flex flex-col gap-1 ${isUser ? "items-end" : "items-start"}`}>
        <div
          className={`max-w-[70%] whitespace-pre-wrap rounded-2xl px-4 py-3 text-sm leading-relaxed font-trade-sans text-trade-body ${
            isUser ? "rounded-br-sm bg-trade-elevated" : "rounded-bl-sm bg-trade-surface"
          }`}
        >
          {message.content}
        </div>
        <span className="text-xs font-trade-mono text-trade-muted">{ts}</span>
      </div>
    </div>
  );
}

const WELCOME_MESSAGE: Message = {
  id: "welcome",
  role: "assistant",
  content:
    "안녕하세요! DDS Agent입니다.\n\n현재 트레이딩 봇의 상태 조회, 거래 이력 요약, 룰 설명, 리포팅 등을 도와드릴 수 있습니다.\n\n무엇이 궁금하신가요?",
  createdAt: new Date(),
};

export function TradingChatPage() {
  const [messages, setMessages] = useState<Message[]>([WELCOME_MESSAGE]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || isTyping) return;

    const userMsg: Message = {
      id: crypto.randomUUID(),
      role: "user",
      content: trimmed,
      createdAt: new Date(),
    };

    // 직전 대화 맥락 (welcome 안내 메시지는 제외)
    const history: TradingChatTurn[] = messages
      .filter((m) => m.id !== "welcome")
      .map((m) => ({ role: m.role, content: m.content }));

    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsTyping(true);

    try {
      const res = await sendTradingChat({ message: trimmed, history });
      const reply = res.data?.reply?.trim();
      const assistantMsg: Message = {
        id: crypto.randomUUID(),
        role: "assistant",
        content:
          reply && reply.length > 0
            ? reply
            : "응답을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        createdAt: new Date(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err) {
      const message =
        err instanceof ApiRequestError
          ? err.message
          : "Agent 연결에 실패했습니다. 잠시 후 다시 시도해 주세요.";
      const errorMsg: Message = {
        id: crypto.randomUUID(),
        role: "assistant",
        content: message,
        createdAt: new Date(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsTyping(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <div className="flex h-[calc(100vh-3.5rem-2rem)] flex-col">
      {/* 헤더 */}
      <div className="mb-4 flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-trade-elevated">
          <span className="font-trade-mono text-sm font-bold text-trade-primary">D</span>
        </div>
        <div>
          <p className="font-trade-sans font-semibold text-trade-on-dark">DDS Agent</p>
          <p className="font-trade-sans text-xs text-trade-muted">모니터링 · 설명 · 리포팅</p>
        </div>
        <span className="ml-auto flex items-center gap-1.5 font-trade-sans text-xs text-trade-up">
          <span className="h-2 w-2 rounded-full bg-trade-up" />
          대기 중
        </span>
        <span className="rounded border border-trade-hairline bg-trade-surface px-2 py-0.5 font-trade-mono text-xs text-trade-muted">
          STATE
        </span>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 space-y-4 overflow-y-auto rounded-xl border border-trade-hairline bg-trade-surface p-4">
        {messages.map((msg) => (
          <ChatMessage key={msg.id} message={msg} />
        ))}
        {isTyping ? <TypingIndicator /> : null}
        <div ref={bottomRef} />
      </div>

      {/* 입력 영역 */}
      <form onSubmit={handleSubmit} className="mt-3 flex items-end gap-2">
        <textarea
          ref={inputRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="메시지를 입력하세요... (Enter로 전송, Shift+Enter 줄바꿈)"
          rows={1}
          className="flex-1 resize-none rounded-xl border border-trade-hairline bg-trade-surface px-4 py-3 font-trade-sans text-sm text-trade-body placeholder:text-trade-muted focus:outline-none focus:ring-2 focus:ring-trade-info"
          style={{ maxHeight: "8rem", overflowY: "auto" }}
          onInput={(e) => {
            const el = e.currentTarget;
            el.style.height = "auto";
            el.style.height = `${Math.min(el.scrollHeight, 128)}px`;
          }}
        />
        <TradeButton
          type="submit"
          variant="primary"
          disabled={!input.trim() || isTyping}
          className="h-11 w-11 shrink-0 rounded-xl px-0"
        >
          <svg className="h-4 w-4 rotate-90" fill="currentColor" viewBox="0 0 24 24">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
          </svg>
        </TradeButton>
      </form>
    </div>
  );
}
