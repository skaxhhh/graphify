---
phase: 01-backtest-visualization
plan: "03"
subsystem: frontend
tags: [recharts, typescript, equity-curve, drawdown, visualization]
dependency_graph:
  requires: ["01-01", "01-02"]
  provides: ["EquityCurveChart component", "recharts", "DrawdownSegment type", "updated BacktestResult types"]
  affects: ["01-04-PLAN.md (PaperBacktestPage wiring)"]
tech_stack:
  added: ["recharts@2.15.0"]
  patterns: ["recharts LineChart + ReferenceArea for drawdown overlay", "inline CustomTooltip with initialCash prop injection"]
key_files:
  created:
    - frontend/src/components/backtest/EquityCurveChart.tsx
  modified:
    - frontend/src/types/trading.ts
    - frontend/package.json
    - frontend/package-lock.json
decisions:
  - "CustomTooltip defined with inline interface (not recharts TooltipProps generic) to avoid recharts v2 TypeScript generic complexity"
  - "ReferenceArea x1/x2 receive ISO datetime strings directly matching XAxis dataKey=datetime values"
  - "XAxis tick labels shown only at 09:00 (session open) to avoid label crowding on 5m bars"
  - "YAxis formatted in Korean won units (만/억 abbreviations)"
metrics:
  duration_minutes: 1
  completed_date: "2026-06-20"
  tasks_completed: 2
  files_changed: 4
---

# Phase 1 Plan 03: recharts Install + EquityCurveChart Component Summary

**One-liner:** recharts@2.15.0 installed with EquityCurveChart using LineChart + ReferenceArea drawdown overlays and Korean-formatted CustomTooltip.

## What Was Built

### Task 1: Install recharts and fix TypeScript types (commit: e63e54f)

- Installed `recharts@2.15.0` via npm
- Updated `frontend/src/types/trading.ts`:
  - `BacktestTrade.date` renamed to `datetime` (matches backend `TradeDto` LocalDateTime serialization)
  - `BacktestEquityPoint.date` renamed to `datetime` (matches backend `EquityPoint` LocalDateTime serialization)
  - Added `DrawdownSegment` interface `{ start: string; end: string }` (ISO datetime strings)
  - Extended `BacktestResult` with `sharpeRatio`, `sortinoRatio`, `profitFactor`, `drawdownSegments: DrawdownSegment[]`
  - Added `timeFrom?: string` and `timeTo?: string` to `BacktestRequest` (used in plan 01-04 form)
- TypeScript compiles with zero errors in modified files; only expected `PaperBacktestPage.tsx` error from `date` → `datetime` rename (fixed in 01-04)

### Task 2: Create EquityCurveChart component (commit: 0aa61a7)

- Created `frontend/src/components/backtest/EquityCurveChart.tsx`
- recharts `LineChart` with emerald-500 (`#10b981`) equity line, `strokeWidth=2`, no dots
- `ReferenceArea` overlays for each `DrawdownSegment` with `fill="rgba(239,68,68,0.15)"` and `stroke="none"`
- `XAxis` `dataKey="datetime"` with tick formatter showing date only at 09:00 session opens
- `YAxis` formatted in Korean won abbreviations (만/억)
- `CustomTooltip` renders three lines: datetime (formatted as `YYYY-MM-DD HH:mm`), equity in 원, cumulative return %
- Empty state fallback: renders placeholder div when `data.length === 0`

## Verification Results

All success criteria met:

1. `recharts@^2.15.0` present in `package.json` dependencies and installed in node_modules
2. `trading.ts` exports `DrawdownSegment`, `BacktestEquityPoint.datetime`, `BacktestResult` with sharpeRatio/sortinoRatio/profitFactor/drawdownSegments
3. `EquityCurveChart.tsx` exports `EquityCurveChart` using recharts LineChart + ReferenceArea
4. `CustomTooltip` renders datetime, equity (원), and cumulative return %
5. TypeScript compiles cleanly for both new files (only PaperBacktestPage error is expected, fixed in 01-04)

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

Files confirmed:
- FOUND: frontend/src/components/backtest/EquityCurveChart.tsx
- FOUND: frontend/src/types/trading.ts (DrawdownSegment at line 96, datetime at lines 83/92)

Commits confirmed:
- FOUND: e63e54f (feat(01-03): install recharts@2.15.0 and fix TypeScript backtest types)
- FOUND: 0aa61a7 (feat(01-03): add EquityCurveChart component with drawdown overlays and custom tooltip)
