import type { CompanySignal } from "@/types/company";

interface CompanySignalSectionsProps {
  signals: CompanySignal[];
}

function SignalBlock({
  title,
  testId,
  signals,
  emptyMessage,
}: {
  title: string;
  testId: string;
  signals: CompanySignal[];
  emptyMessage: string;
}) {
  return (
    <section
      className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
      data-testid={testId}
    >
      <h2 className="text-sm font-semibold text-charcoal">{title}</h2>
      {signals.length === 0 ? (
        <p className="mt-3 text-sm text-muted-gray">{emptyMessage}</p>
      ) : (
        <ul className="mt-4 space-y-3">
          {signals.map((signal) => (
            <li
              key={`${signal.kind}-${signal.label}`}
              className="border-b border-warm-border/60 pb-3 last:border-0 last:pb-0"
            >
              <p className="text-sm font-medium text-charcoal">{signal.label}</p>
              {signal.relatedNodeIds.length > 0 ? (
                <p className="mt-1 text-xs text-muted-gray">
                  관련 노드: {signal.relatedNodeIds.join(", ")}
                </p>
              ) : null}
              {signal.sources.length > 0 ? (
                <p className="mt-0.5 text-xs text-muted-gray">출처: {signal.sources.join(", ")}</p>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export function CompanySignalSections({ signals }: CompanySignalSectionsProps) {
  const risks = signals.filter((s) => s.kind === "RISK");
  const opportunities = signals.filter((s) => s.kind === "OPPORTUNITY");

  return (
  <>
    <SignalBlock
      title="리스크 신호"
      testId="risk-signals-section"
      signals={risks}
      emptyMessage="현재 감지된 리스크 신호가 없습니다."
    />
    <SignalBlock
      title="기회 신호"
      testId="opportunity-signals-section"
      signals={opportunities}
      emptyMessage="현재 감지된 기회 신호가 없습니다."
    />
  </>
  );
}
