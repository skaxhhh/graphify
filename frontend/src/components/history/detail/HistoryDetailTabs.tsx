import { useState } from "react";
import { InsightCard } from "@/components/shared/InsightCard";
import { SignalList } from "@/components/shared/SignalList";
import type { CompanySignal, InsightCard as InsightCardType } from "@/types/company";
import type { HistoryDiffSummary } from "@/types/history";

type TabId = "insights" | "signals" | "diff";

interface HistoryDetailTabsProps {
  insights: InsightCardType[];
  signals: CompanySignal[];
  diffSummary: HistoryDiffSummary | null;
}

export function HistoryDetailTabs({ insights, signals, diffSummary }: HistoryDetailTabsProps) {
  const [tab, setTab] = useState<TabId>("insights");

  return (
    <section className="mt-8">
      <div className="hidden gap-2 border-b border-warm-border md:flex">
        <TabButton active={tab === "insights"} onClick={() => setTab("insights")}>
          인사이트 스냅샷
        </TabButton>
        <TabButton active={tab === "signals"} onClick={() => setTab("signals")}>
          신호 스냅샷
        </TabButton>
        <TabButton active={tab === "diff"} onClick={() => setTab("diff")}>
          AI 트렌드 요약
        </TabButton>
      </div>
      <div className="mb-4 md:hidden">
        <label className="sr-only" htmlFor="history-detail-tab">
          상세 탭
        </label>
        <select
          id="history-detail-tab"
          value={tab}
          onChange={(e) => setTab(e.target.value as TabId)}
          className="w-full rounded-lg border border-warm-border bg-cream px-3 py-2 text-sm text-charcoal"
        >
          <option value="insights">인사이트 스냅샷</option>
          <option value="signals">신호 스냅샷</option>
          <option value="diff">AI 트렌드 요약</option>
        </select>
      </div>

      {tab === "insights" ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {insights.length === 0 ? (
            <p className="text-sm text-muted-gray">저장된 인사이트가 없습니다.</p>
          ) : (
            insights.map((card) => (
              <InsightCard
                key={card.id}
                card={card}
                onDetail={() => {}}
                onReliability={() => {}}
              />
            ))
          )}
        </div>
      ) : null}

      {tab === "signals" ? (
        signals.length === 0 ? (
          <p className="text-sm text-muted-gray">저장된 신호가 없습니다.</p>
        ) : (
          <SignalList signals={signals} />
        )
      ) : null}

      {tab === "diff" ? (
        <DiffSummaryPanel diffSummary={diffSummary} />
      ) : null}
    </section>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
        active
          ? "border-charcoal text-charcoal"
          : "border-transparent text-muted-gray hover:text-charcoal"
      }`}
    >
      {children}
    </button>
  );
}

function DiffSummaryPanel({ diffSummary }: { diffSummary: HistoryDiffSummary | null }) {
  if (!diffSummary) {
    return (
      <p className="rounded-xl border border-dashed border-warm-border p-6 text-sm text-muted-gray">
        AI 트렌드 요약이 아직 생성되지 않았습니다.
      </p>
    );
  }

  return (
    <article className="rounded-xl border border-warm-border bg-cream p-6">
      <p className="text-sm leading-relaxed text-charcoal">{diffSummary.text}</p>
      <p className="mt-3 text-xs text-muted-gray">
        생성 {new Date(diffSummary.generatedAt).toLocaleString("ko-KR")}
      </p>
    </article>
  );
}
