import { SimpleBarChart } from "@/components/admin/SimpleBarChart";
import type { AdminAgentStats, AdminPeriod } from "@/types/admin";

interface TrendChartsRowProps {
  stats: AdminAgentStats;
  period: AdminPeriod;
  onPeriodChange: (period: AdminPeriod) => void;
}

export function TrendChartsRow({
  stats,
  period,
  onPeriodChange,
}: TrendChartsRowProps) {
  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-base font-semibold text-charcoal">추이</h2>
        <div className="inline-flex rounded-lg border border-warm-border p-0.5">
          <button
            type="button"
            onClick={() => onPeriodChange("day")}
            className={`rounded-md px-3 py-1.5 text-sm transition-colors ${
              period === "day"
                ? "bg-charcoal text-cream"
                : "text-muted-gray hover:text-charcoal"
            }`}
          >
            일별
          </button>
          <button
            type="button"
            onClick={() => onPeriodChange("week")}
            className={`rounded-md px-3 py-1.5 text-sm transition-colors ${
              period === "week"
                ? "bg-charcoal text-cream"
                : "text-muted-gray hover:text-charcoal"
            }`}
          >
            주별
          </button>
        </div>
      </div>
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <SimpleBarChart
          title="Agent 실행 수"
          points={stats.series}
          valueKey="runCount"
          valueLabel="일/주 단위 실행 건수"
        />
        <SimpleBarChart
          title="토큰 사용량"
          points={stats.series}
          valueKey="tokenUsage"
          valueLabel="일/주 단위 토큰"
        />
      </div>
    </section>
  );
}
