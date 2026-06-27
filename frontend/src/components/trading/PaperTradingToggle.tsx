import { useState } from "react";
import { useTradingStore } from "@/stores/tradingStore";
import { updateTradingMode } from "@/lib/tradingApi";
import type { TradingMode } from "@/types/user";
import { TradeButton } from "@/components/trading/ui/TradeButton";

// RESKIN ONLY — applyMode / optimistic-update / rollback / confirmLive flow preserved exactly
export function PaperTradingToggle() {
  const mode = useTradingStore((s) => s.mode);
  const setMode = useTradingStore((s) => s.setMode);
  const [pending, setPending] = useState(false);
  const [confirmLive, setConfirmLive] = useState(false);

  const isPaper = mode === "PAPER";

  const applyMode = async (next: TradingMode) => {
    const prev = mode;
    setMode(next); // 낙관적 업데이트
    setPending(true);
    try {
      await updateTradingMode(next);
    } catch {
      setMode(prev); // 실패 시 롤백
    } finally {
      setPending(false);
    }
  };

  const handleToggle = () => {
    if (pending) return;
    if (isPaper) {
      // PAPER -> LIVE: 위험 동작, 확인 필요
      setConfirmLive(true);
    } else {
      // LIVE -> PAPER: 안전 방향, 즉시 적용
      void applyMode("PAPER");
    }
  };

  return (
    <div className="px-3 py-3 font-trade-sans">
      {/* D8: 세그먼트 토글 — 모의/실거래 */}
      <div className="bg-trade-bg rounded-lg p-1 flex gap-1 mb-3">
        <button
          type="button"
          onClick={() => !isPaper && !pending && void applyMode("PAPER")}
          disabled={pending}
          className={`flex-1 rounded-md py-1.5 text-sm font-semibold transition-colors disabled:opacity-50 ${
            isPaper
              ? "bg-trade-primary text-trade-ink"
              : "bg-transparent text-trade-muted hover:text-trade-body"
          }`}
        >
          모의
        </button>
        <button
          type="button"
          onClick={handleToggle}
          disabled={pending}
          aria-label="실거래 모드로 전환"
          className={`flex-1 rounded-md py-1.5 text-sm font-semibold transition-colors disabled:opacity-50 ${
            !isPaper
              ? "bg-trade-primary text-trade-ink"
              : "bg-transparent text-trade-muted hover:text-trade-body"
          }`}
        >
          실거래
        </button>
      </div>

      {/* 현재 모드 상태 텍스트 */}
      <p className={`text-xs text-center ${isPaper ? "text-trade-up" : "text-trade-down"}`}>
        {isPaper ? "모의투자 모드" : "실거래 중"}
      </p>

      {/* PAPER → LIVE 위험 확인 모달 (D8: 옐로우 확인 CTA) */}
      {confirmLive ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <button
            type="button"
            className="absolute inset-0 bg-black/60"
            aria-label="닫기"
            onClick={() => setConfirmLive(false)}
          />
          <div className="relative w-full max-w-sm rounded-xl border border-trade-hairline bg-trade-surface p-6 shadow-2xl">
            <div className="flex items-center gap-3 mb-3">
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-trade-down-soft">
                <span className="text-trade-down font-bold text-base">!</span>
              </span>
              <h2 className="text-base font-semibold text-trade-on-dark font-trade-sans">
                실거래 모드로 전환
              </h2>
            </div>
            <p className="text-sm text-trade-muted font-trade-sans leading-relaxed">
              실거래(LIVE) 모드에서는 토스증권 실계좌로{" "}
              <span className="text-trade-down font-semibold">실제 주문이 발행</span>됩니다.
              검증된 룰만 운영하세요. 계속하시겠습니까?
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <TradeButton
                variant="secondary"
                size="sm"
                onClick={() => setConfirmLive(false)}
              >
                취소
              </TradeButton>
              <TradeButton
                variant="primary"
                size="sm"
                disabled={pending}
                onClick={() => {
                  setConfirmLive(false);
                  void applyMode("LIVE");
                }}
              >
                실거래로 전환
              </TradeButton>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
