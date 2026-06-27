import { TradeCard } from "@/components/trading/ui";

export function TradingHistoryPage() {
  return (
    <div className="space-y-6">
      {/* 페이지 헤더 */}
      <div className="flex items-center gap-3">
        <span className="inline-flex items-center rounded border border-trade-primary px-2 py-0.5 font-trade-mono text-xs font-semibold text-trade-primary">
          🚧 준비 중
        </span>
        <h2 className="font-trade-sans text-xl font-semibold text-trade-on-dark">LIVE 거래 이력</h2>
      </div>

      {/* 메인 컨텐츠 플레이스홀더 */}
      <TradeCard>
        <div className="rounded-lg border border-dashed border-trade-hairline p-8 text-center">
          <p className="font-trade-sans text-sm text-trade-body">LIVE 거래 이력</p>
          <p className="mt-2 font-trade-sans text-xs text-trade-muted">
            Phase 8에서 실데이터화 · 현재 준비 중 플레이스홀더
          </p>
        </div>
      </TradeCard>
    </div>
  );
}
