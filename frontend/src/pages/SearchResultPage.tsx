import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { CompanyResultList } from "@/components/search/CompanyResultList";
import type { SearchFilters } from "@/components/search/FilterPopover";
import { SearchToolbar } from "@/components/search/SearchToolbar";
import { SemanticSuggestionsBanner } from "@/components/search/SemanticSuggestionsBanner";
import { EmptyState } from "@/components/shared/EmptyState";
import { GlobalSearchBar } from "@/components/shared/GlobalSearchBar";
import { PageState, type PageStateKind } from "@/components/shared/PageState";
import { Pagination } from "@/components/shared/Pagination";
import { useDebounce } from "@/hooks/useDebounce";
import { useRecentSearches } from "@/hooks/useRecentSearches";
import { fetchCompanySearch } from "@/lib/searchApi";
import { useAuthStore } from "@/stores/authStore";
import type { AutocompleteItem, CompanySort } from "@/types/search";

const PAGE_SIZE = 20;

function parseSort(value: string | null): CompanySort {
  if (value === "industry" || value === "updatedAt") {
    return value;
  }
  return "name";
}

function filtersFromParams(params: URLSearchParams): SearchFilters {
  return {
    industry: params.get("industry") ?? "",
    market: params.get("market") ?? "",
    dataStatus: params.get("dataStatus") ?? "",
  };
}

export function SearchResultPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const { addEntry } = useRecentSearches();

  const q = searchParams.get("q")?.trim() ?? "";
  const sort = parseSort(searchParams.get("sort"));
  const page = Number(searchParams.get("page") ?? "0") || 0;
  const urlFilters = filtersFromParams(searchParams);

  const [localFilters, setLocalFilters] = useState(urlFilters);
  const debouncedFilters = useDebounce(localFilters, 300);

  useEffect(() => {
    setLocalFilters(filtersFromParams(searchParams));
  }, [searchParams]);

  useEffect(() => {
    const current = filtersFromParams(searchParams);
    const changed =
      debouncedFilters.industry !== current.industry ||
      debouncedFilters.market !== current.market ||
      debouncedFilters.dataStatus !== current.dataStatus;

    if (!changed) {
      return;
    }

    const next = new URLSearchParams(searchParams);
    const apply = (key: keyof SearchFilters, value: string) => {
      if (value) {
        next.set(key, value);
      } else {
        next.delete(key);
      }
    };
    apply("industry", debouncedFilters.industry);
    apply("market", debouncedFilters.market);
    apply("dataStatus", debouncedFilters.dataStatus);
    next.delete("page");
    setSearchParams(next, { replace: true });
  }, [debouncedFilters, searchParams, setSearchParams]);

  const activeFilters = filtersFromParams(searchParams);

  const searchQuery = useQuery({
    queryKey: [
      "companies",
      "search",
      q,
      sort,
      page,
      activeFilters.industry,
      activeFilters.market,
      activeFilters.dataStatus,
    ],
    queryFn: async () =>
      fetchCompanySearch({
        q,
        sort,
        page,
        size: PAGE_SIZE,
        industry: activeFilters.industry || undefined,
        market: activeFilters.market || undefined,
        dataStatus: activeFilters.dataStatus || undefined,
      }),
    enabled: q.length > 0,
  });

  const filtering =
    localFilters.industry !== debouncedFilters.industry ||
    localFilters.market !== debouncedFilters.market ||
    localFilters.dataStatus !== debouncedFilters.dataStatus;

  const updateParams = (updates: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([key, value]) => {
      if (value == null || value === "") {
        next.delete(key);
      } else {
        next.set(key, value);
      }
    });
    setSearchParams(next, { replace: true });
  };

  const items = searchQuery.data?.data?.items ?? [];
  const hints = searchQuery.data?.data?.semanticHints;
  const total = searchQuery.data?.meta?.total ?? 0;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const pageState: PageStateKind = (() => {
    if (!q) return "empty";
    if (searchQuery.isLoading && !searchQuery.data) return "loading";
    if (searchQuery.isError) return "error";
    if (items.length === 0) return "empty";
    return "populated";
  })();

  const handleSortChange = (nextSort: CompanySort) => {
    updateParams({ sort: nextSort === "name" ? null : nextSort, page: null });
  };

  const handleQuerySelect = (query: string) => {
    updateParams({ q: query, page: null });
  };

  const handleCompanySelect = (item: AutocompleteItem) => {
    if (isAuthenticated) {
      addEntry(item.id, item.name);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-[1200px] flex-col gap-6 px-4 py-8 md:px-8">
      <GlobalSearchBar
        variant="inline"
        initialQuery={q}
        onCompanySelect={handleCompanySelect}
      />

      {!q ? (
        <PageState
          state="empty"
          empty={
            <EmptyState
              title="검색어를 입력해 주세요"
              description="기업명 또는 종목코드로 검색할 수 있습니다."
            />
          }
        >
          {null}
        </PageState>
      ) : (
        <>
          <SearchToolbar
            total={total}
            sort={sort}
            filters={localFilters}
            filtering={filtering || searchQuery.isFetching}
            onSortChange={handleSortChange}
            onFiltersChange={(next) => {
              setLocalFilters(next);
            }}
          />

          {hints &&
          (hints.relatedQueries.length > 0 || hints.similarCompanies.length > 0) ? (
            <SemanticSuggestionsBanner
              hints={hints}
              onQuerySelect={handleQuerySelect}
            />
          ) : null}

          <PageState
            state={pageState}
            loading={<CompanyResultList items={[]} loading />}
            empty={
              <EmptyState
                title="검색 결과가 없습니다"
                description="다른 검색어나 필터를 시도해 보세요."
                suggestions={hints?.relatedQueries ?? []}
                onSuggestionClick={handleQuerySelect}
              />
            }
            error={
              <EmptyState
                title="검색에 실패했습니다"
                description="잠시 후 다시 시도해 주세요."
              />
            }
          >
            <CompanyResultList items={items} />
            <Pagination
              page={page}
              totalPages={totalPages}
              onPageChange={(nextPage) =>
                updateParams({ page: nextPage > 0 ? String(nextPage) : null })
              }
              disabled={searchQuery.isFetching}
            />
          </PageState>
        </>
      )}
    </div>
  );
}
