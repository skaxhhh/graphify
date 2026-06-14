import { useEffect, useRef, useState } from "react";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  createdAt: Date;
}

function TypingIndicator() {
  return (
    <div className="flex items-end gap-2">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-xs font-bold text-emerald-400">
        D
      </div>
      <div className="flex items-center gap-1 rounded-2xl rounded-bl-sm bg-gray-800 px-4 py-3">
        <span className="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:-0.3s]" />
        <span className="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:-0.15s]" />
        <span className="h-2 w-2 animate-bounce rounded-full bg-gray-400" />
      </div>
    </div>
  );
}

function ChatMessage({ message }: { message: Message }) {
  const isUser = message.role === "user";
  return (
    <div className={`flex items-end gap-2 ${isUser ? "flex-row-reverse" : ""}`}>
      {!isUser ? (
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-xs font-bold text-emerald-400">
          D
        </div>
      ) : (
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gray-600 text-xs font-bold text-gray-300">
          나
        </div>
      )}
      <div
        className={`max-w-[70%] whitespace-pre-wrap rounded-2xl px-4 py-3 text-sm leading-relaxed ${
          isUser
            ? "rounded-br-sm bg-emerald-600 text-white"
            : "rounded-bl-sm bg-gray-800 text-gray-100"
        }`}
      >
        {message.content}
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || isTyping) return;

    const userMsg: Message = {
      id: crypto.randomUUID(),
      role: "user",
      content: trimmed,
      createdAt: new Date(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setIsTyping(true);

    // TODO: 실제 Agent API 연동 시 교체
    setTimeout(() => {
      const assistantMsg: Message = {
        id: crypto.randomUUID(),
        role: "assistant",
        content:
          "현재 Agent 백엔드 연동 준비 중입니다.\n곧 실제 트레이딩 데이터 조회 및 분석 기능이 제공될 예정입니다.",
        createdAt: new Date(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
      setIsTyping(false);
    }, 1200);
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
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-500/20">
          <span className="text-sm font-bold text-emerald-400">D</span>
        </div>
        <div>
          <p className="font-semibold text-white">DDS Agent</p>
          <p className="text-xs text-gray-400">모니터링 · 설명 · 리포팅</p>
        </div>
        <span className="ml-auto flex items-center gap-1.5 text-xs text-emerald-400">
          <span className="h-2 w-2 rounded-full bg-emerald-400" />
          대기 중
        </span>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 space-y-4 overflow-y-auto rounded-xl border border-white/10 bg-gray-900/50 p-4">
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
          className="flex-1 resize-none rounded-xl border border-white/10 bg-gray-800 px-4 py-3 text-sm text-white placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
          style={{ maxHeight: "8rem", overflowY: "auto" }}
          onInput={(e) => {
            const el = e.currentTarget;
            el.style.height = "auto";
            el.style.height = `${Math.min(el.scrollHeight, 128)}px`;
          }}
        />
        <button
          type="submit"
          disabled={!input.trim() || isTyping}
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-emerald-600 text-white transition-opacity hover:bg-emerald-500 disabled:opacity-40"
          aria-label="전송"
        >
          <svg className="h-4 w-4 rotate-90" fill="currentColor" viewBox="0 0 24 24">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
          </svg>
        </button>
      </form>
    </div>
  );
}
