import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { AdminDashboardSkeleton } from "@/components/admin/AdminDashboardSkeleton";
import { AnomalyAlertList } from "@/components/admin/AnomalyAlertList";
import { KpiCardGrid } from "@/components/admin/KpiCardGrid";
import { SessionDetailDrawer } from "@/components/admin/SessionDetailDrawer";
import { TrendChartsRow } from "@/components/admin/TrendChartsRow";
import { UserUsageTable } from "@/components/admin/UserUsageTable";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { useDebounce } from "@/hooks/useDebounce";
import { ApiRequestError } from "@/lib/apiClient";
import { fetchAdminAgentStats, fetchAdminUserUsage } from "@/lib/adminApi";
import type { AdminPeriod, UserUsageRow } from "@/types/admin";

export function AdminDashboardPage() {
  const [period, setPeriod] = useState<AdminPeriod>("day");
  const debouncedPeriod = useDebounce(period, 200);
  const [selectedRow, setSelectedRow] = useState<UserUsageRow | null>(null);

  const statsQuery = useQuery({
    queryKey: ["admin", "agent", "stats", debouncedPeriod],
    queryFn: async () => {
      const response = await fetchAdminAgentStats(debouncedPeriod);
      return response.data;
    },
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const usageQuery = useQuery({
    queryKey: ["admin", "users", "usage"],
    queryFn: async () => {
      const response = await fetchAdminUserUsage();
      return response.data;
    },
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const pageState: PageStateKind = useMemo(() => {
    if (statsQuery.isLoading || usageQuery.isLoading) return "loading";
    if (statsQuery.isError && usageQuery.isError) return "error";
    const stats = statsQuery.data;
    if (!stats || (stats.runCount === 0 && stats.series.length === 0)) {
      return "empty";
    }
    return "populated";
  }, [
    statsQuery.data,
    statsQuery.isError,
    statsQuery.isLoading,
    usageQuery.isError,
    usageQuery.isLoading,
  ]);

  const stats = statsQuery.data;
  const usageRows = usageQuery.data?.rows ?? [];

  return (
    <div className="mx-auto w-full max-w-[1600px]">
      <header className="mb-6">
        <h1 className="text-2xl font-semibold text-charcoal">관리자 대시보드</h1>
        <p className="mt-1 text-sm text-muted-gray">
          Agent 실행·토큰·알림·사용자 사용량을 모니터링합니다.
        </p>
      </header>

      <PageState
        state={pageState}
        loading={<AdminDashboardSkeleton />}
        empty={
          <EmptyState
            title="대시보드 데이터 없음"
            description="아직 집계된 Agent 실행 메트릭이 없습니다."
          />
        }
        error={
          <ErrorBanner
            message={
              statsQuery.error instanceof ApiRequestError
                ? statsQuery.error.message
                : "대시보드 데이터를 불러오지 못했습니다."
            }
            onRetry={() => {
              void statsQuery.refetch();
              void usageQuery.refetch();
            }}
          />
        }
      >
        {stats ? (
          <div className="space-y-6">
            {statsQuery.isError ? (
              <ErrorBanner
                message="Agent 통계를 불러오지 못했습니다."
                onRetry={() => void statsQuery.refetch()}
              />
            ) : (
              <>
                <KpiCardGrid stats={stats} />
                <TrendChartsRow
                  stats={stats}
                  period={period}
                  onPeriodChange={setPeriod}
                />
              </>
            )}

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
              {statsQuery.isError ? null : (
                <AnomalyAlertList alerts={stats.alerts} />
              )}
              {usageQuery.isError ? (
                <ErrorBanner
                  message="사용자 사용량을 불러오지 못했습니다."
                  onRetry={() => void usageQuery.refetch()}
                />
              ) : (
                <UserUsageTable
                  rows={usageRows}
                  onRowClick={setSelectedRow}
                />
              )}
            </div>
          </div>
        ) : null}
      </PageState>

      <SessionDetailDrawer
        open={selectedRow != null}
        row={selectedRow}
        onClose={() => setSelectedRow(null)}
      />
    </div>
  );
}
