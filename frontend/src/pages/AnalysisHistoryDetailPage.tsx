import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { CompareToggle } from "@/components/shared/CompareToggle";
import { GraphCompareLayout } from "@/components/history/detail/GraphCompareLayout";
import { HistoryDetailSkeleton } from "@/components/history/detail/HistoryDetailSkeleton";
import { HistoryDetailTabs } from "@/components/history/detail/HistoryDetailTabs";
import { SessionMetaBar } from "@/components/history/detail/SessionMetaBar";
import { TimelineSlider } from "@/components/history/detail/TimelineSlider";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { ApiRequestError } from "@/lib/apiClient";
import { fetchCompanyGraph } from "@/lib/graphApi";
import { fetchHistoryDetail } from "@/lib/historyApi";
import { useDebounce } from "@/hooks/useDebounce";
import { useAuthStore } from "@/stores/authStore";

export function AnalysisHistoryDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const logout = useAuthStore((s) => s.logout);

  const [compareEnabled, setCompareEnabled] = useState(false);
  const [timelineIndex, setTimelineIndex] = useState(0);
  const debouncedTimelineIndex = useDebounce(timelineIndex, 150);

  const detailQuery = useQuery({
    queryKey: ["history", "detail", sessionId],
    queryFn: async () => {
      const response = await fetchHistoryDetail(sessionId!);
      return response.data;
    },
    enabled: Boolean(sessionId),
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      if (error instanceof ApiRequestError && error.code === "ERR_HISTORY_002") {
        return false;
      }
      return failureCount < 1;
    },
  });

  useEffect(() => {
    if (
      detailQuery.error instanceof ApiRequestError &&
      detailQuery.error.code.startsWith("ERR_AUTH")
    ) {
      logout();
    }
  }, [detailQuery.error, logout]);

  const detail = detailQuery.data;
  const companyId = detail?.company.id;

  const liveGraphQuery = useQuery({
    queryKey: ["company-graph", companyId, "history-compare"],
    queryFn: async () => {
      const response = await fetchCompanyGraph(companyId!, { depth: 2 });
      return response.data;
    },
    enabled: compareEnabled && companyId != null,
  });

  useEffect(() => {
    if (detail?.timeline.length) {
      setTimelineIndex(detail.timeline.length - 1);
    }
  }, [detail?.sessionId, detail?.timeline.length]);

  const pageState: PageStateKind = (() => {
    if (!sessionId) return "empty";
    if (detailQuery.isLoading) return "loading";
    if (detailQuery.isError) return "error";
    if (!detail) return "empty";
    if (detail.status === "FAILED") return "empty";
    if (detail.graphSnapshot.nodes.length === 0 && detail.timeline.length === 0) {
      return "empty";
    }
    return "populated";
  })();

  const snapshotNodes = detail?.graphSnapshot.nodes ?? [];
  const snapshotEdges = detail?.graphSnapshot.edges ?? [];

  return (
    <PageState
      state={pageState}
      loading={<HistoryDetailSkeleton />}
      empty={
        <div className="mx-auto max-w-[1400px] px-4 py-8 md:px-8">
          <EmptyState
            title={
              detail?.status === "FAILED"
                ? "분석에 실패한 세션입니다"
                : "표시할 분석 데이터가 없습니다"
            }
            description={
              detail?.summaryLine ??
              "다른 이력을 선택하거나 목록으로 돌아가 주세요."
            }
          />
          <p className="mt-4 text-center">
            <Link to="/app/history" className="text-sm text-charcoal underline">
              분석 이력 목록으로
            </Link>
          </p>
        </div>
      }
      error={
        <div className="mx-auto max-w-[1400px] px-4 py-8 md:px-8">
          <ErrorBanner
            message={
              detailQuery.error instanceof Error
                ? detailQuery.error.message
                : "분석 이력을 불러오지 못했습니다."
            }
            onRetry={() => detailQuery.refetch()}
          />
          <Link to="/app/history" className="mt-4 inline-block text-sm text-muted-gray underline">
            분석 이력 목록으로
          </Link>
        </div>
      }
    >
      {detail ? (
        <div className="mx-auto max-w-[1400px] px-4 py-6 md:px-8">
          <button
            type="button"
            onClick={() => navigate("/app/history")}
            className="mb-4 text-sm text-muted-gray underline hover:text-charcoal"
          >
            ← 분석 이력 목록
          </button>

          <SessionMetaBar
            companyName={detail.company.name}
            analyzedAt={detail.analyzedAt}
            status={detail.status}
            compareToggle={
              <CompareToggle enabled={compareEnabled} onChange={setCompareEnabled} />
            }
          />

          {detail.timeline.length > 0 ? (
            <TimelineSlider
              events={detail.timeline}
              selectedIndex={debouncedTimelineIndex}
              onSelectIndex={setTimelineIndex}
            />
          ) : null}

          <GraphCompareLayout
            compareEnabled={compareEnabled}
            snapshotNodes={snapshotNodes}
            snapshotEdges={snapshotEdges}
            liveNodes={liveGraphQuery.data?.nodes ?? []}
            liveEdges={liveGraphQuery.data?.edges ?? []}
            liveLoading={compareEnabled && liveGraphQuery.isLoading}
          />

          <HistoryDetailTabs
            insights={detail.insights}
            signals={detail.signals}
            diffSummary={detail.diffSummary}
          />
        </div>
      ) : null}
    </PageState>
  );
}
