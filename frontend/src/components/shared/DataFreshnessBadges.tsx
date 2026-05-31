interface DataFreshnessBadgesProps {
  dataStatus: string;
  coverage?: Record<string, number>;
}

const STATUS_LABEL: Record<string, string> = {
  FRESH: "최신",
  STALE: "갱신 필요",
  PARTIAL: "부분 수집",
};

export function DataFreshnessBadges({ dataStatus, coverage }: DataFreshnessBadgesProps) {
  const statusLabel = STATUS_LABEL[dataStatus] ?? dataStatus;
  const coverageTotal = coverage
    ? Object.values(coverage).reduce((sum, n) => sum + n, 0)
    : 0;

  return (
    <div className="flex flex-wrap gap-2">
      <span className="inline-flex items-center rounded-md border border-warm-border bg-charcoal/[0.03] px-2.5 py-1 text-xs text-muted-gray">
        데이터 {statusLabel}
      </span>
      {coverageTotal > 0 ? (
        <span className="inline-flex items-center rounded-md border border-warm-border bg-charcoal/[0.03] px-2.5 py-1 text-xs text-muted-gray">
          관계 커버리지 {coverageTotal}건
        </span>
      ) : null}
    </div>
  );
}
