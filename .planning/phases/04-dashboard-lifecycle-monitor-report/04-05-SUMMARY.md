---
phase: 04-dashboard-lifecycle-monitor-report
plan: "05"
subsystem: backend-paper-live-engine
tags:
  - paper-live
  - seam
  - lifecycle
  - executor-routing
  - symbol-resolution
dependency_graph:
  requires:
    - 03-paper-live-engine (PaperLiveSymbolService, PaperLiveSymbolRepository, LiveEvaluationService, PaperLifecycleService)
  provides:
    - RuleStatus.isLiveActive — single status predicate for live-loop filtering (Phase 6 extend with one line)
    - OrderExecutorPort.supports(TradingRule) — mode-based executor routing seam
    - PaperLifecycleService.resolveSymbols — mode-agnostic symbol assignment helper
    - paper_live_symbols as single canonical symbol source for both ingestion and evaluation
  affects:
    - LiveDataScheduler (Guard 3 activeSymbolsUnion now non-empty after promote)
    - LiveEvaluationService (evaluates exactly the symbols in paper_live_symbols)
    - Phase 6 LIVE promotion (additive: extend isLiveActive, add TossOrderExecutor, reuse assignSymbols)
tech_stack:
  added: []
  patterns:
    - Strategy pattern (OrderExecutorPort.supports per-rule routing)
    - Single canonical symbol source (paper_live_symbols table)
    - Single edit point predicate (RuleStatus.isLiveActive)
key_files:
  created:
    - backend/src/main/java/com/graphify/trading/rule/RuleStatus.java
  modified:
    - backend/src/main/java/com/graphify/trading/rule/PaperLiveSymbolService.java
    - backend/src/main/java/com/graphify/trading/paper/LiveEvaluationService.java
    - backend/src/main/java/com/graphify/trading/paper/OrderExecutorPort.java
    - backend/src/main/java/com/graphify/trading/paper/PaperExecutor.java
    - backend/src/main/java/com/graphify/trading/paper/PaperLifecycleService.java
decisions:
  - "paper_live_symbols is the SINGLE canonical symbol source for both ingestion (activeSymbolsUnion) and evaluation (resolveSymbols) — eliminates divergent sources that caused the scheduler Guard 3 short-circuit"
  - "volume_top_n rules snapshot the full candidate universe at promote time (symbolsByMarket union additionalSymbols); per-day top-N re-selection in the live lane is deferred (out of scope for gap-closure)"
  - "copy() is non-destructive — no deactivateRule call on the original; pause() is the only deactivation path"
  - "resolveSymbols in PaperLifecycleService has zero PAPER_LIVE literals — purely mode/status-agnostic so Phase 6 LIVE promotion reuses it unchanged"
  - "ERR_LIFECYCLE_005 thrown on empty resolved universe at promote/resume — prevents silently promoting unrunnable rules"
metrics:
  duration: "4m"
  completed_date: "2026-06-21"
  tasks_completed: 3
  files_created: 1
  files_modified: 5
---

# Phase 4 Plan 5: PAPER_LIVE Activation Chain Fix + Phase 6 Seams Summary

**One-liner:** Fixed broken DRAFT→PAPER_LIVE activation by wiring `paper_live_symbols` as single canonical source for both scheduler ingestion and live evaluation, plus three non-destructive extension seams (shared status predicate, per-rule executor routing, mode-agnostic symbol assignment) that make Phase 6 LIVE purely additive.

## What Was Built

### Root Cause Fixed

`PaperLifecycleService.promote()` previously only flipped `status='PAPER_LIVE'` without populating `paper_live_symbols`, so `LiveDataScheduler` Guard 3 (`activeSymbolsUnion()`) always found an empty table and short-circuited every tick — no ingestion, no evaluation, no trades. Additionally `LiveEvaluationService.resolveSymbols()` read from `rule.definition.universe` while the scheduler ingested from `paper_live_symbols`, creating two divergent symbol sources.

### Tasks Completed

#### Task 1: SEAM 1 — RuleStatus.isLiveActive (commit `6aa9313`)

