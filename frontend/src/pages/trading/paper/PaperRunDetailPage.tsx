import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  fetchRunDashboard,
  fetchRunHistory,
  fetchRunReport,
} from "@/lib/paperApi";
import type {
  RunDashboard,
  PaperTradeHistoryItem,
  ReportData,
  PaperPositionItem,
} from "@/types/paper";
import type { BacktestEquityPoint } from "@/types/trading";
import { EquityCurveChart } from "@/components/backtest/EquityCurveChart";
import {
  TradeRationaleRow,
  parseRationale,
} from "@/components/trading/TradeRationaleRow";
import {
  TradeBadge,
  TradeCard,
  TradePageState,
  TradeStatCard,
  TradeTable,
  TradeTableHeader,
  TradeTableRow,
} from "@/components/trading/ui";

// ── helpers ──────────────────────────────────────────────────────────────────
type PeriodMode = "SINGLE_RUN" | "RULE_AGGREGATE";
type TabId = "dashboard" | "history" | "report";

const fmtMoney = (n: number) =>
  n.toLocaleString("ko-KR", { maximumFractionDigits: 0 }) + "원";
const fmtPnl = (n: number) =>
  `${n >= 0 ? "+" : ""}${n.toLocaleString("ko-KR", { maximumFractionDigits: 0 })}원`;
const fmtPct = (n: number) => `${n >= 0 ? "+" : ""}${n.toFixed(2)}%`;
const fmtStat = (n: number) =>
  Number.isFinite(n) ? n.toFixed(2) : "—";

// ── Dashboard tab ─────────────────────────────────────────────────────────────
function DashboardTab({ runId }: { runId: number }) {
  const [data, setData] = useState<RunDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    fetchRunDashboard(runId)
      .then((res) => {
        setData(res.data ?? null);
        setLoading(false);
      })
      .catch(() => {
        setError("대시보드를 불러오지 못했어요.");
        setLoading(false);
      });
  };

  useEffect(() => {
    load();
  }, [runId]);

  if (loading) return <TradePageState variant="loading" />;
  if (error)
    return (
      <TradePageState
        variant="error"
        title="불러오기 실패"
        message={error}
        onRetry={load}
      />
    );
  if (!data)
    return (
      <TradePageState variant="empty" title="데이터가 없어요" />
    );

  return (
    <div className="space-y-4">
      {/* account-wide notice */}
      <div className="rounded-lg border border-trade-hairline bg-trade-elevated px-4 py-3 text-xs text-trade-muted font-trade-sans">
        단일 계좌 공유 — 총 자산·가용 현금은 계좌 전체 기준입니다. 실현손익·미실현손익·포지션은 이 실행의 기여분입니다.
      </div>

      {/* 4 stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <TradeStatCard label="총 자산" value={fmtMoney(data.totalEquity)} />
        <TradeStatCard label="가용 현금" value={fmtMoney(data.availableCash)} />
        <TradeStatCard
          label="실현 손익"
          value={fmtPnl(data.realizedPnl)}
          valueColor={data.realizedPnl >= 0 ? "up" : "down"}
        />
        <TradeStatCard
          label="미실현 손익"
          value={fmtPnl(data.unrealizedPnl)}
          valueColor={data.unrealizedPnl >= 0 ? "up" : "down"}
        />
      </div>

      {/* positions */}
      <TradeCard title="오픈 포지션">
        {data.positions.length === 0 ? (
          <TradePageState
            variant="empty"
            title="오픈 포지션 없음"
            className="border-0 bg-transparent"
          />
        ) : (
          <TradeTable>
            <TradeTableHeader>
              <div className="grid grid-cols-[2fr_1fr_1fr_1fr_1fr_1fr] gap-2 text-xs text-trade-muted uppercase tracking-wide font-trade-sans">
                <span>종목</span>
                <span className="text-right">수량</span>
                <span className="text-right">평균단가</span>
                <span className="text-right">현재가</span>
                <span className="text-right">평가금액</span>
                <span className="text-right">미실현손익</span>
              </div>
            </TradeTableHeader>
            {data.positions.map((pos: PaperPositionItem) => (
              <TradeTableRow key={pos.symbol}>
                <div className="grid grid-cols-[2fr_1fr_1fr_1fr_1fr_1fr] gap-2 items-center text-sm">
                  <div>
                    <span className="font-semibold text-trade-body font-trade-sans">
                      {pos.companyName ?? pos.symbol}
                    </span>
                    <span className="ml-2 text-xs text-trade-muted font-trade-mono">
                      {pos.symbol}
                    </span>
                  </div>
                  <span className="text-right font-trade-mono text-trade-body">
                    {pos.qty}
                  </span>
                  <span className="text-right font-trade-mono text-trade-body">
                    {pos.avgPrice.toLocaleString("ko-KR")}
                  </span>
                  <span className="text-right font-trade-mono text-trade-body">
                    {pos.markPrice.toLocaleString("ko-KR")}
                  </span>
                  <span className="text-right font-trade-mono text-trade-body">
                    {pos.marketValue.toLocaleString("ko-KR")}
                  </span>
                  <span
                    className={`text-right font-trade-mono font-semibold ${
                      pos.unrealizedPnl >= 0
                        ? "text-trade-up"
                        : "text-trade-down"
                    }`}
                  >
                    {pos.unrealizedPnl >= 0 ? "+" : ""}
                    {pos.unrealizedPnl.toLocaleString("ko-KR")}
                  </span>
                </div>
              </TradeTableRow>
            ))}
          </TradeTable>
        )}
      </TradeCard>
    </div>
  );
}

