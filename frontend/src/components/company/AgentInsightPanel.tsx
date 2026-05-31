import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import type { AgentInsight } from "@/types/company";

interface AgentInsightPanelProps {
  insight: AgentInsight | null | undefined;
  loading: boolean;
  /** 0–100, 로딩 중 진행률 */
  loadingPercent?: number;
  loadingLabel?: string;
  errorMessage?: string | null;
  onRetry?: () => void;
}

function renderContent(content: string) {
  const blocks = content.split(/\n{2,}/).filter((block) => block.trim().length > 0);
  return blocks.map((block, index) => {
    const trimmed = block.trim();
    if (trimmed.startsWith("## ")) {
      return (
        <h3 key={index} className="mt-4 text-sm font-semibold text-charcoal first:mt-0">
          {trimmed.replace(/^##\s+/, "")}
        </h3>
      );
    }
    if (trimmed.startsWith("**") && trimmed.includes("**:")) {
      const parts = trimmed.split("**:");
      const label = parts[0] ?? "";
      const body = parts.slice(1).join("**:");
      return (
        <p key={index} className="mt-2 text-sm leading-relaxed text-charcoal">
          <span className="font-medium">{label.replace(/\*\*/g, "")}: </span>
          {body.replace(/\*\*/g, "")}
        </p>
      );
    }
    return (
      <p key={index} className="mt-2 text-sm leading-relaxed text-muted-gray">
        {trimmed.replace(/\*\*/g, "")}
      </p>
    );
  });
}

export function AgentInsightPanel({
  insight,
  loading,
  loadingPercent = 0,
  loadingLabel,
  errorMessage,
  onRetry,
}: AgentInsightPanelProps) {
  if (loading) {
    const percent = Math.min(100, Math.max(0, Math.round(loadingPercent)));
    return (
      <section
        aria-busy="true"
        aria-label="Agent 인사이트 로딩"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        data-testid="agent-insight-loading"
        className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-charcoal">AI 인사이트</h2>
          <span
            className="text-sm font-medium tabular-nums text-charcoal"
            data-testid="agent-insight-progress-percent"
          >
            {percent}%
          </span>
        </div>
        <div
          className="h-2 w-full overflow-hidden rounded-full bg-warm-border/60"
          role="progressbar"
          aria-valuenow={percent}
          aria-valuemin={0}
          aria-valuemax={100}
        >
          <div
            className="h-full rounded-full bg-charcoal transition-[width] duration-300 ease-out"
            style={{ width: `${percent}%` }}
            data-testid="agent-insight-progress-bar"
          />
        </div>
        <p className="mt-2 text-xs text-muted-gray" data-testid="agent-insight-progress-label">
          {loadingLabel ?? "AI 인사이트를 생성하는 중입니다…"}
        </p>
        <SkeletonBlock className="mt-4 h-40 rounded-xl opacity-60" />
      </section>
    );
  }

  if (errorMessage) {
    return (
      <section className="rounded-xl border border-warm-border bg-cream p-6">
        <h2 className="text-sm font-semibold text-charcoal">AI 인사이트</h2>
        <p className="mt-3 text-sm text-muted-gray">{errorMessage}</p>
        {onRetry ? (
          <button
            type="button"
            className="mt-4 text-sm text-charcoal underline hover:opacity-80"
            onClick={onRetry}
          >
            다시 시도
          </button>
        ) : null}
      </section>
    );
  }

  if (!insight?.content) {
    return (
      <section className="rounded-xl border border-dashed border-warm-border p-8 text-center">
        <p className="text-sm text-muted-gray">DART 데이터 수집 후 AI 인사이트가 생성됩니다.</p>
      </section>
    );
  }

  return (
    <section
      className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
      data-testid="agent-insight-section"
    >
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-semibold text-charcoal">AI 인사이트</h2>
        <span className="text-xs text-muted-gray">
          {insight.modelLabel ? `${insight.modelLabel} · ` : ""}
          {new Date(insight.generatedAt).toLocaleString("ko-KR")}
        </span>
      </div>
      <div className="prose-sm max-w-none">{renderContent(insight.content)}</div>
    </section>
  );
}
