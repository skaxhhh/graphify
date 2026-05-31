import type { StatsPoint } from "@/types/admin";

interface SimpleBarChartProps {
  title: string;
  points: StatsPoint[];
  valueKey: "runCount" | "tokenUsage";
  valueLabel: string;
}

function formatAxisDate(date: string): string {
  const d = new Date(date);
  if (Number.isNaN(d.getTime())) return date;
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

export function SimpleBarChart({
  title,
  points,
  valueKey,
  valueLabel,
}: SimpleBarChartProps) {
  const max = Math.max(1, ...points.map((p) => p[valueKey]));

  return (
    <section className="rounded-xl border border-warm-border bg-cream p-4">
      <h3 className="text-sm font-medium text-charcoal">{title}</h3>
      <p className="mt-1 text-xs text-muted-gray">{valueLabel}</p>
      {points.length === 0 ? (
        <p className="mt-8 text-center text-sm text-muted-gray">
          차트 데이터가 없습니다.
        </p>
      ) : (
        <div
          className="mt-4 flex h-48 items-end gap-1 overflow-x-auto pb-2"
          role="img"
          aria-label={title}
        >
          {points.map((point) => {
            const value = point[valueKey];
            const heightPct = Math.max(4, Math.round((value / max) * 100));
            return (
              <div
                key={point.date}
                className="flex min-w-[28px] flex-1 flex-col items-center gap-1"
                title={`${formatAxisDate(point.date)}: ${value.toLocaleString("ko-KR")}`}
              >
                <div
                  className="w-full max-w-[40px] rounded-t bg-charcoal/70 transition-all"
                  style={{ height: `${heightPct}%` }}
                />
                <span className="text-[10px] text-muted-gray">
                  {formatAxisDate(point.date)}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
