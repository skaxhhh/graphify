---
phase: "04"
plan: "02"
subsystem: "rule-lifecycle"
tags: [lifecycle, state-machine, jpa, react, status-badge]
dependency_graph:
  requires: [04-01]
  provides: [PaperLifecycleService, PaperLifecycleController, TradingRulesPage, V33 migration]
  affects: [04-03, 04-04]
tech_stack:
  added: []
  patterns: [state machine guards (promote/pause/resume), backtested flag pattern, status badge component]
key_files:
  created:
    - backend/src/main/resources/db/migration/V33__rule_backtested_flag.sql
    - backend/src/main/java/com/graphify/trading/paper/PaperLifecycleService.java
    - backend/src/main/java/com/graphify/trading/paper/PaperLifecycleController.java
  modified:
    - backend/src/main/java/com/graphify/trading/rule/TradingRule.java
    - backend/src/main/java/com/graphify/trading/rule/dto/RuleResponse.java
    - backend/src/main/java/com/graphify/trading/rule/PaperRuleService.java
    - backend/src/main/java/com/graphify/trading/backtest/BacktestService.java
    - frontend/src/types/trading.ts
    - frontend/src/pages/trading/TradingRulesPage.tsx
decisions:
  - "backtested flag set lazily on first successful BacktestService.run() call when ruleId provided — avoids separate backtest_count table"
  - "DRAFT→PAPER_LIVE requires backtested=true guard — enforces at least one validation before live evaluation"
  - "RuleResponse includes backtested so frontend can show promote button state without extra API call"
metrics:
  duration: "3m"
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_created: 3
  files_modified: 6
---

# Phase 04 Plan 02: Rule Lifecycle Summary

**One-liner:** Rule lifecycle state machine (DRAFT→PAPER_LIVE→PAUSED) with backtested flag gating promotion, 4 REST endpoints, and TradingRulesPage with colored status badges and context-aware action buttons.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Lifecycle backend (V33 + backtested flag + service + controller) | 57f4b5a | V33 migration + TradingRule + RuleResponse + PaperRuleService + BacktestService + PaperLifecycleService + PaperLifecycleController |
| 2 | TradingRulesPage with status badges | 57f4b5a | trading.ts + TradingRulesPage.tsx |

## What Was Built

**V33 migration** — `ALTER TABLE trading_rules ADD COLUMN IF NOT EXISTS backtested BOOLEAN NOT NULL DEFAULT FALSE`

**`TradingRule.backtested`** field — set to `true` by `BacktestService.run()` on first successful run when `ruleId` is provided (lazy, idempotent: only saves if currently false).

**`RuleResponse.backtested`** — added so frontend can determine promote eligibility without extra API call. All `toResponse()` callers updated.

**`PaperLifecycleService`** — state machine with 4 transitions:
- `promote(userId, ruleId)`: DRAFT/BACKTESTED → PAPER_LIVE; rejects if `backtested=false` (ERR_LIFECYCLE_002)
- `pause(userId, ruleId)`: PAPER_LIVE → PAUSED; rejects if not PAPER_LIVE
- `resume(userId, ruleId)`: PAUSED → PAPER_LIVE; rejects if not PAUSED
- `copy(userId, ruleId)`: any status → new DRAFT copy with name "복사본 - {original}"

**`PaperLifecycleController`** — `POST /api/v1/trading/paper/rules/{id}/promote|pause|resume|copy`

**`trading.ts`** — `RuleStatus` extended: `"DRAFT" | "ACTIVE" | "PAUSED" | "BACKTESTED" | "PAPER_LIVE" | "LIVE"`; `TradingRule.backtested: boolean` added.

**`TradingRulesPage`** — full implementation replacing stub:
- Colored status badges: DRAFT=gray, ACTIVE=blue-dark, BACKTESTED=blue, PAPER_LIVE=green, PAUSED=yellow, LIVE=purple
- Per-rule action buttons: PAPER_LIVE 승격 (disabled if !backtested) | 일시 정지 | 재개 | 복사
- LIVE rules show "편집 불가" with tooltip
- Error banner for mutation failures
- Empty state and loading state

## Deviations from Plan

**[Rule 2 - Critical] RuleResponse missing backtested field**
- **Found during:** Task 1 — PaperLifecycleService needed to return RuleResponse with backtested
- **Fix:** Added `backtested` field to `RuleResponse` record; updated `PaperRuleService.toResponse()` and `PaperLifecycleService.toResponse()` to pass `rule.isBacktested()`
- **Files modified:** RuleResponse.java, PaperRuleService.java

## Self-Check

Files exist:
- backend/src/main/resources/db/migration/V33__rule_backtested_flag.sql — FOUND
- backend/src/main/java/com/graphify/trading/paper/PaperLifecycleService.java — FOUND
- backend/src/main/java/com/graphify/trading/paper/PaperLifecycleController.java — FOUND
- frontend/src/pages/trading/TradingRulesPage.tsx — FOUND

Commits: 57f4b5a — feat(04-02): rule lifecycle — promote/pause/resume/copy + TradingRulesPage with status badges

Full test suite: BUILD SUCCESSFUL

## Self-Check: PASSED
