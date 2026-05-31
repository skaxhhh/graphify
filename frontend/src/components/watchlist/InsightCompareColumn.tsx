import { InsightCard } from "@/components/shared/InsightCard";
import type { CompareCompany } from "@/types/watchlist";

interface InsightCompareColumnProps {
  company: CompareCompany;
}

export function InsightCompareColumn({ company }: InsightCompareColumnProps) {
  return (
    <div className="min-w-[280px] snap-start rounded-xl border border-warm-border bg-cream p-4 md:min-w-0">
      <h3 className="text-base font-semibold text-charcoal">{company.name}</h3>
      <p className="mt-1 text-xs text-muted-gray">{company.industry ?? "—"}</p>
      <dl className="mt-4 grid grid-cols-3 gap-2 text-center text-xs">
        <div>
          <dt className="text-muted-gray">인사이트</dt>
          <dd className="mt-1 font-medium text-charcoal">{company.metrics.insightCount}</dd>
        </div>
        <div>
          <dt className="text-muted-gray">신호</dt>
          <dd className="mt-1 font-medium text-charcoal">{company.metrics.signalCount}</dd>
        </div>
        <div>
          <dt className="text-muted-gray">관계</dt>
          <dd className="mt-1 font-medium text-charcoal">{company.metrics.relationCount}</dd>
        </div>
      </dl>
      <div className="mt-4 space-y-3">
        {company.insightCards.length === 0 ? (
          <p className="text-xs text-muted-gray">해당 기준 인사이트가 없습니다.</p>
        ) : (
          company.insightCards.map((card) => (
            <InsightCard
              key={card.id}
              card={card}
              onDetail={() => {}}
              onReliability={() => {}}
            />
          ))
        )}
      </div>
    </div>
  );
}
