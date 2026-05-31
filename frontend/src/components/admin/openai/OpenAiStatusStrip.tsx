import type { OpenAiStatus } from "@/types/openaiConfig";

interface OpenAiStatusStripProps {
  status: OpenAiStatus | null;
  loading: boolean;
  onRefresh: () => void;
}

function statusLabel(connection: string) {
  switch (connection) {
    case "OK":
      return { text: "정상", className: "bg-green-100 text-green-800" };
    case "ERROR":
      return { text: "오류", className: "bg-red-100 text-red-800" };
    default:
      return { text: "미설정", className: "bg-charcoal/10 text-muted-gray" };
  }
}

function formatWhen(iso: string | null) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("ko-KR", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

export function OpenAiStatusStrip({
  status,
  loading,
  onRefresh,
}: OpenAiStatusStripProps) {
  const pill = status ? statusLabel(status.connection) : statusLabel("NOT_CONFIGURED");

  return (
    <div className="sticky bottom-4 z-10 rounded-xl border border-warm-border bg-cream/95 p-4 shadow-[0_0_24px_rgba(0,0,0,0.06)] backdrop-blur-sm">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-3">
          <span
            className={`rounded-full px-3 py-1 text-xs font-medium ${pill.className}`}
          >
            {pill.text}
          </span>
          <span className="text-sm text-charcoal">
            토큰 사용:{" "}
            <strong>{status?.tokensUsed?.toLocaleString() ?? "—"}</strong>
          </span>
          <span className="text-sm text-muted-gray">
            Rate limit 잔여: {status?.rateLimitRemaining?.toLocaleString() ?? "—"}
          </span>
          <span className="text-xs text-muted-gray">
            확인: {formatWhen(status?.lastCheckedAt ?? null)}
          </span>
        </div>
        <button
          type="button"
          onClick={onRefresh}
          disabled={loading}
          className="flex h-10 w-10 items-center justify-center rounded-md border border-warm-border text-charcoal transition-opacity hover:opacity-80 disabled:opacity-50"
          aria-label="상태 새로고침"
          title="상태 새로고침"
        >
          {loading ? (
            <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-charcoal/30 border-t-charcoal" />
          ) : (
            <span aria-hidden>↻</span>
          )}
        </button>
      </div>
      {status?.message ? (
        <p className="mt-2 text-xs text-muted-gray">{status.message}</p>
      ) : null}
    </div>
  );
}
