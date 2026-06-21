---
phase: "04"
plan: "04"
subsystem: "report"
tags: [report, equity-curve, sharpe, sortino, recharts, react]
dependency_graph:
  requires: [04-03]
  provides: [PaperReportService, PaperReportController, PaperReportPage, ReportDto]
  affects: []
tech_stack:
  added: []
  patterns: [MDD from running peak, Sharpe/Sortino from daily returns, EquityCurveChart reuse]
key_files:
  created:
    - backend/src/main/java/com/graphify/trading/paper/dto/ReportDto.java
    - backend/src/main/java/com/graphify/trading/paper/PaperReportService.java
    - backend/src/main/java/com/graphify/trading/paper/PaperReportController.java
    - frontend/src/pages/trading/paper/PaperReportPage.tsx
  modified: []
decisions:
  - "equity curve built from last 30 days of paper_equity_snapshots reversed to ascending order — no separate date-range param needed for initial version"
  - "Sharpe/Sortino computed from per-snapshot daily returns (not grouped by calendar day) — simpler, matches backtest engine formula"
  - "EquityCurveChart reused with drawdownSegments=[] — avoids MDD segment computation complexity for paper report"
  - "ReportDto.empty() static factory for no-account case — keeps controller clean"
metrics:
  duration: "5m"
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_created: 4
  files_modified: 0
---

# Phase 04 Plan 04: Report Summary

**One-liner:** Paper trading performance report API computing equity curve (last 30 days of snapshots), MDD, Sharpe/Sortino from daily returns, win rate from SELL trades; PaperReportPage reusing EquityCurveChart with 6 stat cards and empty state.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Report backend (ReportDto + PaperReportService + PaperReportController) | e6e994a | ReportDto + PaperReportService + PaperReportController |
| 2 | PaperReportPage frontend | e6e994a | PaperReportPage.tsx |

## What Was Built

**`ReportDto`** — record: equityCurve (List<EquityPoint>), totalReturn, maxDrawdownPct, winRate, totalTrades, winTrades, sharpeRatio, sortinoRatio, periodFrom, periodTo. Nested `EquityPoint(String datetime, double equity)` matches `BacktestEquityPoint` shape for chart reuse. `ReportDto.empty()` for no-account case.

**`PaperReportService.getReport(userId)`**:
- Loads snapshots via `findByAccountIdOrderByTsDesc`, reverses to ascending, filters last 30 days
- Equity curve: maps each snapshot to `EquityPoint(ts.toString(), equity.doubleValue())`
- `totalReturn`: (last - first) / first * 100
- `maxDrawdownPct`: running peak → max((peak - eq) / peak * 100)
- Daily returns: per-snapshot % changes → Sharpe = mean/std * sqrt(252), Sortino = mean/downside_std * sqrt(252), min 5 data points else 0.0
- Trade stats: SELL trades only → winTrades = count(pnl > 0), winRate = winTrades/total * 100

**`PaperReportController`** — `GET /api/v1/trading/paper/report` → `ApiResponse<ReportDto>`

**`PaperReportPage`**:
- `useQuery` (no auto-refresh — report is historical)
- Period header: periodFrom ~ periodTo formatted KST
- Equity curve via `<EquityCurveChart data={data.equityCurve} drawdownSegments={[]} initialCash={...} />`
- 6 stat cards: 총 수익률 (colored) | MDD (red) | 승률 | 총 거래 | Sharpe | Sortino
- Empty state: "모의 실행 데이터가 없습니다. PAPER_LIVE 룰을 활성화하면 자동으로 기록됩니다."

## Deviations from Plan

**[Rule 1 - Bug] TypeScript error on equityCurve[0] access**
- **Found during:** Task 2 TypeScript check
- **Issue:** `data.equityCurve[0].equity` flagged as `Object is possibly 'undefined'` (TS2532)
- **Fix:** Changed to `data.equityCurve[0]?.equity ?? 10_000_000`
- **Files modified:** PaperReportPage.tsx

## Self-Check

Files exist:
- backend/src/main/java/com/graphify/trading/paper/dto/ReportDto.java — FOUND
- backend/src/main/java/com/graphify/trading/paper/PaperReportService.java — FOUND
- backend/src/main/java/com/graphify/trading/paper/PaperReportController.java — FOUND
- frontend/src/pages/trading/paper/PaperReportPage.tsx — FOUND

Commits: e6e994a — feat(04-04): paper report API + PaperReportPage with equity curve and stat cards

Full test suite: BUILD SUCCESSFUL (7s)
TypeScript: clean (no errors)

## Self-Check: PASSED
