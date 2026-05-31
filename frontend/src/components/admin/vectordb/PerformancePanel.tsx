import type { VectorDbStats } from "@/types/vectorDb";

interface PerformancePanelProps {
  stats: VectorDbStats;
  periodLabel: string;
}

function MiniBarChart({
  label,
  series,
  unit,
}: {
  label: string;
  series: number[];
  unit: string;
}) {
  const max = Math.max(...series, 1);
  return (
    <div>
      <p className="text-xs text-muted-gray">{label}</p>
      <div className="mt-2 flex h-16 items-end gap-0.5" aria-hidden>
        {series.map((v, i) => (
          <div
            key={`${label}-${i}`}
            className="min-w-[4px] flex-1 rounded-t bg-charcoal/80"
            style={{ height: `${Math.max(8, (v / max) * 100)}%` }}
          />
        ))}
      </div>
      <p className="mt-1 text-xs text-muted-gray">
        최근 {series.length}구간 · {unit}
      </p>
    </div>
  );
}

export function PerformancePanel({ stats, periodLabel }: PerformancePanelProps) {
  return (
    <section className="rounded-xl border border-warm-border bg-cream p-6 shadow-sm">
      <header className="mb-4">
        <h2 className="text-lg font-semibold text-charcoal">성능 지표</h2>
        <p className="mt-1 text-sm text-muted-gray">{periodLabel}</p>
      </header>
      <dl className="mb-6 grid grid-cols-3 gap-4 text-center">
        <div>
          <dt className="text-xs text-muted-gray">평균 응답</dt>
          <dd className="mt-1 text-lg font-semibold text-charcoal">
            {stats.avgLatencyMs.toFixed(0)}ms
          </dd>
        </div>
        <div>
          <dt className="text-xs text-muted-gray">평균 유사도</dt>
          <dd className="mt-1 text-lg font-semibold text-charcoal">
            {(stats.avgSimilarity * 100).toFixed(1)}%
          </dd>
        </div>
        <div>
          <dt className="text-xs text-muted-gray">24h 요청</dt>
          <dd className="mt-1 text-lg font-semibold text-charcoal">
            {new Intl.NumberFormat("ko-KR").format(stats.requestCount24h)}
          </dd>
        </div>
      </dl>
      <div className="grid gap-6 sm:grid-cols-3">
        <MiniBarChart label="응답 시간" series={stats.latencySeries} unit="ms" />
        <MiniBarChart
          label="유사도"
          series={stats.similaritySeries.map((v) => v * 100)}
          unit="%"
        />
        <MiniBarChart label="요청 수" series={stats.requestSeries} unit="건" />
      </div>
    </section>
  );
}
