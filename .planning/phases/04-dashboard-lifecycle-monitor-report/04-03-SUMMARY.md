---
phase: "04"
plan: "03"
subsystem: "monitor"
tags: [monitor, signal-log, indicator-snapshot, react, polling]
dependency_graph:
  requires: [04-02]
  provides: [PaperMonitorService, PaperMonitorController, TradingMonitorPage]
  affects: [04-04]
tech_stack:
  added: []
  patterns: [KST market hours guard, indicator_snapshot JSON parse, 30s polling refetchInterval]
key_files:
  created:
    - backend/src/main/java/com/graphify/trading/paper/dto/SignalLogItem.java
    - backend/src/main/java/com/graphify/trading/paper/dto/TradeItem.java
    - backend/src/main/java/com/graphify/trading/paper/dto/MonitorDto.java
    - backend/src/main/java/com/graphify/trading/paper/PaperMonitorService.java
    - backend/src/main/java/com/graphify/trading/paper/PaperMonitorController.java
  modified:
    - frontend/src/pages/trading/TradingMonitorPage.tsx
decisions:
  - "schedulerLastRun derived from max(ts) of paper_signal_log — avoids separate scheduler_run table"
  - "indicator_snapshot JSON parsed in service layer (not entity) — keeps entity simple, parse errors return nulls non-fatally"
  - "market status computed from server-side KST ZonedDateTime — no client-side timezone logic needed"
metrics:
  duration: "4m"
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_created: 5
  files_modified: 1
---

# Phase 04 Plan 03: Monitor Summary

**One-liner:** Monitor API aggregating KST market status, scheduler last run (max signal ts), recent 50 signals with parsed rsi14/sma20/price from indicator_snapshot, and today's trades; TradingMonitorPage with signal log table, trade feed, and 30s auto-refresh.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Monitor backend (DTOs + Service + Controller) | 5622c18 | SignalLogItem + TradeItem + MonitorDto + PaperMonitorService + PaperMonitorController |
| 2 | TradingMonitorPage frontend | 5622c18 | TradingMonitorPage.tsx |

## What Was Built

**`SignalLogItem`** — record with id, ruleId, symbol, ts, signal, executed, rsi14, sma20, price (parsed from JSON).

**`TradeItem`** — record with id, symbol, side, qty, price, pnl, tradedAt.

**`MonitorDto`** — record: schedulerLastRun (Instant|null), marketStatus ("OPEN"|"CLOSED"), recentSignals (List<SignalLogItem>), todayTrades (List<TradeItem>).

**`PaperMonitorService.getMonitor(userId)`**:
- `schedulerLastRun`: max `ts` from `findTop50ByOrderByTsDesc()`, null if no logs
- `marketStatus`: KST weekday 09:00–15:30 → "OPEN", otherwise "CLOSED"
- `recentSignals`: top 50 logs, each parsed via Jackson for rsi14/sma20/price; parse errors produce nulls (non-fatal)
- `todayTrades`: account's trades filtered to >= KST midnight today; empty list if no account

**`PaperMonitorController`** — `GET /api/v1/trading/paper/monitor` → `ApiResponse<MonitorDto>`

**`TradingMonitorPage`**:
- Market status card: green dot + "개장" / gray + "폐장"
- Scheduler last run card: formatted KST datetime
- Signal log table: ts | symbol | BUY(green)/SELL(red)/HOLD(gray) badge | 체결/미체결 | RSI14 | SMA20 | 현재가
- Today's trade table: ts | symbol | 매수(green)/매도(red) | qty | price | PnL (colored)
- `refetchInterval: 30000` — 30s auto-refresh
- Last updated timestamp shown in header

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check

Files exist:
- backend/src/main/java/com/graphify/trading/paper/PaperMonitorService.java — FOUND
- backend/src/main/java/com/graphify/trading/paper/PaperMonitorController.java — FOUND
- backend/src/main/java/com/graphify/trading/paper/dto/MonitorDto.java — FOUND
- frontend/src/pages/trading/TradingMonitorPage.tsx — FOUND

Commits: 5622c18 — feat(04-03): monitor API + TradingMonitorPage with signal log and trade feed

Full test suite: BUILD SUCCESSFUL (7s)
TypeScript: clean (no errors)

## Self-Check: PASSED