- Created `RuleStatus.java` (final utility, no Spring bean) with `isLiveActive(String status)` returning `PAPER_LIVE.equals(status)`
- `PaperLiveSymbolService.activeSymbolsUnion()` filter replaced with `RuleStatus.isLiveActive(r.getStatus())`
- `LiveEvaluationService.evaluateTick()` filter replaced with `RuleStatus.isLiveActive(r.getStatus())`
- Phase 6: adding `"LIVE"` status to the live loop requires one line in one method

#### Task 2: SEAM 2 + SEAM 3 (commit `14aba6a`)

**SEAM 2 — executor routing:**
- `OrderExecutorPort` gains `boolean supports(TradingRule rule)` routing method
- `PaperExecutor.supports()` returns `true` for `"PAPER".equals(rule.getMode())` — all current PAPER_LIVE rules match
- `LiveEvaluationService` changes from single `OrderExecutorPort executor` field to `List<OrderExecutorPort> executors` with private `executorFor(TradingRule)` selector; warns and skips if no executor matches
- Phase 6: `TossOrderExecutor` auto-joins via Spring injection and serves LIVE-mode rules — no change to `LiveEvaluationService`

**SEAM 3 + activation fix:**
- `PaperLifecycleService` gains two new injected dependencies: `PaperLiveSymbolService` and `MarketDataPort`
- Private `resolveSymbols(TradingRule rule)` helper: mode/status-agnostic (zero `"PAPER_LIVE"` literals inside); mirrors `BacktestService.resolveInitialSymbols` logic — `volume_top_n` expands via `symbolsByMarket(market) ∪ additionalSymbols`; `symbols`/`watchlist` returns `u.symbols()` unchanged
- `promote()`: resolves symbols before status flip; throws `ERR_LIFECYCLE_005` on empty universe; calls `assignSymbols(ruleId, symbols)` after save
- `pause()`: calls `deactivateRule(ruleId)` after status flip to PAUSED
- `resume()`: resolves and calls `assignSymbols(ruleId, symbols)` after status flip back to PAPER_LIVE
- `copy()`: non-destructive — no symbol side-effects on original (deactivation is pause's job)

#### Task 3: Activation fix — canonical symbol source (commit `1d866e8`)

- `LiveEvaluationService` gains `PaperLiveSymbolRepository` field + constructor parameter
- `resolveSymbols(TradingRule rule)` rewrites to: `paperLiveSymbolRepository.findByRuleId(rule.getId()).stream().map(PaperLiveSymbol::getSymbol).distinct().toList()`
- `evaluateRule()` calls `resolveSymbols(rule)` — `def.universe()` no longer drives the live symbol list
- `def` still parsed and passed to `evaluateSymbol` for entry/exit condition evaluation
- Ingested symbols (`activeSymbolsUnion`) == evaluated symbols (`findByRuleId`) — guaranteed single source
- `volume_top_n` live rules now evaluated correctly (full candidate universe pre-expanded by `promote()`)

## Deviations from Plan

None — plan executed exactly as written.

## Phase 6 Extension Guide

To add real LIVE order execution via Toss:

1. **SEAM 1:** In `RuleStatus.isLiveActive()`, add `|| "LIVE".equals(status)` — one line, ingestion + evaluation both extend automatically
2. **SEAM 2:** Create `TossOrderExecutor implements OrderExecutorPort`; `supports(rule)` returns `"LIVE".equals(rule.getMode())`; Spring auto-injects it into `LiveEvaluationService.executors` list — zero changes to `LiveEvaluationService`
3. **SEAM 3:** Call the existing `resolveSymbols(rule)` + `assignSymbols(ruleId, symbols)` pattern in LIVE promotion — the helper is already mode-agnostic

Market-data source for LIVE (TossLiveIntradayAdapter per TOSS-05) is a separate seam not touched here.

## Self-Check: PASSED

All created files exist on disk. All task commits verified in git history.

| Item | Status |
|------|--------|
| RuleStatus.java | FOUND |
| PaperLiveSymbolService.java (modified) | FOUND |
| LiveEvaluationService.java (modified) | FOUND |
| OrderExecutorPort.java (modified) | FOUND |
| PaperExecutor.java (modified) | FOUND |
| PaperLifecycleService.java (modified) | FOUND |
| 04-05-SUMMARY.md | FOUND |
| Commit 6aa9313 (Task 1) | FOUND |
| Commit 14aba6a (Task 2) | FOUND |
| Commit 1d866e8 (Task 3) | FOUND |
