// TradeRationaleRow — 백테스트/모의 거래 공유 근거 확장 행 컴포넌트 (06.5-05)
// 필드명 출처: 06.5-03-SUMMARY.md (side, exitReason, exitPct, conditions[].{expr,leftLabel,leftValue,op,rightLabel,rightValue,passed})
// 06.8-02: Reskinned to Binance dark tokens (no logic change, no string change)

export interface ConditionResult {
  expr: string;
  leftLabel: string;
  leftValue: number;
  op: string;
  rightLabel: string;
  rightValue: number;
  passed: boolean;
}

export interface TradeRationale {
  side: "BUY" | "SELL";
  exitReason: "TAKE_PROFIT" | "STOP_LOSS" | "INDICATOR" | null;
  exitPct: number | null;
  conditions: ConditionResult[];
}

/**
 * PaperHistoryPage의 rationaleJson은 indicatorSnapshot 전체를 담고 있어
 * { price, rsi14, sma20, rationale: TradeRationale } 구조다.
 * parseRationale()이 두 형태를 모두 처리한다.
 */
export function parseRationale(json: string | null): TradeRationale | null {
  if (!json) return null;
  try {
    const parsed = JSON.parse(json) as Record<string, unknown>;
    // indicatorSnapshot 래퍼 형태: { ..., rationale: { ... } }
    if (parsed.rationale && typeof parsed.rationale === "object") {
      return parsed.rationale as TradeRationale;
    }
    // 백테스트 직접 형태: { side, exitReason, exitPct, conditions }
    if (parsed.side) {
      return parsed as unknown as TradeRationale;
    }
    return null;
  } catch {
    return null;
  }
}

function exitLabel(rationale: TradeRationale): string | null {
  if (rationale.exitReason === "TAKE_PROFIT") {
    return `익절 (+${rationale.exitPct?.toFixed(1) ?? "?"}%)`;
  }
  if (rationale.exitReason === "STOP_LOSS") {
    return `손절 (${rationale.exitPct?.toFixed(1) ?? "?"}%)`;
  }
  if (rationale.exitReason === "INDICATOR") {
    return "지표 조건";
  }
  return null;
}

export function TradeRationaleRow({ rationale }: { rationale: TradeRationale | null }) {
  if (!rationale) {
    return (
      <div className="py-2 text-xs text-trade-muted">근거 정보 없음</div>
    );
  }

  const label = exitLabel(rationale);
  const isProfit = rationale.exitReason === "TAKE_PROFIT";
  const isLoss = rationale.exitReason === "STOP_LOSS";

  return (
    <div className="space-y-1.5 py-1 text-xs">
      {label && (
        <div
          className={
            isProfit
              ? "font-medium text-trade-up"
              : isLoss
              ? "font-medium text-trade-down"
              : "font-medium text-trade-body"
          }
        >
          청산 사유: {label}
        </div>
      )}
      {rationale.conditions.length === 0 && !label && (
        <div className="text-trade-muted">조건 정보 없음</div>
      )}
      {rationale.conditions.map((c, i) => (
        <div key={i} className="flex items-center gap-2">
          {/* ✓/✗ trigger markers — trade-up / trade-down */}
          <span className={c.passed ? "text-trade-up" : "text-trade-down"}>
            {c.passed ? "✓" : "✗"}
          </span>
          <span className="text-trade-body">
            {c.leftLabel}={c.leftValue.toFixed(2)} {c.op} {c.rightValue.toFixed(2)}
          </span>
          <span className="text-xs text-trade-muted">({c.expr})</span>
        </div>
      ))}
    </div>
  );
}
