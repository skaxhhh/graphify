import { StatCard } from "@/components/admin/vectordb/StatCard";
import type { VectorDbStats } from "@/types/vectorDb";

function formatNumber(value: number): string {
  return new Intl.NumberFormat("ko-KR").format(value);
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

function formatTypeBreakdown(byType: Record<string, number>): string {
  const parts = Object.entries(byType).map(([k, v]) => `${k} ${formatNumber(v)}`);
  return parts.join(" · ") || "—";
}

function formatLastJob(stats: VectorDbStats): string {
  const job = stats.lastJobs[0];
  if (!job) return "작업 없음";
  const label = job.jobType === "REINDEX" ? "재임베딩" : "정리";
  return `${label} · ${job.status}`;
}

interface VectorStatsDashboardProps {
  stats: VectorDbStats;
}

export function VectorStatsDashboard({ stats }: VectorStatsDashboardProps) {
  return (
    <section
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
      aria-label="벡터 인덱스 통계"
    >
      <StatCard label="총 벡터 수" value={formatNumber(stats.totalVectors)} />
      <StatCard
        label="유형별 분포"
        value={formatTypeBreakdown(stats.byType)}
        hint="COMPANY · INSIGHT · RELATION"
      />
      <StatCard label="인덱스 크기" value={formatBytes(stats.indexSizeBytes)} />
      <StatCard label="최근 작업" value={formatLastJob(stats)} />
    </section>
  );
}