// ── History tab ───────────────────────────────────────────────────────────────
function HistoryTab({
  runId,
  mode,
  from,
  to,
}: {
  runId: number;
  mode: PeriodMode;
  from: string;
  to: string;
}) {
  const [items, setItems] = useState<PaperTradeHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    fetchRunHistory(
      runId,
      mode === "RULE_AGGREGATE" ? "RULE_AGGREGATE" : undefined,
      mode === "RULE_AGGREGATE" && from ? from : undefined,
      mode === "RULE_AGGREGATE" && to ? to : undefined
    )
      .then((res) => {
        setItems(res.data ?? []);
        setLoading(false);
      })
      .catch(() => {
        setError("거래 이력을 불러오지 못했어요.");
        setLoading(false);
      });
  };

  useEffect(() => {
    load();
  }, [runId, mode, from, to]);

  if (loading) return <TradePageState variant="loading" />;
  if (error)
    return (
      <TradePageState
        variant="error"
        title="불러오기 실패"
        message={error}
        onRetry={load}
      />
    );
  if (items.length === 0)
    return (
      <TradePageState variant="empty" title="거래 이력이 없어요" />
    );

  return (
    <TradeTable>
      <TradeTableHeader>
        <div className="grid grid-cols-[1.5fr_1fr_70px_80px_90px_90px] gap-2 text-xs text-trade-muted uppercase tracking-wide font-trade-sans">
          <span>종목</span>
          <span>거래일시</span>
          <span>구분</span>
          <span className="text-right">수량</span>
          <span className="text-right">단가</span>
          <span className="text-right">손익</span>
        </div>
      </TradeTableHeader>
      {items.map((t) => {
        const isExpanded = expandedId === t.id;
        const rationale = parseRationale(t.rationaleJson);
        return (
          <div key={t.id}>
            <TradeTableRow
              onClick={() =>
                setExpandedId(isExpanded ? null : t.id)
              }
            >
              <div className="grid grid-cols-[1.5fr_1fr_70px_80px_90px_90px] gap-2 items-center text-sm">
                <div>
                  <span className="font-semibold text-trade-body font-trade-sans">
                    {t.companyName ?? t.symbol}
                  </span>
                  <span className="ml-1.5 text-xs text-trade-muted font-trade-mono">
                    {t.symbol}
                  </span>
                </div>
                <span className="text-xs text-trade-muted font-trade-mono">
                  {new Date(t.tradedAt).toLocaleString("ko-KR", {
                    month: "2-digit",
                    day: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
                <span>
                  <TradeBadge variant={t.side === "BUY" ? "up" : "down"}>
                    {t.side === "BUY" ? "매수" : "매도"}
                  </TradeBadge>
                </span>
                <span className="text-right font-trade-mono text-trade-body">
                  {t.qty}
                </span>
                <span className="text-right font-trade-mono text-trade-body">
                  {t.price.toLocaleString("ko-KR")}
                </span>
                <span
                  className={`text-right font-trade-mono font-semibold ${
                    t.pnl == null
                      ? "text-trade-muted"
                      : t.pnl >= 0
                      ? "text-trade-up"
                      : "text-trade-down"
                  }`}
                >
                  {t.pnl == null
                    ? "—"
                    : `${t.pnl >= 0 ? "+" : ""}${t.pnl.toLocaleString("ko-KR")}`}
                </span>
              </div>
            </TradeTableRow>
            {isExpanded && (
              <div className="px-4 pb-3 bg-trade-elevated border-b border-trade-hairline">
                <TradeRationaleRow rationale={rationale} />
              </div>
            )}
          </div>
        );
      })}
    </TradeTable>
  );
}

// ── Report tab ────────────────────────────────────────────────────────────────
function ReportTab({
  runId,
  mode,
  from,
  to,
}: {
  runId: number;
  mode: PeriodMode;
  from: string;
  to: string;
}) {
  const [report, setReport] = useState<ReportData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    fetchRunReport(
      runId,
      mode === "RULE_AGGREGATE" ? "RULE_AGGREGATE" : undefined,
      mode === "RULE_AGGREGATE" && from ? from : undefined,
      mode === "RULE_AGGREGATE" && to ? to : undefined
    )
      .then((res) => {
        setReport(res.data ?? null);
        setLoading(false);
      })
      .catch(() => {
        setError("성과 리포트를 불러오지 못했어요.");
        setLoading(false);
      });
  };

  useEffect(() => {
    load();
  }, [runId, mode, from, to]);

  if (loading) return <TradePageState variant="loading" />;
  if (error)
    return (
      <TradePageState
        variant="error"
        title="불러오기 실패"
        message={error}
        onRetry={load}
      />
    );
  if (!report || report.equityCurve.length === 0)
    return (
      <TradePageState variant="empty" title="리포트 데이터가 없어요" />
    );

  const initialCash =
    report.equityCurve.length > 0 ? (report.equityCurve[0]?.equity ?? 0) : 0;

  return (
    <div className="space-y-4">
      {/* RULE_AGGREGATE caption */}
      {mode === "RULE_AGGREGATE" && (
        <div className="rounded-lg border border-trade-hairline bg-trade-elevated px-4 py-2.5 text-xs text-trade-muted font-trade-sans">
          기간 내 계좌 전체 자산 흐름 — 단일 계좌 공유로 다른 전략의 영향이 포함됩니다.
        </div>
      )}

      {/* equity curve */}
      <TradeCard title="자산 곡선">
        <EquityCurveChart
          data={report.equityCurve as BacktestEquityPoint[]}
          drawdownSegments={[]}
          initialCash={initialCash}
        />
      </TradeCard>

      {/* metric cards */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
        <TradeStatCard
          label="총 수익률"
          value={fmtPct(report.totalReturn)}
          valueColor={report.totalReturn >= 0 ? "up" : "down"}
        />
        <TradeStatCard
          label="최대 낙폭"
          value={`${report.maxDrawdownPct.toFixed(2)}%`}
          valueColor="down"
        />
        <TradeStatCard
          label="승률"
          value={`${report.winRate.toFixed(1)}%`}
        />
        <TradeStatCard
          label="샤프 비율"
          value={fmtStat(report.sharpeRatio)}
        />
        <TradeStatCard
          label="소르티노 비율"
          value={fmtStat(report.sortinoRatio)}
        />
      </div>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export function PaperRunDetailPage() {
  const { runId: runIdStr } = useParams<{ runId: string }>();
  const navigate = useNavigate();
  const runId = Number(runIdStr);

  // breadcrumb data from dashboard
  const [breadcrumb, setBreadcrumb] = useState<{
    ruleName: string;
    runIndex: number;
    status: "RUNNING" | "STOPPED";
  } | null>(null);

  // tab state
  const [activeTab, setActiveTab] = useState<TabId>("dashboard");

  // period filter state
  const [periodMode, setPeriodMode] = useState<PeriodMode>("SINGLE_RUN");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  // load breadcrumb from dashboard
  useEffect(() => {
    if (!runId) return;
    fetchRunDashboard(runId)
      .then((res) => {
        if (res.data) {
          setBreadcrumb({
            ruleName: res.data.ruleName,
            runIndex: res.data.runIndex,
            status: res.data.status,
          });
        }
      })
      .catch(() => {
        /* breadcrumb best-effort */
      });
  }, [runId]);

  const tabs: { id: TabId; label: string }[] = [
    { id: "dashboard", label: "대시보드" },
    { id: "history", label: "거래이력" },
    { id: "report", label: "성과리포트" },
  ];

  return (
    <div className="min-h-screen bg-trade-bg px-6 py-6 font-trade-sans">
      {/* breadcrumb */}
      <div className="flex items-center gap-2 mb-4">
        <button
          type="button"
          onClick={() => navigate("/trading/paper/runs")}
          className="text-sm text-trade-muted hover:text-trade-body transition-colors font-trade-sans"
        >
          ‹ 실행 이력
        </button>
        {breadcrumb && (
          <>
            <span className="text-trade-hairline">/</span>
            <span className="text-sm text-trade-body font-trade-sans">
              {breadcrumb.ruleName} #{breadcrumb.runIndex}회차
            </span>
            <TradeBadge
              variant={breadcrumb.status === "RUNNING" ? "up" : "draft"}
              className="ml-1"
            >
              {breadcrumb.status === "RUNNING" ? "실행중" : "종료"}
            </TradeBadge>
          </>
        )}
      </div>

      {/* period filter toggle */}
      <div className="flex flex-wrap items-center gap-3 mb-4">
        <div className="flex rounded-lg border border-trade-hairline overflow-hidden bg-trade-surface">
          <button
            type="button"
            onClick={() => setPeriodMode("SINGLE_RUN")}
            className={`px-4 py-2 text-sm font-medium font-trade-sans transition-colors ${
              periodMode === "SINGLE_RUN"
                ? "bg-trade-primary text-trade-bg"
                : "text-trade-muted hover:text-trade-body"
            }`}
          >
            이 실행만
          </button>
          <button
            type="button"
            onClick={() => setPeriodMode("RULE_AGGREGATE")}
            className={`px-4 py-2 text-sm font-medium font-trade-sans transition-colors ${
              periodMode === "RULE_AGGREGATE"
                ? "bg-trade-primary text-trade-bg"
                : "text-trade-muted hover:text-trade-body"
            }`}
          >
            전략 전체 통합
          </button>
        </div>
        {periodMode === "RULE_AGGREGATE" && (
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="rounded border border-trade-hairline bg-trade-surface px-3 py-2 text-sm text-trade-body font-trade-mono focus:outline-none focus:border-trade-primary"
            />
            <span className="text-trade-muted text-sm">~</span>
            <input
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="rounded border border-trade-hairline bg-trade-surface px-3 py-2 text-sm text-trade-body font-trade-mono focus:outline-none focus:border-trade-primary"
            />
          </div>
        )}
      </div>

      {/* tab bar */}
      <div className="flex gap-1 bg-trade-surface rounded-lg p-1 w-fit mb-5">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveTab(tab.id)}
            className={`px-5 py-2 rounded text-sm font-semibold font-trade-sans transition-colors ${
              activeTab === tab.id
                ? "bg-trade-primary text-trade-bg"
                : "text-trade-muted hover:text-trade-body"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* tab content */}
      {activeTab === "dashboard" && <DashboardTab runId={runId} />}
      {activeTab === "history" && (
        <HistoryTab
          runId={runId}
          mode={periodMode}
          from={fromDate}
          to={toDate}
        />
      )}
      {activeTab === "report" && (
        <ReportTab
          runId={runId}
          mode={periodMode}
          from={fromDate}
          to={toDate}
        />
      )}
    </div>
  );
}
