---
phase: 06-rule-builder-ui
plan: "01"
subsystem: frontend-rule-builder
tags: [react, typescript, tailwind, rule-builder, form-ui]
dependency_graph:
  requires: []
  provides: [RuleBuilderPage, toDefinition, fromDefinition, volume_top_n-universe-type]
  affects: [frontend/src/types/trading.ts, frontend/src/pages/trading/TradingRulesEditPage.tsx]
tech_stack:
  added: []
  patterns: [useState-form-state, useQuery-edit-load, useMutation-save, pure-serialization-functions]
key_files:
  created: []
  modified:
    - frontend/src/types/trading.ts
    - frontend/src/pages/trading/TradingRulesEditPage.tsx
decisions:
  - "ConditionRow extracted as sub-component to avoid inline JSX repetition for entry/exit rows"
  - "INPUT_CLS single constant covers both input and select styles — SELECT_CLS alias removed as unnecessary"
  - "toDefinition/fromDefinition defined outside component as pure functions — no React dependencies, trivially testable"
  - "universe.market hardcoded to KOSPI in toDefinition — only KOSPI supported in v1; can be promoted to BuilderState field in Phase 7"
metrics:
  duration: 3m
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_modified: 2
---

# Phase 6 Plan 01: Rule Builder UI Summary

**One-liner:** Full RuleBuilderPage form with bidirectional BuilderState↔RuleDefinition serialization, builder/JSON tab toggle, and volume_top_n universe type support.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Extend trading.ts types for volume_top_n universe | 477f9bc | frontend/src/types/trading.ts |
| 2 | Implement RuleBuilderPage in TradingRulesEditPage.tsx | 19345f9 | frontend/src/pages/trading/TradingRulesEditPage.tsx |

## What Was Built

### Task 1: Type Extension (trading.ts)
- Added `"volume_top_n"` to `UniverseType` union
- Extended `RuleDefinition.universe` with `market?: string`, `topN?: number`, `additionalSymbols?: string[]`
- No other changes to the file — targeted modification only

### Task 2: RuleBuilderPage (TradingRulesEditPage.tsx)
Replaced 8-line stub with 838-line full implementation:

**Internal State:**
- `ConditionRowState` — per-row form state for condition builder (left indicator/period, operator, right type/value/indicator/period)
- `BuilderState` — aggregate form state covering name, universe, entry conditions, exit conditions (including takeProfitPct/stopLossPct), sizing, and constraints

**Serialization (pure functions):**
- `toDefinition(s: BuilderState): RuleDefinition` — converts builder form state to backend-compatible RuleDefinition; skips incomplete condition rows; handles NaN-safe float parsing; conditionally builds exit spec
- `fromDefinition(def: RuleDefinition, name: string): BuilderState` — inverse; reconstructs form state from API response for edit mode; falls back to emptyConditionRow() when no conditions exist

**UI Sections (dark Tailwind theme, card layout):**
1. 룰 이름 — text input
2. 유니버스 — radio toggle between `symbols` (comma-separated codes) and `volume_top_n` (topN + additionalSymbols)
3. 진입 조건 — AND/OR logic select + multi-row ConditionRow list with add/remove
4. 청산 조건 — takeProfitPct + stopLossPct inputs always visible; optional indicator condition rows with logic select
5. 사이징 — sizingType select + value input
6. 제약 — cooldownBars + maxPositionsPerSymbol side-by-side inputs

**Tab Toggle:**
- Builder → JSON: serializes current BuilderState via `toDefinition` into pretty-printed textarea
- JSON → Builder: parses textarea, calls `fromDefinition`, shows error message on invalid JSON

**React Query integration:**
- `useQuery` loads existing rule in edit mode (`isEdit = id !== undefined`)
- `useEffect` hydrates BuilderState via `fromDefinition` when rule data arrives
- `useMutation` calls `createPaperRule` (create) or `updatePaperRule` (edit); navigates to `/trading/paper/rules` on success; shows error message on failure

**Validation in handleSave:**
- Rule name must not be empty
- At least 1 valid entry condition (leftIndicator + op both set)
- sizingValue must parse as a valid float

## Deviations from Plan

None — plan executed exactly as written.

## Verification

- `npx tsc --noEmit` exits 0 (no TypeScript errors)
- `trading.ts` contains `"volume_top_n"` in UniverseType and `topN?`, `additionalSymbols?` in universe
- `TradingRulesEditPage.tsx` is 838 lines (well above 200-line minimum)
- All key patterns present: `toDefinition`, `fromDefinition`, `ConditionRowState`, `BuilderState`, `switchToJson`, `switchToBuilder`, `saveMutation`

## Self-Check: PASSED

- FOUND: frontend/src/types/trading.ts
- FOUND: frontend/src/pages/trading/TradingRulesEditPage.tsx
- FOUND: .planning/phases/06-rule-builder-ui/06-01-SUMMARY.md
- FOUND: commit 477f9bc (feat(06-01): extend UniverseType)
- FOUND: commit 19345f9 (feat(06-01): implement RuleBuilderPage)
