import type { CompanyMarketTechnical, MaAlignment } from "@/types/marketTechnical";

interface CompanyTechnicalPanelProps {
  data: CompanyMarketTechnical | undefined;
  loading: boolean;
  errorMessage?: string | null;
}

const ALIGNMENT_LABEL: Record<MaAlignment, string> = {
  BULLISH_ALIGN: "정배열 (60·120·240 상승 추세)",
  BEARISH_ALIGN: "역배열 (60·120·240 하락 추세)",
  MIXED: "혼조 (정·역배열 아님)",
};

const SOURCE_LABEL: Record<string, string> = {
  NAVER: "네이버 금융",
  YAHOO: "Yahoo Finance",
  KRX: "KRX Open API",
};

function formatPrice(value: number | null | undefined, currency: string | null): string {
  if (value == null) return "—";
  const formatted = value.toLocaleString("ko-KR", { maximumFractionDigits: 0 });
  return currency === "KRW" ? `${formatted}원` : `${formatted} ${currency ?? ""}`.trim();
}

function formatPercent(value: number | null | undefined): string {
  if (value == null) return "—";
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(2)}%`;
}

function formatMa(value: number | null | undefined): string {
  if (value == null) return "—";
  return value.toLocaleString("ko-KR", { maximumFractionDigits: 0 });
}

function formatDiffFromMa(price: number | null | undefined, ma: number | null | undefined): string {
  if (price == null || ma == null) return "";
  const diff = price - ma;
  const sign = diff > 0 ? "+" : "";
  return ` (${sign}${diff.toLocaleString("ko-KR", { maximumFractionDigits: 0 })})`;
}

export function CompanyTechnicalPanel({ data, loading, errorMessage }: CompanyTechnicalPanelProps) {
  if (loading) {
    return (
      <section
        className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
        data-testid="company-technical-loading"
        aria-busy="true"
      >
        <h2 className="text-sm font-semibold text-charcoal">시장·기술 지표</h2>
        <p className="mt-3 text-sm text-muted-gray">시세 데이터를 불러오는 중…</p>
      </section>
    );
  }

  if (errorMessage) {
    return (
      <section className="rounded-xl border border-dashed border-warm-border p-5 md:p-6">
        <h2 className="text-sm font-semibold text-charcoal">시장·기술 지표</h2>
        <p className="mt-2 text-sm text-muted-gray">{errorMessage}</p>
      </section>
    );
  }

  if (!data) {
    return null;
  }

  const changeUp = (data.changePercent ?? 0) >= 0;
  const quoteTimeKst =
    data.quoteTime &&
    new Date(data.quoteTime).toLocaleString("ko-KR", {
      timeZone: "Asia/Seoul",
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  return (
    <section
      className="rounded-xl border border-warm-border bg-cream p-5 md:p-6"
      data-testid="company-technical-section"
    >
      <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-sm font-semibold text-charcoal">시장·기술 지표</h2>
        <span className="text-xs text-muted-gray">{data.yahooSymbol}</span>
      </div>

      <p className="text-xs font-medium text-muted-gray" data-testid="company-technical-price-label">
        {data.priceLabel}
        {data.tradingDate ? ` · 거래일 ${data.tradingDate}` : ""}
      </p>
      <div className="mt-1 flex flex-wrap items-end gap-3">
        <p className="text-2xl font-semibold tabular-nums text-charcoal">
          {formatPrice(data.price, data.currency)}
        </p>
        <p
          className={`text-sm font-medium tabular-nums ${
            changeUp ? "text-emerald-700" : "text-red-700"
          }`}
          data-testid="company-technical-change"
        >
          {formatPercent(data.changePercent)}
        </p>
      </div>

      <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        <div>
          <dt className="text-muted-gray">추세 (60·120·240)</dt>
          <dd className="mt-0.5 font-medium text-charcoal" data-testid="company-technical-alignment">
            {ALIGNMENT_LABEL[data.maAlignment]}
          </dd>
        </div>
        <div>
          <dt className="text-muted-gray">일봉 RSI (14)</dt>
          <dd className="mt-0.5 font-medium tabular-nums text-charcoal" data-testid="company-technical-rsi">
            {data.rsi14 != null ? data.rsi14.toFixed(1) : "—"}
          </dd>
        </div>
        <div>
          <dt className="text-muted-gray">초단기 (5일선)</dt>
          <dd className="mt-0.5 font-medium text-charcoal">
            {data.ma5 == null
              ? "판별 불가"
              : data.shortTermRise5
                ? "상승 (현재가 > 5일선)"
                : "하락 (현재가 < 5일선)"}
            <span className="ml-1 text-xs text-muted-gray">
              MA5 {formatMa(data.ma5)}
              {formatDiffFromMa(data.price, data.ma5)}
            </span>
          </dd>
        </div>
        <div>
          <dt className="text-muted-gray">단기 (20일선)</dt>
          <dd className="mt-0.5 font-medium text-charcoal">
            {data.ma20 == null
              ? "판별 불가"
              : data.price != null && data.price > data.ma20
                ? "상승 (현재가 > 20일선)"
                : "하락 (현재가 < 20일선)"}
            <span className="ml-1 text-xs text-muted-gray">
              MA20 {formatMa(data.ma20)}
              {formatDiffFromMa(data.price, data.ma20)}
            </span>
          </dd>
        </div>
      </dl>

      <p className="mt-4 text-xs text-muted-gray">
        MA60 {formatMa(data.ma60)} · MA120 {formatMa(data.ma120)} · MA240 {formatMa(data.ma240)}
      </p>
      <p className="mt-1 text-[11px] text-muted-gray/80" data-testid="company-technical-quote-time">
        {quoteTimeKst ? `시세 기준 ${quoteTimeKst} (KST)` : ""}
        {data.previousClose != null
          ? ` · 전일 종가 ${formatMa(data.previousClose)}원`
          : ""}
        {(data.priceSource || data.historySource) && (
          <>
            {" · "}장중 {SOURCE_LABEL[data.priceSource ?? ""] ?? data.priceSource}
            {" · "}지표 {SOURCE_LABEL[data.historySource ?? ""] ?? data.historySource}
          </>
        )}
        {" · "}조회 {new Date(data.asOf).toLocaleString("ko-KR")}
      </p>
    </section>
  );
}
