import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { HOME_PANEL_CARD_CLASS } from "@/components/home/homePanelStyles";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { fetchTrendingCompanies } from "@/lib/homeApi";

function formatViewCount(count: number): string {
  return new Intl.NumberFormat("ko-KR").format(count);
}

export function TrendingCompaniesPanel() {
  const query = useQuery({
    queryKey: ["home", "trending-companies"],
    queryFn: () => fetchTrendingCompanies(8),
  });

  if (query.isLoading) {
    return (
      <article className={HOME_PANEL_CARD_CLASS}>
        <SkeletonBlock className="h-6 w-40" />
        <div className="mt-4 min-h-0 flex-1 space-y-3 overflow-y-auto pr-1">
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonBlock key={i} className="h-12 w-full" />
          ))}
        </div>
      </article>
    );
  }

  if (query.isError) {
    return (
      <article className={HOME_PANEL_CARD_CLASS}>
        <h2 className="text-lg font-semibold text-charcoal">인기 조회 기업</h2>
        <div className="mt-4">
          <ErrorBanner
            message="순위를 불러오지 못했습니다."
            onRetry={() => void query.refetch()}
          />
        </div>
      </article>
    );
  }

  const items = query.data?.data ?? [];

  return (
    <article className={HOME_PANEL_CARD_CLASS}>
      <header className="shrink-0">
        <h2 className="text-lg font-semibold text-charcoal">인기 조회 기업</h2>
        <p className="mt-1 text-sm text-muted-gray">
          최근 시스템에서 가장 많이 조회된 기업입니다.
        </p>
      </header>

      {items.length === 0 ? (
        <p className="mt-6 flex flex-1 items-center justify-center text-sm text-muted-gray">
          아직 조회 데이터가 없습니다.
        </p>
      ) : (
        <ol className="mt-4 min-h-0 flex-1 space-y-1 overflow-y-auto pr-1" aria-label="인기 조회 순위">
          {items.map((item) => (
            <li key={item.companyId}>
              <Link
                to={`/companies/${item.companyId}`}
                className="flex items-center gap-3 rounded-lg px-2 py-2.5 transition-colors hover:bg-charcoal/[0.03]"
              >
                <span
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-charcoal/[0.05] text-sm font-semibold text-charcoal"
                  aria-hidden
                >
                  {item.rank}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-charcoal">
                    {item.name}
                  </span>
                  <span className="block truncate text-xs text-muted-gray">
                    {[item.ticker, item.industry].filter(Boolean).join(" · ")}
                  </span>
                </span>
                <span className="shrink-0 text-xs text-muted-gray">
                  {formatViewCount(item.viewCount)}회
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </article>
  );
}
