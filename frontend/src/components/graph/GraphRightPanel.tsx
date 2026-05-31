import { DataProvenanceCard } from "@/components/shared/DataProvenanceCard";
import { DisclaimerCompact } from "@/components/shared/DisclaimerCompact";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import type { InsightCard } from "@/types/company";
import type { Provenance } from "@/types/company";

interface GraphRightPanelProps {
  insights: InsightCard[];
  loading: boolean;
  provenance: Provenance;
  onReliability: () => void;
  onInsightDetail: (card: InsightCard) => void;
}

export function GraphRightPanel({
  insights,
  loading,
  provenance,
  onReliability,
  onInsightDetail,
}: GraphRightPanelProps) {
  return (
    <aside className="flex h-full min-h-0 w-96 shrink-0 flex-col border-l border-warm-border bg-cream">
      <div className="flex-1 overflow-y-auto p-4">
        <h3 className="mb-3 text-sm font-semibold text-charcoal">인사이트</h3>
        {loading ? (
          <div className="space-y-3">
            <SkeletonBlock className="h-24 rounded-xl" />
            <SkeletonBlock className="h-24 rounded-xl" />
          </div>
        ) : insights.length === 0 ? (
          <p className="text-sm text-muted-gray">표시할 인사이트가 없습니다.</p>
        ) : (
          <ul className="space-y-3">
            {insights.map((card) => (
              <li
                key={card.id}
                className="rounded-xl border border-warm-border bg-off-white p-3"
              >
                <p className="text-xs text-muted-gray">{card.type}</p>
                <p className="mt-1 text-sm font-medium text-charcoal">{card.title}</p>
                <p className="mt-1 line-clamp-2 text-xs text-muted-gray">{card.summary}</p>
                <button
                  type="button"
                  className="mt-2 text-xs text-charcoal underline"
                  onClick={() => onInsightDetail(card)}
                >
                  자세히
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
      <footer className="space-y-3 border-t border-warm-border p-4">
        <DataProvenanceCard provenance={provenance} onReliabilityClick={onReliability} />
        <DisclaimerCompact />
      </footer>
    </aside>
  );
}
