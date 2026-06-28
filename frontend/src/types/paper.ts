export interface PaperPositionItem {
  symbol: string;
  companyName: string | null;
  qty: number;
  avgPrice: number;
  markPrice: number;
  marketValue: number;
  unrealizedPnl: number;
  unrealizedPnlPct: number;
}

export interface PaperDashboardData {
  cash: number;
  totalEquity: number;
  totalUnrealizedPnl: number;
  todayRealizedPnl: number;
  activePaperLiveRuleCount: number;
  positions: PaperPositionItem[];
}

export interface SignalLogItem {
  id: number;
  ruleId: number;
  symbol: string;
  ts: string;
  signal: "BUY" | "SELL" | "HOLD";
  executed: boolean;
  rsi14: number | null;
  sma20: number | null;
  price: number | null;
}

export interface TradeItem {
  id: number;
  symbol: string;
  side: "BUY" | "SELL";
  qty: number;
  price: number;
  pnl: number | null;
  tradedAt: string;
}

export interface PaperTradeHistoryItem {
  id: number;
  tradedAt: string;
  symbol: string;
  companyName: string | null;
  side: "BUY" | "SELL";
  qty: number;
  price: number;
  fee: number | null;
  pnl: number | null;
  rationaleJson: string | null;
}

export interface MonitorData {
  schedulerLastRun: string | null;
  marketStatus: "OPEN" | "CLOSED";
  recentSignals: SignalLogItem[];
  todayTrades: TradeItem[];
}

export interface EquityPoint {
  datetime: string;
  equity: number;
}

export interface ReportData {
  equityCurve: EquityPoint[];
  totalReturn: number;
  maxDrawdownPct: number;
  winRate: number;
  totalTrades: number;
  winTrades: number;
  sharpeRatio: number;
  sortinoRatio: number;
  periodFrom: string | null;
  periodTo: string | null;
}

// 6.9 Wave 4: run-scoped types (Wave 5 pages consume these)
export interface RunSummary {
  runId: number;
  ruleId: number;
  ruleName: string;
  runIndex: number;
  status: "RUNNING" | "STOPPED";
  startedAt: string;
  endedAt: string | null;
  universe: string[];
  realizedPnl: number;
  returnPct: number;
  tradeCount: number;
  finalEquity: number;
}

export interface RunDashboard {
  runId: number;
  runIndex: number;
  ruleName: string;
  status: "RUNNING" | "STOPPED";
  totalEquity: number;
  availableCash: number;
  realizedPnl: number;
  unrealizedPnl: number;
  tradeCount: number;
  positions: PaperPositionItem[];
}
