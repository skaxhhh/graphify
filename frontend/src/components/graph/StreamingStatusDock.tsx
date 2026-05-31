import type { AgentStreamEvent } from "@/types/graph";

interface StreamingStatusDockProps {
  events: AgentStreamEvent[];
  streaming: boolean;
  error?: boolean;
  onReconnect?: () => void;
}

export function StreamingStatusDock({
  events,
  streaming,
  error,
  onReconnect,
}: StreamingStatusDockProps) {
  if (events.length === 0 && !streaming && !error) {
    return null;
  }

  const latest = events[events.length - 1];

  return (
    <aside className="border-b border-warm-border bg-charcoal/[0.03] px-4 py-2">
      <div className="flex items-center justify-between gap-3">
        <p className="text-xs text-charcoal">
          {error
            ? "스트리밍 연결이 끊어졌습니다."
            : latest?.message ?? "Agent 분석을 준비 중입니다…"}
        </p>
        {streaming ? (
          <span className="inline-block h-3 w-3 animate-spin rounded-full border-2 border-charcoal/20 border-t-charcoal" />
        ) : null}
        {error && onReconnect ? (
          <button
            type="button"
            onClick={onReconnect}
            className="text-xs text-charcoal underline"
          >
            재연결
          </button>
        ) : null}
      </div>
      {events.length > 1 ? (
        <ul className="mt-2 max-h-20 overflow-y-auto text-[11px] text-muted-gray">
          {events.map((event, index) => (
            <li key={`${event.stage}-${index}`}>
              [{event.stage}] {event.message}
            </li>
          ))}
        </ul>
      ) : null}
    </aside>
  );
}
