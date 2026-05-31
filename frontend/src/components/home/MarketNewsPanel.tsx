import { useQuery } from "@tanstack/react-query";
import { HOME_PANEL_CARD_CLASS } from "@/components/home/homePanelStyles";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { fetchMarketNews } from "@/lib/homeApi";
import { formatRelativeTime } from "@/lib/formatRelativeTime";

export function MarketNewsPanel() {
  const query = useQuery({
    queryKey: ["home", "market-news"],
    queryFn: () => fetchMarketNews(12),
  });

  if (query.isLoading) {
    return (
      <article className={HOME_PANEL_CARD_CLASS}>
        <SkeletonBlock className="h-6 w-32 shrink-0" />
        <div className="mt-4 min-h-0 flex-1 space-y-3 overflow-y-auto">
          {Array.from({ length: 5 }).map((_, i) => (
            <SkeletonBlock key={i} className="h-20 w-full" />
          ))}
        </div>
      </article>
    );
  }

  if (query.isError) {
    return (
      <article className={HOME_PANEL_CARD_CLASS}>
        <h2 className="shrink-0 text-lg font-semibold text-charcoal">시장 뉴스</h2>
        <div className="mt-4 min-h-0 flex-1">
          <ErrorBanner
            message="뉴스를 불러오지 못했습니다."
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
        <h2 className="text-lg font-semibold text-charcoal">시장 뉴스</h2>
        <p className="mt-1 text-sm text-muted-gray">
          한국 경제·비즈니스 뉴스를 실시간 수집해 표시합니다. (15분마다 갱신)
        </p>
      </header>

      {items.length === 0 ? (
        <p className="mt-6 flex flex-1 items-center justify-center text-sm text-muted-gray">
          표시할 뉴스가 없습니다.
        </p>
      ) : (
        <ul
          className="mt-4 min-h-0 flex-1 space-y-3 overflow-y-auto pr-1"
          aria-label="시장 뉴스 목록"
        >
          {items.map((item) => {
            const meta = [item.companyName, item.ticker].filter(Boolean).join(" · ");
            const body = (
              <>
                <div className="flex flex-wrap items-center gap-2 text-xs text-muted-gray">
                  <span>{item.sourceName}</span>
                  <span aria-hidden>·</span>
                  <time dateTime={item.publishedAt}>
                    {formatRelativeTime(item.publishedAt)}
                  </time>
                </div>
                <h3 className="mt-1 text-sm font-medium leading-snug text-charcoal">
                  {item.title}
                </h3>
                {meta ? (
                  <p className="mt-0.5 text-xs text-muted-gray">{meta}</p>
                ) : null}
                <p className="mt-1 line-clamp-2 text-sm leading-relaxed text-muted-gray">
                  {item.summary}
                </p>
              </>
            );

            return (
              <li
                key={item.id}
                className="rounded-lg border border-warm-border bg-light-cream/30 p-3"
              >
                {item.sourceUrl ? (
                  <a
                    href={item.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block transition-opacity hover:opacity-80"
                  >
                    {body}
                  </a>
                ) : (
                  <div>{body}</div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </article>
  );
}
