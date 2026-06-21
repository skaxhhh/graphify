---
phase: "04"
plan: "01"
subsystem: "paper-dashboard"
tags: [dashboard, jpa, react, tanstack-query, mark-to-market]
dependency_graph:
  requires: [03-02]
  provides: [PaperDashboardController, PaperDashboardService, PaperDashboardPage, paper.ts types, paperApi.ts]
  affects: [04-02, 04-03, 04-04]
tech_stack:
  added: []
  patterns: [mark-to-market position valuation, 30s useQuery refetchInterval, KST midnight today-filter]
key_files:
  created:
    - backend/src/main/java/com/graphify/trading/paper/dto/PaperPositionItem.java
    - backend/src/main/java/com/graphify/trading/paper/dto/PaperDashboardDto.java
    - backend/src/main/java/com/graphify/trading/paper/PaperDashboardService.java
    - backend/src/main/java/com/graphify/trading/paper/PaperDashboardController.java
    - frontend/src/types/paper.ts
    - frontend/src/lib/paperApi.ts
    - frontend/src/pages/trading/paper/PaperDashboardPage.tsx
  modified: []
decisions:
  - "paper.ts centralizes all Phase 4 types (Dashboard, Monitor, Report, SignalLog, TradeItem) so later plans can import without creating new files"
  - "paperApi.ts also pre-declares lifecycle stubs (promoteRule/pauseRule/resumeRule/copyRule) for 04-02 to fill in"
  - "Mark-to-market uses latest bar from findBySymbolAndIntervalOrderByTsAsc + last element — simple and consistent with LiveEvaluationService"
metrics:
  duration: "1.5m"
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_created: 7
---

# Phase 04 Plan 01: Paper Dashboard Summary

**One-liner:** Paper dashboard API (cash + mark-to-market positions + today PnL + active rule count) with React UI using 30-second auto-refresh and green/red PnL color coding.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | PaperDashboard backend | 0d2cc32 | PaperDashboardDto + PaperPositionItem + PaperDashboardService + PaperDashboardController |
| 2 | PaperDashboardPage frontend | 0d2cc32 | paper.ts + paperApi.ts + PaperDashboardPage.tsx |

## What Was Built

**`GET /api/v1/trading/paper/dashboard`** — returns:
- `cash`: current paper account cash
- `totalEquity`: cash + sum(qty * markPrice) for all positions
- `totalUnrealizedPnl`: sum of all position unrealizedPnl
- `todayRealizedPnl`: sum of SELL trade pnl since KST midnight today
- `activePaperLiveRuleCount`: count of PAPER status rules with PAPER_LIVE status
- `positions[]`: symbol, qty, avgPrice, markPrice, marketValue, unrealizedPnl, unrealizedPnlPct

**Mark-to-market**: uses `findBySymbolAndIntervalOrderByTsAsc(symbol, "5m")` → last element's close price; falls back to avgPrice if no bars.

**`PaperDashboardPage`**:
- 4 stat cards: 총 평가금액 (with % return sub-text) | 가용 현금 | 오늘 실현손익 | 활성 PAPER_LIVE 룰
- Positions table: 종목 | 수량 | 평균단가 | 현재가 | 평가금액 | 평가손익 | 손익률
- Green/red color coding for all PnL values
- Loading skeleton (4 gray cards + table placeholder)
- Empty state for positions ("보유 포지션 없음")
- 30s `refetchInterval` via `useQuery`

**`frontend/src/types/paper.ts`** — all Phase 4 types pre-declared: `PaperDashboardData`, `PaperPositionItem`, `SignalLogItem`, `TradeItem`, `MonitorData`, `EquityPoint`, `ReportData`

**`frontend/src/lib/paperApi.ts`** — API functions: `fetchPaperDashboard`, `fetchPaperMonitor`, `fetchPaperReport`, `promoteRule`, `pauseRule`, `resumeRule`, `copyRule`

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check

Files exist:
- backend/src/main/java/com/graphify/trading/paper/PaperDashboardController.java — FOUND
- frontend/src/types/paper.ts — FOUND
- frontend/src/lib/paperApi.ts — FOUND
- frontend/src/pages/trading/paper/PaperDashboardPage.tsx — FOUND

Commits: 0d2cc32 — feat(04-01): paper dashboard API + UI

## Self-Check: PASSED
