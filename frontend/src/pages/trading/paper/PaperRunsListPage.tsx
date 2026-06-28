import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { fetchPaperRuns } from "@/lib/paperApi";
import type { RunSummary } from "@/types/paper";
import {
  TradeBadge,
  TradePageState,
  TradeTable,
  TradeTableHeader,
  TradeTableRow,
} from "@/components/trading/ui";

// ── helpers ──────────────────────────────────────────────────────────────────
const fmtMoney = (n: number) =>
  n.toLocaleString("ko-KR", { maximumFractionDigits: 0 });
const fmtPct = (n: number) =>
  `${n >= 0 ? "+" : ""}${n.toFixed(2)}%`;
const fmtDate = (iso: string) => {
  const d = new Date(iso);
  return `${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
};

function RunsTableHeader() {
  return (
    <TradeTableHeader>
      <div className="grid grid-cols-[2fr_90px_2fr_1.5fr_90px_100px_60px_110px_24px] gap-2 items-center font-trade-sans text-xs text-trade-muted uppercase tracking-wide">
        <span>전략명</span>
        <span>상태</span>
        <span>실행 기간</span>
        <span>유니버스</span>
        <span className="text-right">수익률</span>
        <span className="text-right">실현손익</span>
        <span className="text-right">거래</span>
        <span className="text-right">최종 자산</span>
        <span />
      </div>
    </TradeTableHeader>
  );
}

function RunsTableRow({
  run,
  onClick,
}: {
  run: RunSummary;
  onClick: () => void;
}) {
  const universeLabel =
    run.universe.length === 0
      ? "—"
      : run.universe.length <= 2
      ? run.universe.join(", ")
      : `${run.universe[0]} 외 ${run.universe.length - 1}`;

  const periodLabel =
    fmtDate(run.startedAt) +
    " ~ " +
    (run.endedAt ? fmtDate(run.endedAt) : "진행중");

  return (
    <TradeTableRow onClick={onClick}>
      <div className="grid grid-cols-[2fr_90px_2fr_1.5fr_90px_100px_60px_110px_24px] gap-2 items-center">
        <span className="font-semibold text-trade-body font-trade-sans text-sm truncate">
          {run.ruleName}
        </span>
        <span>
          {run.status === "RUNNING" ? (
            <TradeBadge variant="up">
              <span className="inline-block w-1.5 h-1.5 rounded-full bg-trade-up mr-1" />
              실행중
            </TradeBadge>
          ) : (
            <TradeBadge variant="draft">종료</TradeBadge>
          )}
        </span>
        <span className="text-xs text-trade-muted font-trade-mono">
          {periodLabel}
        </span>
        <span className="text-xs text-trade-muted font-trade-sans truncate">
          {universeLabel}
        </span>
        <span
          className={`text-right text-sm font-trade-mono font-semibold ${
            run.returnPct >= 0 ? "text-trade-up" : "text-trade-down"
          }`}
        >
          {fmtPct(run.returnPct)}
        </span>
        <span
          className={`text-right text-sm font-trade-mono ${
            run.realizedPnl >= 0 ? "text-trade-up" : "text-trade-down"
          }`}
        >
          {run.realizedPnl >= 0 ? "+" : ""}
          {fmtMoney(run.realizedPnl)}
        </span>
        <span className="text-right text-sm font-trade-mono text-trade-body">
          {run.tradeCount}
        </span>
        <span className="text-right text-sm font-trade-mono text-trade-body">
          {fmtMoney(run.finalEquity)}
        </span>
        <span className="text-right text-trade-muted text-xs">›</span>
      </div>
    </TradeTableRow>
  );
}

// ── page ─────────────────────────────────────────────────────────────────────
export function PaperRunsListPage() {
  const navigate = useNavigate();
  const [runs, setRuns] = useState<RunSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    fetchPaperRuns()
      .then((res) => {
        setRuns(res.data ?? []);
        setLoading(false);
      })
      .catch(() => {
        setError("실행 이력을 불러오지 못했어요.");
        setLoading(false);
      });
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="min-h-screen bg-trade-bg px-6 py-6 font-trade-sans">
      {/* header */}
      <div className="mb-4">
        <h1 className="text-xl font-bold text-trade-body">실행 이력</h1>
        <p className="mt-1 text-sm text-trade-muted">
          전략을 실행(start)한 기록. 실행 중 · 종료 모두 표시. 행을 누르면
          상세(3탭)로 이동.
        </p>
      </div>

      {/* filter bar */}
      <div className="flex items-center gap-2 mb-4 flex-wrap">
        <span className="inline-flex items-center gap-1.5 rounded border border-trade-hairline bg-trade-surface px-3 py-1.5 text-xs font-medium text-trade-muted">
          기간: 최근 30일 ▾
        </span>
        <span className="inline-flex items-center gap-1.5 rounded border border-trade-hairline bg-trade-surface px-3 py-1.5 text-xs font-medium text-trade-muted">
          상태: 전체 ▾
        </span>
        {!loading && !error && (
          <span className="ml-auto text-xs text-trade-muted font-trade-mono">
            총 {runs.length}건
          </span>
        )}
      </div>

      {/* content */}
      {loading ? (
        <TradePageState variant="loading" className="w-full" />
      ) : error ? (
        <TradePageState
          variant="error"
          title="불러오기 실패"
          message={error}
          onRetry={load}
        />
      ) : runs.length === 0 ? (
        <TradePageState
          variant="empty"
          title="아직 실행한 전략이 없어요"
          message="전략 운영 화면에서 전략을 시작해 보세요."
        />
      ) : (
        <div className="overflow-x-auto">
          <TradeTable className="min-w-[900px]">
            <RunsTableHeader />
            {runs.map((run) => (
              <RunsTableRow
                key={run.runId}
                run={run}
                onClick={() =>
                  navigate(`/trading/paper/runs/${run.runId}`)
                }
              />
            ))}
          </TradeTable>
        </div>
      )}
    </div>
  );
}
