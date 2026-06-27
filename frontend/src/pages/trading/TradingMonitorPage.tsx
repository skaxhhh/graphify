import { useQuery } from "@tanstack/react-query";
import { fetchPaperMonitor } from "@/lib/paperApi";
import type { MonitorData, SignalLogItem, TradeItem } from "@/types/paper";
import {
  TradeBadge,
  TradeCard,
  TradePageState,
  TradeStatCard,
} from "@/components/trading/ui";

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

// ---- Signal log table ----
function SignalLogTable({ signals }: { signals: SignalLogItem[] }) {
  if (signals.length === 0) {
    return (
      <p className="py-4 text-center text-sm text-trade-muted font-trade-sans">
        최근 신호 없음
      </p>
    );
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-trade-hairline text-left text-trade-muted">
            <th className="px-3 py-2 font-medium font-trade-sans">시각 (KST)</th>
            <th className="px-3 py-2 font-medium font-trade-sans">종목</th>
            <th className="px-3 py-2 font-medium font-trade-sans">신호</th>
            <th className="px-3 py-2 font-medium font-trade-sans">체결</th>
            <th className="px-3 py-2 text-right font-medium font-trade-sans">RSI14</th>
            <th className="px-3 py-2 text-right font-medium font-trade-sans">SMA20</th>
            <th className="px-3 py-2 text-right font-medium font-trade-sans">현재가</th>
          </tr>
        </thead>
        <tbody>
          {signals.map((s) => (
            <tr key={s.id} className="border-b border-trade-hairline/50 last:border-0">
              <td className="px-3 py-2 text-trade-muted font-trade-mono">{fmtKst(s.ts)}</td>
              <td className="px-3 py-2 font-medium text-trade-body font-trade-sans">{s.symbol}</td>
              <td className="px-3 py-2">
                {s.signal === "BUY" ? (
                  <TradeBadge variant="up">매수</TradeBadge>
                ) : s.signal === "SELL" ? (
                  <TradeBadge variant="down">매도</TradeBadge>
                ) : (
                  <TradeBadge variant="draft">관망</TradeBadge>
                )}
              </td>
              <td className="px-3 py-2">
                {s.executed ? (
                  <span className="text-xs text-trade-up font-trade-sans">체결</span>
                ) : (
                  <span className="text-xs text-trade-muted font-trade-sans">미체결</span>
                )}
              </td>
              <td className="px-3 py-2 text-right text-trade-muted font-trade-mono">
                {fmtNum(s.rsi14, 1)}
              </td>
              <td className="px-3 py-2 text-right text-trade-muted font-trade-mono">
                {fmtNum(s.sma20, 0)}
              </td>
              <td className="px-3 py-2 text-right text-trade-body font-trade-mono">
                {fmtNum(s.price, 0)}
              </td>
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
      <p className="py-4 text-center text-sm text-trade-muted font-trade-sans">
        오늘 체결 내역 없음
      </p>
    );
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-trade-hairline text-left text-trade-muted">
            <th className="px-3 py-2 font-medium font-trade-sans">시각 (KST)</th>
            <th className="px-3 py-2 font-medium font-trade-sans">종목</th>
            <th className="px-3 py-2 font-medium font-trade-sans">구분</th>
            <th className="px-3 py-2 text-right font-medium font-trade-sans">수량</th>
            <th className="px-3 py-2 text-right font-medium font-trade-sans">체결가</th>
            <th className="px-3 py-2 text-right font-medium font-trade-sans">손익</th>
          </tr>
        </thead>
        <tbody>
          {trades.map((t) => (
            <tr key={t.id} className="border-b border-trade-hairline/50 last:border-0">
              <td className="px-3 py-2 text-trade-muted font-trade-mono">{fmtKst(t.tradedAt)}</td>
              <td className="px-3 py-2 font-medium text-trade-body font-trade-sans">{t.symbol}</td>
              <td className="px-3 py-2">
                <span
                  className={`text-xs font-medium font-trade-sans ${
                    t.side === "BUY" ? "text-trade-up" : "text-trade-down"
                  }`}
                >
                  {t.side === "BUY" ? "매수" : "매도"}
                </span>
              </td>
              <td className="px-3 py-2 text-right text-trade-muted font-trade-mono">
                {fmtNum(t.qty, 0)}
              </td>
              <td className="px-3 py-2 text-right text-trade-body font-trade-mono">
                {fmtNum(t.price, 0)}
              </td>
              <td className="px-3 py-2 text-right">
                {t.pnl === null ? (
                  <span className="text-trade-muted font-trade-mono">-</span>
                ) : (
                  <span
                    className={`font-trade-mono ${
                      t.pnl > 0
                        ? "text-trade-up"
                        : t.pnl < 0
                        ? "text-trade-down"
                        : "text-trade-muted"
                    }`}
                  >
                    {t.pnl >= 0 ? "+" : ""}
                    {fmtNum(t.pnl, 0)}
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

  const lastUpdated =
    dataUpdatedAt > 0
      ? new Date(dataUpdatedAt).toLocaleTimeString("ko-KR", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        })
      : null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-trade-on-dark font-trade-sans">
          동작 모니터링
        </h2>
        {lastUpdated && (
          <p className="text-xs font-trade-mono text-trade-muted">
            30초 자동 갱신 · {lastUpdated}
          </p>
        )}
      </div>

      {isLoading ? (
        <TradePageState variant="loading" className="h-[300px]" />
      ) : isError ? (
        <TradePageState
          variant="error"
          message="모니터 데이터를 불러오지 못했습니다."
        />
      ) : data ? (
        <>
          {/* Status row: market status + scheduler */}
          <div className="grid grid-cols-2 gap-4">
            {/* Market status card — custom content for dot indicator */}
            <TradeCard>
              <p className="text-xs text-trade-muted font-trade-sans">시장 상태</p>
              <div className="mt-2 flex items-center gap-2">
                <span
                  className={`h-2.5 w-2.5 rounded-full ${
                    data.marketStatus === "OPEN" ? "bg-trade-up" : "bg-trade-muted"
                  }`}
                />
                <span
                  className={`text-lg font-bold font-trade-mono ${
                    data.marketStatus === "OPEN" ? "text-trade-up" : "text-trade-muted"
                  }`}
                >
                  {data.marketStatus === "OPEN" ? "개장" : "폐장"}
                </span>
              </div>
            </TradeCard>

            {/* Scheduler last run */}
            <TradeStatCard
              label="스케줄러 최근 실행"
              value={data.schedulerLastRun ? fmtKst(data.schedulerLastRun) : "-"}
            />
          </div>

          {/* Signal log */}
          <div className="rounded-lg border border-trade-hairline bg-trade-surface overflow-hidden">
            <div className="border-b border-trade-hairline px-4 py-3">
              <h3 className="text-sm font-semibold text-trade-body font-trade-sans">
                최근 신호 로그
                <span className="ml-2 text-xs font-normal text-trade-muted">
                  (최대 50건)
                </span>
              </h3>
            </div>
            <SignalLogTable signals={data.recentSignals} />
          </div>

          {/* Today trades */}
          <div className="rounded-lg border border-trade-hairline bg-trade-surface overflow-hidden">
            <div className="border-b border-trade-hairline px-4 py-3">
              <h3 className="text-sm font-semibold text-trade-body font-trade-sans">
                오늘 체결 내역
                <span className="ml-2 text-xs font-normal text-trade-muted">
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
