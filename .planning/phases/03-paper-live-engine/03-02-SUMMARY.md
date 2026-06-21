---
phase: "03"
plan: "02"
subsystem: "paper-live-evaluation"
tags: [paper-trading, evaluation, tdd, live-scheduler, indicator-snapshot]
dependency_graph:
  requires: [03-01, 02-02]
  provides: [LiveEvaluationService, LiveDataScheduler wiring]
  affects: []
tech_stack:
  added: []
  patterns: [per-tick evaluation loop, staleness guard, min-bars guard, indicator snapshot JSON]
key_files:
  created:
    - backend/src/main/java/com/graphify/trading/paper/LiveEvaluationService.java
    - backend/src/test/java/com/graphify/trading/paper/LiveEvaluationServiceTest.java
  modified:
    - backend/src/main/java/com/graphify/market/LiveDataScheduler.java
    - backend/src/test/java/com/graphify/market/LiveDataSchedulerTest.java
decisions:
  - "saveEquitySnapshot uses positionRepo.findByAccountId (all positions) and marks last bar price for each; falls back to avgPrice if no bars available"
  - "indicator_snapshot JSON uses 0.0 instead of null for NaN RSI/SMA to keep JSON schema consistent for MON-04 consumers"
  - "evaluateRule swallows per-symbol exceptions (warn-only) so one bad symbol never blocks others"
metrics:
  duration: "2.5m"
  completed_date: "2026-06-21"
  tasks_completed: 1
  files_created: 2
  files_modified: 2
---

# Phase 03 Plan 02: LiveEvaluationService Summary

**One-liner:** Per-tick PAPER_LIVE evaluation loop: load rules → staleness/min-bars guards → RuleEvaluator entry/exit → PaperExecutor fill → equity snapshot, wired into LiveDataScheduler after ingest.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | LiveEvaluationService + LiveDataScheduler wiring (TDD GREEN) | c1f3f0b | LiveEvaluationService.java + LiveEvaluationServiceTest.java + LiveDataScheduler.java + LiveDataSchedulerTest.java |

## What Was Built

**`LiveEvaluationService`** (`@Service`, `com.graphify.trading.paper`):
- `evaluateTick(Instant)` — entry point called by `LiveDataScheduler` after each ingest cycle
- Loads all `PAPER_LIVE`-status rules from `TradingRuleRepository.findAll()` filtered by status
- Per-rule: resolves symbols from `RuleDefinition.universe` (type=symbols list or additionalSymbols)
- Loads `PaperAccount` by `userId`; skips rule if no account exists (warns)
- Per-symbol evaluation:
  - **Staleness guard**: `intradayRepo.findMaxTsBySymbolAndInterval(symbol, "5m")` — skips if maxTs older than 10 minutes
  - **Min-bars guard**: `marketDataPort.recentIntradayBars(symbol)` — skips if < 20 bars (RSI-14 + SMA-20 minimum)
  - **Signal determination**: position held → `ruleEvaluator.exitTriggered()` → SELL or HOLD; no position → `ruleEvaluator.entryTriggered()` → BUY or HOLD
  - **Execution**: `executor.execute(signal, rule, symbol, lastPrice, tickTime, indicatorJson)` on BUY or SELL
- **Indicator snapshot** (MON-04): builds `{"price": X, "rsi14": Y, "sma20": Z}` JSON via `Indicators.rsi()/sma()`; passed to `executor.execute()` for `paper_signal_log`
- **Equity snapshot**: after all symbols evaluated for a rule, computes `cash + sum(qty * markPrice)` for all positions and saves `PaperEquitySnapshot`
- Per-symbol and per-rule exceptions are swallowed (warn-only) so one failure never blocks others

**`LiveDataScheduler`** (modified):
- Added `LiveEvaluationService evaluationService` constructor parameter
- Added `evaluationService.evaluateTick(now.toInstant())` call after ingest loop completes

**`LiveDataSchedulerTest`** (updated):
- Added `@Mock LiveEvaluationService evaluationService` and updated `setUp()` to pass it as 5th constructor arg

## Test Results

```
LiveEvaluationServiceTest (6 tests, Mockito — no DB required)
  no_paper_live_rules_skips_everything    PASSED
  entry_triggered_calls_buy               PASSED
  exit_triggered_calls_sell               PASSED
  stale_bars_skips_evaluation             PASSED
  insufficient_bars_skips_evaluation      PASSED
  equity_snapshot_saved_after_tick        PASSED

LiveDataSchedulerTest (5 tests)  PASSED
PaperExecutorTest (6 tests)      PASSED
Full suite: BUILD SUCCESSFUL
```

## Deviations from Plan

**[Rule 3 - Blocking] LiveDataSchedulerTest constructor mismatch**
- **Found during:** Task 1 compilation
- **Issue:** Adding `LiveEvaluationService` to `LiveDataScheduler` constructor broke the existing 4-arg test `setUp()`
- **Fix:** Added `@Mock LiveEvaluationService evaluationService` and updated constructor call to 5 args
- **Files modified:** `LiveDataSchedulerTest.java`
- **Commit:** c1f3f0b (same commit)

**[Rule 1 - Bug] UnnecessaryStubbingException in stale_bars test**
- **Found during:** Task 1 test run
- **Issue:** Test stubbed `marketDataPort.recentIntradayBars()` but staleness guard returns before bars are read
- **Fix:** Removed the unused stub; kept only stubs for paths actually exercised
- **Commit:** c1f3f0b (fixed in same commit)

## Phase 3 Completion

Phase 3 is now complete. The PAPER_LIVE loop is fully closed:
1. `LiveDataScheduler` (Phase 2) collects 5m bars every 5 minutes during KRX hours
2. `LiveEvaluationService` (Phase 3) evaluates all PAPER_LIVE rules against fresh bars
3. `PaperExecutor` (Phase 3) executes BUY/SELL signals with full write-through persistence
4. `PaperSignalLog` records every signal + indicator snapshot for monitoring (MON-04)
5. `PaperEquitySnapshot` records account equity after every tick

## Self-Check

Files exist:
- backend/src/main/java/com/graphify/trading/paper/LiveEvaluationService.java — FOUND
- backend/src/test/java/com/graphify/trading/paper/LiveEvaluationServiceTest.java — FOUND

Commits:
- c1f3f0b — feat(03-02): LiveEvaluationService + LiveDataScheduler wiring (TDD GREEN)

## Self-Check: PASSED
