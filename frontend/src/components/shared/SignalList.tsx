import type { CompanySignal } from "@/types/company";

const KIND_LABEL: Record<string, string> = {
  RISK: "리스크",
  OPPORTUNITY: "기회",
};

interface SignalListProps {
  signals: CompanySignal[];
}

export function SignalList({ signals }: SignalListProps) {
  if (signals.length === 0) {
    return null;
  }

  return (
    <section className="rounded-xl border border-warm-border bg-cream p-5">
      <h3 className="text-sm font-semibold text-charcoal">리스크 · 기회 신호</h3>
      <ul className="mt-4 space-y-3">
        {signals.map((signal) => (
          <li
            key={`${signal.kind}-${signal.label}`}
            className="flex flex-col gap-1 border-b border-warm-border pb-3 last:border-0 last:pb-0"
          >
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-md border border-warm-border bg-charcoal/[0.03] px-2 py-0.5 text-xs text-muted-gray">
                {KIND_LABEL[signal.kind] ?? signal.kind}
              </span>
              <span className="text-sm font-medium text-charcoal">{signal.label}</span>
            </div>
            {signal.relatedNodeIds.length > 0 ? (
              <p className="text-xs text-muted-gray">
                관련 노드: {signal.relatedNodeIds.join(", ")}
              </p>
            ) : null}
            {signal.sources.length > 0 ? (
              <p className="text-xs text-muted-gray">출처: {signal.sources.join(", ")}</p>
            ) : null}
          </li>
        ))}
      </ul>
    </section>
  );
}
