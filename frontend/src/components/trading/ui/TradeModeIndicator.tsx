// D8: reads useTradingStore mode; renders the wireframe "mode indicator" card.
// Placed at the top of the sidebar (below header) per D8.
import { useTradingStore } from "@/stores/tradingStore";

export function TradeModeIndicator() {
  const mode = useTradingStore((s) => s.mode);
  const isPaper = mode === "PAPER";

  return (
    <div className="bg-trade-surface rounded-lg p-3 mb-4 font-trade-sans">
      <div className="flex items-center gap-2">
        <span
          className={`inline-block h-2 w-2 rounded-full ${
            isPaper ? "bg-trade-primary" : "bg-trade-down"
          }`}
        />
        <span className="text-sm font-semibold text-trade-body">
          {isPaper ? "PAPER 모드" : "실거래 모드"}
        </span>
      </div>
      <p className="mt-1 text-xs text-trade-muted">
        {isPaper ? "모의 투자 · 가상 체결" : "실계좌 · 실주문"}
      </p>
    </div>
  );
}
