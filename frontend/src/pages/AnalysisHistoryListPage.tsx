import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { DateRangeFilter, type DateRange } from "@/components/history/DateRangeFilter";
import { ExportMenu } from "@/components/history/ExportMenu";
import { HistoryCardListMobile } from "@/components/history/HistoryCardListMobile";
import { HistorySearchBar } from "@/components/history/HistorySearchBar";
import { HistoryTableDesktop } from "@/components/history/HistoryTableDesktop";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { Pagination } from "@/components/shared/Pagination";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { useDebounce } from "@/hooks/useDebounce";
import { ApiRequestError } from "@/lib/apiClient";
import { fetchMyHistory } from "@/lib/historyApi";
import { useAuthStore } from "@/stores/authStore";

const PAGE_SIZE = 10;

function dateRangeFromParams(params: URLSearchParams): DateRange {
  return {
    from: params.get("from") ?? "",
    to: params.get("to") ?? "",
  };
}

export function AnalysisHistoryListPage() {
  const logout = useAuthStore((s) => s.logout);
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0") || 0;
  const q = searchParams.get("q") ?? "";
  const dateRange = dateRangeFromParams(searchParams);

  const [localQ, setLocalQ] = useState(q);
  const debouncedQ = useDebounce(localQ, 300);

  useEffect(() => {
    setLocalQ(q);
  }, [q]);

  useEffect(() => {
    if (debouncedQ === q) return;
    const next = new URLSearchParams(searchParams);
    if (debouncedQ) {
      next.set("q", debouncedQ);
    } else {
      next.delete("q");
    }
    next.delete("page");
    setSearchParams(next, { replace: true });
  }, [debouncedQ, q, searchParams, setSearchParams]);

  const historyQuery = useQuery({
    queryKey: ["history", "me", page, q, dateRange.from, dateRange.to],
    queryFn: async () =>
      fetchMyHistory({
        page,
        size: PAGE_SIZE,
        q: q || undefined,
        from: dateRange.from || undefined,
        to: dateRange.to || undefined,
      }),
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  useEffect(() => {
    if (
      historyQuery.error instanceof ApiRequestError &&
      historyQuery.error.code.startsWith("ERR_AUTH")
    ) {
      logout();
    }
  }, [historyQuery.error, logout]);

  const items = historyQuery.data?.data?.items ?? [];
  const meta = historyQuery.data?.meta;
  const totalPages =
    meta && meta.total != null && meta.size
      ? Math.max(1, Math.ceil(meta.total / meta.size))
      : 1;

  const pageState: PageStateKind = (() => {
    if (historyQuery.isLoading) return "loading";
    if (historyQuery.isError) return "error";
    if (items.length === 0) return "empty";
    return "populated";
  })();

  const updateDateRange = (next: DateRange) => {
    const params = new URLSearchParams(searchParams);
    if (next.from) {
      params.set("from", next.from);
    } else {
      params.delete("from");
    }
    if (next.to) {
      params.set("to", next.to);
    } else {
      params.delete("to");
    }
    params.delete("page");
    setSearchParams(params, { replace: true });
  };

  const clearFilters = () => {
    setLocalQ("");
    setSearchParams({}, { replace: true });
  };

  return (
    <PageState
      state={pageState}
      loading={
        <div className="mx-auto max-w-[960px] space-y-4 px-4 py-8 md:px-8">
          <SkeletonBlock className="h-10 w-48" />
          {Array.from({ length: 6 }).map((_, index) => (
            <SkeletonBlock key={index} className="h-14 w-full rounded-lg" />
          ))}
        </div>
      }
      empty={
        <div className="mx-auto max-w-[960px] px-4 py-8 md:px-8">
          <EmptyState
            title="분석 이력이 없습니다"
            description="필터를 조정하거나 새 분석을 시작해 보세요."
          />
          <button
            type="button"
            onClick={clearFilters}
            className="mx-auto mt-4 block text-sm text-charcoal underline"
          >
            필터 초기화
          </button>
        </div>
      }
      error={
        <div className="mx-auto max-w-[960px] px-4 py-8 md:px-8">
          <ErrorBanner
            message={
              historyQuery.error instanceof Error
                ? historyQuery.error.message
                : "분석 이력을 불러오지 못했습니다."
            }
            onRetry={() => historyQuery.refetch()}
          />
        </div>
      }
    >
      <div className="mx-auto max-w-[960px] px-4 py-8 md:px-8">
        <header className="mb-8 flex flex-col gap-4">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <h1 className="text-2xl font-semibold text-charcoal">분석 이력</h1>
            <ExportMenu isPremium={false} />
          </div>
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <HistorySearchBar value={localQ} onChange={setLocalQ} />
            <DateRangeFilter value={dateRange} onChange={updateDateRange} />
          </div>
        </header>

        <HistoryTableDesktop items={items} />
        <HistoryCardListMobile items={items} />

        <Pagination
          page={page}
          totalPages={totalPages}
          disabled={historyQuery.isFetching}
          onPageChange={(nextPage) => {
            const next = new URLSearchParams(searchParams);
            if (nextPage > 0) {
              next.set("page", String(nextPage));
            } else {
              next.delete("page");
            }
            setSearchParams(next, { replace: true });
          }}
        />
      </div>
    </PageState>
  );
}
