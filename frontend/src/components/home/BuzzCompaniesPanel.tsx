import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { HOME_PANEL_CARD_CLASS } from "@/components/home/homePanelStyles";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { fetchBuzzCompanies } from "@/lib/homeApi";

function formatPrice(value: number | null | undefined): string {
  if (value == null) return "—";
  return `${value.toLocaleString("ko-KR")}원`;
}

export function BuzzCompaniesPanel() {
  const query = useQuery({
    queryKey: ["home", "buzz-companies"],
    queryFn: () => fetchBuzzCompanies(8),
  });

  if (query.isLoading) {
    return (
      <article className={HOME_PANEL_CARD_CLASS}>
        <SkeletonBlock className="h-6 w-36" />
        <div className="mt-4 min-h-0 flex-1 space-y-3 overflow-hidden">
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
        <h2 className="text-lg font-semibold text-charcoal">주목 기업</h2>
        <div className="mt-4 min-h-0 flex-1">
          <ErrorBanner
            message="관심 종목을 불러오지 못했습니다."
            onRetry={() => void query.refetch()}
          />
        </div>
      </article>
    );
  }

  const items = query.data?.data ?? [];

  return (
    <article className={HOME_PANEL_CARD_CLASS} data-testid="home-notable-companies">
      <header className="shrink-0">
        <h2 className="text-lg font-semibold text-charcoal">주목 기업</h2>
        <p className="mt-1 text-sm text-muted-gray">
          네이버 금융 인기검색 기준 관심 종목
        </p>
      </header>

      {items.length === 0 ? (
        <p className="mt-6 flex flex-1 items-center justify-center text-sm text-muted-gray">
          표시할 종목이 없습니다.
        </p>
      ) : (
        <ol className="mt-4 min-h-0 flex-1 space-y-1 overflow-y-auto pr-1" aria-label="인기검색 순위">
          {items.map((item) => {
            const meta = [item.ticker, item.industry].filter(Boolean).join(" · ");
            const row = (
              <>
                <span
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-charcoal/[0.05] text-sm font-semibold text-charcoal"
                  aria-hidden
                >
                  {item.rank}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-charcoal">{item.name}</span>
                  <span className="block truncate text-xs text-muted-gray">{meta}</span>
                </span>
                <span
                  className={`shrink-0 text-xs tabular-nums ${
                    item.priceDirection === "down"
                      ? "text-red-700"
                      : item.priceDirection === "up"
                        ? "text-emerald-700"
                        : "text-muted-gray"
                  }`}
                >
                  {formatPrice(item.price)}
                </span>
              </>
            );

            return (
              <li key={`${item.rank}-${item.ticker}`}>
                {item.companyId != null ? (
                  <Link
                    to={`/companies/${item.companyId}`}
                    className="flex items-center gap-3 rounded-lg px-2 py-2.5 transition-colors hover:bg-charcoal/[0.03]"
                  >
                    {row}
                  </Link>
                ) : (
                  <div className="flex items-center gap-3 rounded-lg px-2 py-2.5 opacity-90">
                    {row}
                  </div>
                )}
              </li>
            );
          })}
        </ol>
      )}
    </article>
  );
}
