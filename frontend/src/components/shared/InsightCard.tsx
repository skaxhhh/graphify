import type { InsightCard as InsightCardType } from "@/types/company";

const TYPE_LABEL: Record<string, string> = {
  SUPPLY_CHAIN: "공급망",
  INVESTMENT: "투자",
  PARTNERSHIP: "파트너십",
  RISK: "리스크",
};

const CONFIDENCE_LABEL: Record<string, string> = {
  HIGH: "높음",
  MEDIUM: "보통",
  LOW: "낮음",
};

interface InsightCardProps {
  card: InsightCardType;
  onDetail: (card: InsightCardType) => void;
  onReliability: () => void;
  onGraphHighlight?: (card: InsightCardType) => void;
}

export function InsightCard({
  card,
  onDetail,
  onReliability,
  onGraphHighlight,
}: InsightCardProps) {
  return (
    <article className="flex h-full flex-col rounded-xl border border-warm-border bg-cream p-5">
      <div className="flex items-start justify-between gap-2">
        <span className="rounded-md border border-warm-border bg-charcoal/[0.03] px-2 py-0.5 text-xs text-muted-gray">
          {TYPE_LABEL[card.type] ?? card.type}
        </span>
        <button
          type="button"
          onClick={onReliability}
          className="text-xs text-muted-gray underline hover:text-charcoal"
        >
          신뢰도 {CONFIDENCE_LABEL[card.confidence] ?? card.confidence}
        </button>
      </div>
      <h4 className="mt-3 text-base font-semibold text-charcoal">{card.title}</h4>
      <p className="mt-2 flex-1 text-sm leading-relaxed text-muted-gray">{card.summary}</p>
      <div className="mt-4 flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => onDetail(card)}
          className="text-sm text-charcoal underline hover:opacity-80"
        >
          자세히
        </button>
        {onGraphHighlight ? (
          <button
            type="button"
            onClick={() => onGraphHighlight(card)}
            className="text-sm text-muted-gray underline hover:text-charcoal"
          >
            그래프에서 보기
          </button>
        ) : null}
      </div>
    </article>
  );
}
