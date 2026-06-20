---
phase: 01-backtest-visualization
plan: "04"
subsystem: frontend
tags: [react, recharts, backtest, ui, typescript]
dependency_graph:
  requires: ["01-03"]
  provides: ["CHART-01", "CHART-02", "CHART-03"]
  affects: ["frontend/src/pages/trading/paper/PaperBacktestPage.tsx"]
tech_stack:
  added: []
  patterns: ["recharts ResponsiveContainer", "React useState for form fields", "TanStack Query useMutation"]
key_files:
  created: []
  modified:
    - frontend/src/pages/trading/paper/PaperBacktestPage.tsx
decisions:
  - "drawdownSegments ?? [] null-guard applied — server may return result with no drawdown segments"
  - "StatCard component defined inline in PaperBacktestPage — no need for separate file at this scale"
  - "lg:grid-cols-6 form layout with lg:col-span-1 on rule select — fits 6 inputs without overflow"
metrics:
  duration_minutes: 8
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_modified: 1
---

# Phase 1 Plan 4: PaperBacktestPage — Equity Chart, Advanced Stats, Time Inputs Summary

**One-liner:** Wired EquityCurveChart + drawdown overlays into PaperBacktestPage, added Sharpe/Sortino/PF stats cards and 09:00–12:00 time range inputs, fixed t.date→t.datetime TypeScript error.

## What Was Built

Complete PaperBacktestPage rewrite that closes all three Phase 1 verification gaps:

- **6-column form:** Rule select, 시작일/종료일 date pickers, 시작 시각 (09:00) / 종료 시각 (12:00) time inputs, 초기 자본 number input
- **timeFrom/timeTo state** wired into `runBacktest()` payload so backend receives the intraday window
- **EquityCurveChart** rendered in a bordered card between the 5 summary metric cards and the trade table — recharts LineChart with emerald line, red ReferenceArea drawdown overlays, CustomTooltip showing datetime / equity / cumulative return %
- **고급 통계 section** with 3 StatCards: Sharpe Ratio, Sortino Ratio, Profit Factor — values formatted via `fmtStat()` which shows "—" for non-finite numbers
- **Trade table** column header renamed from "일자" to "일시"; rows use `t.datetime` (was `t.date`) — resolves TypeScript error from 01-03 type rename
- **Description text** updated to "5분봉 인트라데이 데이터(09:00–12:00 KST, 최대 60일)"
- **TypeScript:** zero `error TS` compile errors after changes

## Tasks Completed

| # | Task | Commit | Status |
|---|------|--------|--------|
| 1 | Rewrite PaperBacktestPage with chart, stats section, and time inputs | ec70a74 | Done |
| 2 | Visual verification of chart, stats, and form | — (checkpoint) | Approved |

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check

- [x] `frontend/src/pages/trading/paper/PaperBacktestPage.tsx` exists and contains all required elements
- [x] Commit ec70a74 exists
- [x] `npx tsc --noEmit` exits with zero errors
- [x] Human verifier approved visual checkpoint

## Self-Check: PASSED
