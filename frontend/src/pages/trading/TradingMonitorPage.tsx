import { useQuery } from "@tanstack/react-query";
import { fetchPaperMonitor } from "@/lib/paperApi";
import type { MonitorData, SignalLogItem, TradeItem } from "@/types/paper";

// ---- helpers ----
function fmtKst(iso: string | null): string {
  if (!iso) return "-";
  return new Date(iso).toLocaleString("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "2-digit",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function fmtNum(v: number | null, decimals = 0): string {
  if (v === null || v === undefined) return "-";
  return v.toLocaleString("ko-KR", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

// ---- Signal badge ----
const SIGNAL_CFG = {
  BUY:  { label: "매수", className: "bg-emerald-600 text-white" },
  SELL: { label: "매도", className: "bg-red-600 text-white" },
  HOLD: { label: "관망", className: "bg-gray-600 text-gray-200" },
} as const;

function SignalBadge({ signal }: { signal: string }) {
  const cfg = SIGNAL_CFG[signal as keyof typeof SIGNAL_CFG] ?? SIGNAL_CFG.HOLD;
  return (
    <span className={`rounded px-2 py-0.5 text-xs font-medium ${cfg.className}`}>
      {cfg.label}
    </span>
  );
}

// ---- Signal log table ----
function SignalLogTable({ signals }: { signals: SignalLogItem[] }) {
  if (signals.length === 0) {
    return (
      <p className="py-4 text-center text-sm text-gray-500">최근 신호 없음</p>
    );
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-white/10 text-left text-gray-400">
            <th className="px-3 py-2 font-medium">시각 (KST)</th>
            <th className="px-3 py-2 font-medium">종목</th>
            <th className="px-3 py-2 font-medium">신호</th>
            <th className="px-3 py-2 font-medium">체결</th>
            <th className="px-3 py-2 text-right font-medium">RSI14</th>
            <th className="px-3 py-2 text-right font-medium">SMA20</th>
            <th className="px-3 py-2 text-right font-medium">현재가</th>
          </tr>
        </thead>
        <tbody>
          {signals.map((s) => (
            <tr key={s.id} className="border-b border-white/5 last:border-0">
              <td className="px-3 py-2 text-gray-400">{fmtKst(s.ts)}</td>
              <td className="px-3 py-2 font-medium text-white">{s.symbol}</td>
              <td className="px-3 py-2"><SignalBadge signal={s.signal} /></td>
              <td className="px-3 py-2">
                {s.executed ? (
                  <span className="text-xs text-emerald-400">체결</span>
                ) : (
                  <span className="text-xs text-gray-500">미체결</span>
                )}
              </td>
              <td className="px-3 py-2 text-right text-gray-300">{fmtNum(s.rsi14, 1)}</td>
              <td className="px-3 py-2 text-right text-gray-300">{fmtNum(s.sma20, 0)}</td>
              <td className="px-3 py-2 text-right text-white">{fmtNum(s.price, 0)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ---- Today trades table ----
function TodayTradesTable({ trades }: { trades: TradeItem[] }) {
  if (trades.length === 0) {
    return (
      <p className="py-4 text-center text-sm text-gray-500">오늘 체결 내역 없음</p>
    );
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-white/10 text-left text-gray-400">
            <th className="px-3 py-2 font-medium">시각 (KST)</th>
            <th className="px-3 py-2 font-medium">종목</th>
            <th className="px-3 py-2 font-medium">구분</th>
            <th className="px-3 py-2 text-right font-medium">수량</th>
            <th className="px-3 py-2 text-right font-medium">체결가</th>
            <th className="px-3 py-2 text-right font-medium">손익</th>
          </tr>
        </thead>
        <tbody>
          {trades.map((t) => (
            <tr key={t.id} className="border-b border-white/5 last:border-0">
              <td className="px-3 py-2 text-gray-400">{fmtKst(t.tradedAt)}</td>
              <td className="px-3 py-2 font-medium text-white">{t.symbol}</td>
              <td className="px-3 py-2">
                <span className={`text-xs font-medium ${
                  t.side === "BUY" ? "text-emerald-400" : "text-red-400"
                }`}>
                  {t.side === "BUY" ? "매수" : "매도"}
                </span>
              </td>
              <td className="px-3 py-2 text-right text-gray-300">{fmtNum(t.qty, 0)}</td>
              <td className="px-3 py-2 text-right text-white">{fmtNum(t.price, 0)}</td>
              <td className="px-3 py-2 text-right">
                {t.pnl === null ? (
                  <span className="text-gray-500">-</span>
                ) : (
                  <span className={t.pnl >= 0 ? "text-emerald-400" : "text-red-400"}>
                    {t.pnl >= 0 ? "+" : ""}{fmtNum(t.pnl, 0)}
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ---- Page ----
export function TradingMonitorPage() {
  const { data, isLoading, isError, dataUpdatedAt } = useQuery<MonitorData>({
    queryKey: ["trading", "paper", "monitor"],
    queryFn: async () => {
      const res = await fetchPaperMonitor();
      return res.data as MonitorData;
    },
    refetchInterval: 30000,
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-white">동작 모니터링</h2>
          <p className="mt-1 text-sm text-gray-400">실시간 봇 상태 및 신호 로그 (30초 자동 갱신)</p>
        </div>
        {dataUpdatedAt > 0 && (
          <p className="text-xs text-gray-500">
            마지막 갱신: {fmtKst(new Date(dataUpdatedAt).toISOString())}
          </p>
        )}
      </div>

      {isLoading ? (
        <p className="text-sm text-gray-400">불러오는 중...</p>
      ) : isError ? (
        <p className="text-sm text-red-400">모니터 데이터를 불러오지 못했습니다.</p>
      ) : data ? (
        <>
          {/* Status row */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-2">
            <div className="rounded-lg border border-white/10 bg-gray-900/50 p-4">
              <p className="text-xs text-gray-400">시장 상태</p>
              <div className="mt-2 flex items-center gap-2">
                <span className={`h-2.5 w-2.5 rounded-full ${
                  data.marketStatus === "OPEN" ? "bg-emerald-400" : "bg-gray-500"
                }`} />
                <span className={`text-lg font-bold ${
                  data.marketStatus === "OPEN" ? "text-emerald-400" : "text-gray-400"
                }`}>
                  {data.marketStatus === "OPEN" ? "개장" : "폐장"}
                </span>
              </div>
            </div>
            <div className="rounded-lg border border-white/10 bg-gray-900/50 p-4">
              <p className="text-xs text-gray-400">스케줄러 최근 실행</p>
              <p className="mt-2 text-lg font-bold text-white">
                {data.schedulerLastRun ? fmtKst(data.schedulerLastRun) : "-"}
              </p>
            </div>
          </div>

          {/* Signal log */}
          <div className="rounded-lg border border-white/10 bg-gray-900/50">
            <div className="border-b border-white/10 px-4 py-3">
              <h3 className="text-sm font-semibold text-white">
                최근 신호 로그
                <span className="ml-2 text-xs font-normal text-gray-400">
                  (최대 50건)
                </span>
              </h3>
            </div>
            <SignalLogTable signals={data.recentSignals} />
          </div>

          {/* Today trades */}
          <div className="rounded-lg border border-white/10 bg-gray-900/50">
            <div className="border-b border-white/10 px-4 py-3">
              <h3 className="text-sm font-semibold text-white">
                오늘 체결 내역
                <span className="ml-2 text-xs font-normal text-gray-400">
                  ({data.todayTrades.length}건)
                </span>
              </h3>
            </div>
            <TodayTradesTable trades={data.todayTrades} />
          </div>
        </>
      ) : null}
    </div>
  );
}
