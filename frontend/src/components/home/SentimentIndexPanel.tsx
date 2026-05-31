import { useQuery } from "@tanstack/react-query";
import { HOME_PANEL_CARD_CLASS } from "@/components/home/homePanelStyles";
import { ErrorBanner } from "@/components/shared/ErrorBanner";
import { SkeletonBlock } from "@/components/shared/SkeletonBlock";
import { fetchMarketSentiment } from "@/lib/homeApi";
import type { MarketSentimentSnapshot, SentimentZone } from "@/types/home";

const ZONE_TEXT_CLASS: Record<SentimentZone, string> = {
  EXTREME_FEAR: "text-red-700",
  FEAR: "text-orange-700",
  NEUTRAL: "text-charcoal",
  GREED: "text-emerald-700",
  EXTREME_GREED: "text-emerald-800",
};

function indicatorBarColor(score: number): string {
  if (score >= 60) return "bg-emerald-500";
  if (score >= 45) return "bg-amber-400";
  return "bg-red-500";
}

function dataSourceLabel(dataSource: string): string {
  if (dataSource === "CNN_OFFICIAL") return "CNN";
  if (dataSource === "YAHOO_PROXY") return "Yahoo 추정";
  return dataSource;
}

function SentimentGauge({ snapshot, title }: { snapshot: MarketSentimentSnapshot; title: string }) {
  const markerLeft = `${Math.min(100, Math.max(0, snapshot.score))}%`;
  const showVix = snapshot.vix != null;

  return (
    <section className="rounded-lg border border-warm-border/80 bg-light-cream/30 p-3">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-sm font-semibold text-charcoal">{title}</h3>
        <span className={`text-xs font-medium ${ZONE_TEXT_CLASS[snapshot.zone]}`}>
          {snapshot.zoneLabel}
        </span>
      </div>

      <div className="mt-2 flex items-end justify-between gap-2">
        <p className={`text-2xl font-bold tabular-nums ${ZONE_TEXT_CLASS[snapshot.zone]}`}>
          {snapshot.score.toFixed(0)}
        </p>
        <div className="text-right text-[10px] text-muted-gray">
          <p>{snapshot.market}</p>
          <p>{dataSourceLabel(snapshot.dataSource)}</p>
        </div>
      </div>

      {showVix ? (
        <div className="mt-2 flex items-baseline gap-3 rounded-md bg-charcoal/5 px-2 py-1.5 text-xs">
          <span className="font-medium text-charcoal">VIX</span>
          <span className="tabular-nums font-semibold text-charcoal">
            {snapshot.vix!.toFixed(2)}
          </span>
          {snapshot.vixMa50 != null ? (
            <span className="text-muted-gray">
              MA50 {snapshot.vixMa50.toFixed(2)}
            </span>
          ) : null}
        </div>
      ) : null}

      <div className="relative mt-2 h-2.5 w-full overflow-hidden rounded-full">
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(90deg, #b91c1c 0%, #f97316 25%, #a8a29e 50%, #34d399 75%, #047857 100%)",
          }}
          aria-hidden
        />
        <div
          className="absolute top-1/2 h-4 w-0.5 -translate-x-1/2 -translate-y-1/2 rounded-sm bg-charcoal shadow"
          style={{ left: markerLeft }}
          aria-hidden
        />
      </div>

      <ul className="mt-3 max-h-[140px] space-y-1.5 overflow-y-auto pr-0.5" aria-label={`${title} 지표`}>
        {snapshot.indicators.map((indicator) => (
          <li key={`${title}-${indicator.id}`}>
            <div className="flex items-center justify-between gap-2 text-[10px]">
              <span className="truncate text-charcoal">{indicator.name}</span>
              <span className="shrink-0 tabular-nums text-muted-gray">
                {indicator.score.toFixed(0)}
              </span>
            </div>
            <div className="mt-1 h-1 overflow-hidden rounded-full bg-charcoal/10">
              <div
                className={`h-full rounded-full ${indicatorBarColor(indicator.score)}`}
                style={{ width: `${Math.min(100, Math.max(0, indicator.score))}%` }}
              />
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

export function SentimentIndexPanel() {
  const query = useQuery({
    queryKey: ["home", "market-sentiment"],
    queryFn: fetchMarketSentiment,
    staleTime: 5 * 60 * 1000,
  });

  if (query.isLoading) {
    return (
      <article className={HOME_PANEL_CARD_CLASS}>
        <SkeletonBlock className="h-6 w-28" />
        <SkeletonBlock className="mt-4 h-24 w-full" />
        <SkeletonBlock className="mt-3 h-24 w-full" />
      </article>
    );
  }

  if (query.isError) {
    return (
      <article className={HOME_PANEL_CARD_CLASS}>
        <h2 className="text-lg font-semibold text-charcoal">공탐 지수</h2>
        <div className="mt-4 min-h-0 flex-1">
          <ErrorBanner
            message="공탐 지수를 불러오지 못했습니다."
            onRetry={() => void query.refetch()}
          />
        </div>
      </article>
    );
  }

  const data = query.data?.data;
  if (!data) {
    return null;
  }

  return (
    <article className={HOME_PANEL_CARD_CLASS} data-testid="home-sentiment-index">
      <header className="shrink-0">
        <h2 className="text-lg font-semibold text-charcoal">공탐 지수</h2>
        <p className="mt-1 text-xs text-muted-gray">
          코스피(Yahoo 추정) · 미국(CNN 공식)
        </p>
      </header>

      <div className="mt-3 min-h-0 flex-1 space-y-3 overflow-y-auto pr-1">
        {data.kospi ? <SentimentGauge snapshot={data.kospi} title="코스피" /> : null}
        {data.nasdaq ? (
          <SentimentGauge snapshot={data.nasdaq} title="미국 (CNN)" />
        ) : null}
        {!data.kospi && !data.nasdaq ? (
          <p className="text-sm text-muted-gray">표시할 지수가 없습니다.</p>
        ) : null}
      </div>

      <p className="mt-2 shrink-0 text-[10px] text-muted-gray/80">
        10분 캐시 · CNN / Yahoo
        {data.asOf ? ` · 조회 ${new Date(data.asOf).toLocaleString("ko-KR")}` : ""}
      </p>
    </article>
  );
}
