import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { CompareDock } from "@/components/watchlist/CompareDock";
import { ComparePanel } from "@/components/watchlist/ComparePanel";
import { WatchlistCard } from "@/components/watchlist/WatchlistCard";
import { WatchlistGridSkeleton } from "@/components/watchlist/WatchlistGridSkeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { MobileBottomSheet } from "@/components/graph/MobileBottomSheet";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { useDebounce } from "@/hooks/useDebounce";
import { ApiRequestError } from "@/lib/apiClient";
import { fetchCompanyCompare, fetchMyWatchlist, removeFromWatchlist } from "@/lib/watchlistApi";
import { useAuthStore } from "@/stores/authStore";
import type { CompareBasis } from "@/types/watchlist";

const MAX_COMPARE = 3;

export function WatchlistPage() {
  const navigate = useNavigate();
  const logout = useAuthStore((s) => s.logout);
  const queryClient = useQueryClient();

  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [compareOpen, setCompareOpen] = useState(false);
  const [basis, setBasis] = useState<CompareBasis>("INVESTMENT");
  const debouncedBasis = useDebounce(basis, 200);
  const [toast, setToast] = useState<string | null>(null);
  const [mobileCompareOpen, setMobileCompareOpen] = useState(false);

  const watchlistQuery = useQuery({
    queryKey: ["watchlist", "me"],
    queryFn: async () => {
      const response = await fetchMyWatchlist();
      return response.data;
    },
    retry: (failureCount, error) => {
      if (error instanceof ApiRequestError && error.code.startsWith("ERR_AUTH")) {
        return false;
      }
      return failureCount < 1;
    },
  });

  useEffect(() => {
    if (
      watchlistQuery.error instanceof ApiRequestError &&
      watchlistQuery.error.code.startsWith("ERR_AUTH")
    ) {
      logout();
    }
  }, [watchlistQuery.error, logout]);

  const compareQuery = useQuery({
    queryKey: ["companies", "compare", selectedIds, debouncedBasis],
    queryFn: async () => {
      const response = await fetchCompanyCompare(selectedIds, debouncedBasis);
      return response.data;
    },
    enabled: compareOpen && selectedIds.length > 0,
  });

  const removeMutation = useMutation({
    mutationFn: (companyId: number) => removeFromWatchlist(companyId),
    onSuccess: (_data, companyId) => {
      queryClient.invalidateQueries({ queryKey: ["watchlist", "me"] });
      setSelectedIds((prev) => prev.filter((id) => id !== companyId));
    },
  });

  const items = watchlistQuery.data?.items ?? [];

  const selectedNames = useMemo(
    () =>
      selectedIds
        .map((id) => items.find((item) => item.companyId === id)?.name)
        .filter((name): name is string => Boolean(name)),
    [selectedIds, items]
  );

  const pageState: PageStateKind = (() => {
    if (watchlistQuery.isLoading) return "loading";
    if (watchlistQuery.isError) return "error";
    if (items.length === 0) return "empty";
    return "populated";
  })();

  const toggleCompare = (companyId: number) => {
    setSelectedIds((prev) => {
      if (prev.includes(companyId)) {
        return prev.filter((id) => id !== companyId);
      }
      if (prev.length >= MAX_COMPARE) {
        setToast("최대 3개까지 비교할 수 있습니다.");
        window.setTimeout(() => setToast(null), 2000);
        return prev;
      }
      return [...prev, companyId];
    });
  };

  const handleCompare = () => {
    if (selectedIds.length === 0) return;
    setCompareOpen(true);
    if (window.matchMedia("(max-width: 767px)").matches) {
      setMobileCompareOpen(true);
    }
  };

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-8 md:px-8">
      {toast ? (
        <div
          className="fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-lg bg-charcoal px-4 py-2 text-sm text-off-white opacity-100 transition-opacity duration-200"
          role="status"
        >
          {toast}
        </div>
      ) : null}

      <header className="mb-8">
        <h1 className="text-2xl font-semibold text-charcoal">관심 기업</h1>
        <p className="mt-2 text-sm text-muted-gray">최대 3개 기업까지 비교할 수 있습니다.</p>
      </header>

      <PageState
        state={pageState}
        loading={<WatchlistGridSkeleton />}
        empty={
          <EmptyState
            title="등록된 관심 기업이 없습니다"
            description="기업 상세에서 관심 등록하거나 검색으로 기업을 찾아 보세요."
            suggestions={["삼성전자", "SK하이닉스", "NAVER"]}
            onSuggestionClick={(q) => navigate(`/search?q=${encodeURIComponent(q)}`)}
          />
        }
        error={
          <ErrorBanner
            message={
              watchlistQuery.error instanceof Error
                ? watchlistQuery.error.message
                : "관심 기업 목록을 불러오지 못했습니다."
            }
            onRetry={() => watchlistQuery.refetch()}
          />
        }
      >
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {items.map((item) => (
            <WatchlistCard
              key={item.companyId}
              item={item}
              selected={selectedIds.includes(item.companyId)}
              onToggleCompare={toggleCompare}
              onRemove={(id) => removeMutation.mutate(id)}
              removing={removeMutation.isPending}
            />
          ))}
        </div>

        <CompareDock
          selectedNames={selectedNames}
          onCompare={handleCompare}
          onClear={() => {
            setSelectedIds([]);
            setCompareOpen(false);
            setMobileCompareOpen(false);
          }}
        />

        <div className="hidden md:block">
          <ComparePanel
            open={compareOpen}
            basis={basis}
            onBasisChange={setBasis}
            data={compareQuery.data ?? undefined}
            loading={compareQuery.isLoading}
            error={compareQuery.isError}
            onRetry={() => compareQuery.refetch()}
          />
        </div>
      </PageState>

      <MobileBottomSheet
        open={mobileCompareOpen && compareOpen}
        title="기업 비교"
        onClose={() => setMobileCompareOpen(false)}
      >
        <ComparePanel
          open
          basis={basis}
          onBasisChange={setBasis}
          data={compareQuery.data ?? undefined}
          loading={compareQuery.isLoading}
          error={compareQuery.isError}
          onRetry={() => compareQuery.refetch()}
        />
      </MobileBottomSheet>

      {pageState === "empty" ? (
        <p className="mt-4 text-center">
          <Link to="/search" className="text-sm text-charcoal underline">
            기업 검색으로 이동
          </Link>
        </p>
      ) : null}
    </div>
  );
}
