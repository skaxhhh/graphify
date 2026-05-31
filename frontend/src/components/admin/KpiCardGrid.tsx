import { KpiCard } from "@/components/admin/KpiCard";
import type { AdminAgentStats } from "@/types/admin";

function formatNumber(value: number): string {
  return new Intl.NumberFormat("ko-KR").format(value);
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function formatErrorRate(rate: number): string {
  return `${(rate * 100).toFixed(1)}%`;
}

interface KpiCardGridProps {
  stats: AdminAgentStats;
}

export function KpiCardGrid({ stats }: KpiCardGridProps) {
  return (
    <section
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
      aria-label="핵심 지표"
    >
      <KpiCard
        label="실행 수"
        value={formatNumber(stats.runCount)}
        hint={stats.period === "week" ? "최근 8주 합계" : "최근 14일 합계"}
      />
      <KpiCard
        label="평균 실행 시간"
        value={formatDuration(stats.avgDurationMs)}
      />
      <KpiCard
        label="토큰 사용량"
        value={formatNumber(stats.tokenUsage)}
      />
      <KpiCard
        label="오류율"
        value={formatErrorRate(stats.errorRate)}
      />
    </section>
  );
}
