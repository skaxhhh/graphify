import { useCallback, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CleanupPanel } from "@/components/admin/vectordb/CleanupPanel";
import { ConfirmDestructiveDialog } from "@/components/admin/vectordb/ConfirmDestructiveDialog";
import { PerformancePanel } from "@/components/admin/vectordb/PerformancePanel";
import { ReindexPanel } from "@/components/admin/vectordb/ReindexPanel";
import { VectorDbSkeleton } from "@/components/admin/vectordb/VectorDbSkeleton";
import { VectorStatsDashboard } from "@/components/admin/vectordb/VectorStatsDashboard";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { GhostButton } from "@/components/shared/GhostButton";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { useDebounce } from "@/hooks/useDebounce";
import { ApiRequestError } from "@/lib/apiClient";
import {
  fetchCleanupPreview,
  fetchEmbeddingJob,
  fetchVectorDbStats,
  runVectorCleanup,
  startVectorReindex,
} from "@/lib/adminVectorDbApi";
import type {
  EmbeddingJob,
  ReindexScope,
  VectorEntityType,
} from "@/types/vectorDb";

const CLEANUP_CONFIRM_PHRASE = "DELETE";

export function AdminVectorDbPage() {
  const queryClient = useQueryClient();
  const [scope, setScope] = useState<ReindexScope>("ALL");
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [activeJobId, setActiveJobId] = useState<number | null>(null);
  const [activeJob, setActiveJob] = useState<EmbeddingJob | null>(null);
  const [jobError, setJobError] = useState<string | null>(null);
  const [reindexConfirmOpen, setReindexConfirmOpen] = useState(false);

  const [olderThanDays, setOlderThanDays] = useState(90);
  const [cleanupTypes, setCleanupTypes] = useState<VectorEntityType[]>([
    "COMPANY",
    "INSIGHT",
  ]);
  const debouncedCleanupRule = useDebounce(
    { olderThanDays, types: cleanupTypes },
    300
  );
  const [cleanupConfirmOpen, setCleanupConfirmOpen] = useState(false);
  const [cleanupPhrase, setCleanupPhrase] = useState("");
  const [chartPeriod, setChartPeriod] = useState<"24h" | "7d">("24h");
  const debouncedChartPeriod = useDebounce(chartPeriod, 300);

  const [toast, setToast] = useState<string | null>(null);

  const statsQuery = useQuery({
    queryKey: ["admin", "vectordb", "stats"],
    queryFn: () => fetchVectorDbStats(),
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const previewQuery = useQuery({
    queryKey: [
      "admin",
      "vectordb",
      "cleanup-preview",
      debouncedCleanupRule.olderThanDays,
      debouncedCleanupRule.types.join(","),
    ],
    queryFn: () =>
      fetchCleanupPreview(
        debouncedCleanupRule.olderThanDays,
        debouncedCleanupRule.types
      ),
    enabled:
      statsQuery.isSuccess &&
      debouncedCleanupRule.types.length > 0 &&
      (statsQuery.data?.data?.totalVectors ?? 0) > 0,
  });

  const pageState: PageStateKind = useMemo(() => {
    if (statsQuery.isLoading) return "loading";
    if (statsQuery.isError) return "error";
    const stats = statsQuery.data?.data;
    if (!stats || stats.totalVectors === 0) return "empty";
    return "populated";
  }, [statsQuery.data, statsQuery.isError, statsQuery.isLoading]);

  const stats = statsQuery.data?.data;

  const pollJob = useCallback(async (jobId: number) => {
    try {
      const res = await fetchEmbeddingJob(jobId);
      const job = res.data;
      if (!job) return;
      setActiveJob(job);
      setJobError(null);
      if (job.status === "SUCCESS") {
        setActiveJobId(null);
        void queryClient.invalidateQueries({ queryKey: ["admin", "vectordb", "stats"] });
        setToast("재임베딩이 완료되었습니다.");
      } else if (job.status === "FAILED") {
        setActiveJobId(null);
        setJobError(job.message ?? "재임베딩에 실패했습니다.");
      }
    } catch (err) {
      const message =
        err instanceof ApiRequestError ? err.message : "작업 상태 조회에 실패했습니다.";
      setJobError(message);
      setActiveJobId(null);
    }
  }, [queryClient]);

  useEffect(() => {
    if (activeJobId == null) return;
    const tick = () => {
      void pollJob(activeJobId);
    };
    tick();
    const id = window.setInterval(tick, 2000);
    return () => window.clearInterval(id);
  }, [activeJobId, pollJob]);

  useEffect(() => {
    if (!toast) return;
    const id = window.setTimeout(() => setToast(null), 4000);
    return () => window.clearTimeout(id);
  }, [toast]);

  const reindexMutation = useMutation({
    mutationFn: () =>
      startVectorReindex({
        scope,
        targetIds: scope === "SELECTED" ? selectedIds : undefined,
      }),
    onSuccess: (res) => {
      const jobId = res.data?.jobId;
      if (jobId != null) {
        setActiveJobId(jobId);
        setActiveJob({
          jobId,
          jobType: "REINDEX",
          scope,
          status: "RUNNING",
          progress: 0,
          message: "재임베딩을 시작했습니다.",
          createdAt: new Date().toISOString(),
          completedAt: null,
        });
      }
      setReindexConfirmOpen(false);
    },
    onError: (err) => {
      const message =
        err instanceof ApiRequestError ? err.message : "재임베딩 시작에 실패했습니다.";
      setJobError(message);
      setReindexConfirmOpen(false);
    },
  });

  const cleanupMutation = useMutation({
    mutationFn: () =>
      runVectorCleanup({
        olderThanDays,
        types: cleanupTypes,
      }),
    onSuccess: (res) => {
      setCleanupConfirmOpen(false);
      setCleanupPhrase("");
      void queryClient.invalidateQueries({ queryKey: ["admin", "vectordb"] });
      setToast(`만료 벡터 ${res.data?.deletedCount ?? 0}건을 삭제했습니다.`);
    },
  });

  const toggleCleanupType = (type: VectorEntityType) => {
    setCleanupTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    );
  };

  const periodLabel =
    debouncedChartPeriod === "24h" ? "최근 24시간" : "최근 7일 (집계)";

  const jobRunning =
    activeJob?.status === "RUNNING" || activeJob?.status === "PENDING";

  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-8 p-6 md:p-8">
      <header>
        <h1 className="text-2xl font-semibold text-charcoal">Vector DB 관리</h1>
        <p className="mt-1 text-sm text-muted-gray">
          벡터 인덱스 통계, 재임베딩, 만료 데이터 정리를 관리합니다.
        </p>
      </header>

      {toast ? (
        <p
          className="rounded-lg border border-warm-border bg-cream px-4 py-2 text-sm text-charcoal"
          role="status"
        >
          {toast}
        </p>
      ) : null}

      <PageState
        state={pageState}
        loading={<VectorDbSkeleton />}
        empty={
          <div className="space-y-4">
            <EmptyState
              title="인덱스가 비어 있습니다"
              description="Azure OpenAI 연결을 설정한 뒤 첫 재임베딩을 실행하면 벡터가 생성됩니다."
            />
            <div className="flex justify-center">
              <GhostButton type="button" onClick={() => statsQuery.refetch()}>
                새로고침
              </GhostButton>
            </div>
          </div>
        }
        error={
          <ErrorBanner
            message={
              statsQuery.error instanceof ApiRequestError
                ? statsQuery.error.message
                : "통계를 불러오지 못했습니다."
            }
            onRetry={() => statsQuery.refetch()}
          />
        }
      >
        {stats ? (
          <>
            <VectorStatsDashboard stats={stats} />

            <div className="flex justify-end gap-2">
              {(["24h", "7d"] as const).map((p) => (
                <GhostButton
                  key={p}
                  type="button"
                  className={debouncedChartPeriod === p ? "ring-1 ring-charcoal/30" : ""}
                  onClick={() => setChartPeriod(p)}
                >
                  {p === "24h" ? "24시간" : "7일"}
                </GhostButton>
              ))}
            </div>

            <div className="grid items-start gap-8 lg:grid-cols-2">
              <ReindexPanel
                scope={scope}
                onScopeChange={setScope}
                selectedIds={selectedIds}
                onSelectedIdsChange={setSelectedIds}
                activeJob={activeJob}
                jobError={jobError}
                disabled={pageState !== "populated"}
                onRequestReindex={() => setReindexConfirmOpen(true)}
              />
              <PerformancePanel stats={stats} periodLabel={periodLabel} />
            </div>

            <CleanupPanel
              olderThanDays={olderThanDays}
              onOlderThanDaysChange={setOlderThanDays}
              types={cleanupTypes}
              onToggleType={toggleCleanupType}
              previewCount={previewQuery.data?.data?.previewCount ?? null}
              previewLoading={previewQuery.isFetching}
              disabled={jobRunning}
              onRequestCleanup={() => setCleanupConfirmOpen(true)}
            />
          </>
        ) : null}
      </PageState>

      <ConfirmDestructiveDialog
        open={reindexConfirmOpen}
        title="재임베딩 실행"
        description={
          scope === "ALL"
            ? "전체 벡터 인덱스를 재생성합니다. 작업 중 검색 품질이 일시적으로 변동될 수 있습니다."
            : `선택한 ${selectedIds.length}개 기업에 대해 재임베딩을 실행합니다.`
        }
        confirmLabel="실행"
        confirmPhrase=""
        onConfirmPhraseChange={() => undefined}
        loading={reindexMutation.isPending}
        onClose={() => setReindexConfirmOpen(false)}
        onConfirm={() => reindexMutation.mutate()}
      />

      <ConfirmDestructiveDialog
        open={cleanupConfirmOpen}
        title="만료 벡터 삭제"
        description={`약 ${previewQuery.data?.data?.previewCount ?? 0}건의 벡터가 삭제됩니다. 이 작업은 되돌릴 수 없습니다.`}
        confirmLabel="삭제 실행"
        requirePhrase={CLEANUP_CONFIRM_PHRASE}
        confirmPhrase={cleanupPhrase}
        onConfirmPhraseChange={setCleanupPhrase}
        loading={cleanupMutation.isPending}
        onClose={() => {
          setCleanupConfirmOpen(false);
          setCleanupPhrase("");
        }}
        onConfirm={() => cleanupMutation.mutate()}
      />
    </div>
  );
}
