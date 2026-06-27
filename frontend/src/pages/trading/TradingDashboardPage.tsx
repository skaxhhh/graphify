import { TradeBadge, TradeButton, TradeCard } from "@/components/trading/ui";

export function TradingDashboardPage() {
  return (
    <div className="space-y-6">
      {/* 페이지 헤더 */}
      <div className="flex items-center gap-3">
        <span className="inline-flex items-center rounded border border-trade-primary px-2 py-0.5 font-trade-mono text-xs font-semibold text-trade-primary">
          🚧 준비 중
        </span>
        <h2 className="font-trade-sans text-xl font-semibold text-trade-on-dark">LIVE 화면</h2>
      </div>

      {/* 서킷 브레이커 경고 배너 SLOT */}
      <div className="rounded-lg border border-trade-down/30 bg-trade-down-soft px-4 py-3">
        <p className="font-trade-sans text-sm font-medium text-trade-down">
          ⚠ 서킷 브레이커 경고 배너 자리 — API 연속 실패 시 평가 중단 (Phase 8 슬롯)
        </p>
      </div>

      {/* 메인 컨텐츠 플레이스홀더 */}
      <div className="rounded-lg border border-dashed border-trade-hairline p-8 text-center">
        <p className="font-trade-sans text-sm text-trade-body">
          PAPER 대시보드/거래 이력과 동형 + 실주문 상태 · 서킷 브레이커
        </p>
        <p className="mt-2 font-trade-sans text-xs text-trade-muted">
          Phase 8에서 실데이터화 · 현재 준비 중 플레이스홀더
        </p>
      </div>

      {/* Phase 7 슬롯 카드 — TradingView 연동 */}
      <TradeCard>
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <TradeBadge variant="info" className="border border-trade-info/30">TV</TradeBadge>
            <span className="font-trade-sans text-sm font-semibold text-trade-on-dark">
              TradingView 연동 (Phase 7 슬롯)
            </span>
          </div>
          <ul className="space-y-1.5 font-trade-sans text-sm text-trade-body">
            <li className="flex items-center gap-2">
              <span className="text-trade-muted">·</span>
              룰 목록 TV 배지
            </li>
            <li className="flex items-center gap-2">
              <span className="text-trade-muted">·</span>
              신호 소스 분기 (TV webhook / 내부 룰 엔진)
            </li>
            <li className="flex items-start gap-2">
              <span className="text-trade-muted">·</span>
              <span>
                webhook URL row —{" "}
                <span className="font-trade-mono text-xs text-trade-muted">
                  https://api.example.com/webhook/tv/···
                </span>{" "}
                <TradeButton variant="ghost" size="sm" disabled>
                  복사
                </TradeButton>
              </span>
            </li>
            <li className="flex items-center gap-2">
              <span className="text-trade-muted">·</span>
              사전 등록 종목 풀
            </li>
            <li className="flex items-center gap-2">
              <span className="text-trade-muted">·</span>
              <span className="text-trade-muted line-through">TradingView에서 보기</span>
              <span className="font-trade-sans text-xs text-trade-muted">(비활성)</span>
            </li>
          </ul>
          <p className="font-trade-sans text-xs text-trade-primary">LATER · coming soon</p>
        </div>
      </TradeCard>

      {/* Phase 8 슬롯 카드 — 승격 게이트 */}
      <TradeCard>
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <TradeBadge variant="warning">P8</TradeBadge>
            <span className="font-trade-sans text-sm font-semibold text-trade-on-dark">
              LIVE 승격 게이트 (Phase 8 슬롯)
            </span>
          </div>
          <ul className="space-y-1.5 font-trade-sans text-sm">
            <li className="flex items-center gap-2">
              <span className="text-trade-up font-semibold">✓</span>
              <span className="text-trade-body">토스 인증 완료</span>
            </li>
            <li className="flex items-center gap-2">
              <span className="text-trade-down font-semibold">✗</span>
              <span className="text-trade-muted">최소 5거래일 운영 (현재 3일)</span>
            </li>
          </ul>
          <div className="flex items-center gap-3">
            <TradeButton variant="primary" disabled>
              LIVE로 승격 (비활성)
            </TradeButton>
            <span className="font-trade-sans text-xs text-trade-muted">LATER</span>
          </div>
        </div>
      </TradeCard>
    </div>
  );
}
