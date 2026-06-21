---
phase: "06"
plan: "02"
subsystem: frontend-routing-rules
tags: [router, paper-rules, navigate, copy-rule, cooldown-display]
dependency_graph:
  requires: ["06-01"]
  provides: ["paper/rules/new route", "paper/rules/edit/:id route", "rule copy button", "cooldown column"]
  affects: ["frontend/src/router/index.tsx", "frontend/src/pages/trading/paper/PaperRulesPage.tsx"]
tech_stack:
  added: []
  patterns: ["useNavigate for page transitions", "useMutation for copyRule side-effect"]
key_files:
  created: []
  modified:
    - frontend/src/router/index.tsx
    - frontend/src/pages/trading/paper/PaperRulesPage.tsx
decisions:
  - "copyMutation uses per-rule id only — no optimistic update; invalidateQueries re-fetches list after copy"
  - "cooldown display formula: cooldownBars * 5m (5-minute bar assumption, consistent with backtest engine)"
  - "table column order: 이름 | 상태 | 쿨다운 | 수정일 | 관리 (5 columns, thead/tbody aligned)"
metrics:
  duration: "2m"
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_modified: 2
---

# Phase 6 Plan 02: Router Wiring + PaperRulesPage Cleanup Summary

**One-liner:** Wired RuleBuilderPage into PAPER router via new/edit routes and replaced PaperRulesPage inline JSON modal with navigate-based flows, rule copy button, and cooldown column.

## What Was Built

- **Task 1** — `router/index.tsx`: Added `paper/rules/new` and `paper/rules/edit/:id` routes, both wrapped in `<ModeGuard mode="PAPER">` with `TradingRulesEditPage` as element (already imported at line 12).
- **Task 2** — `PaperRulesPage.tsx`: Removed all inline modal code (EditorState, TEMPLATE, STATUS_OPTIONS, saveMutation, openCreate, openEdit, fixed-inset JSX). Replaced with:
  - `useNavigate` calls to `/trading/paper/rules/new` and `/trading/paper/rules/edit/:id`
  - `copyMutation` calling `copyRule(id)` from `@/lib/paperApi` (RULE-06)
  - 쿨다운 column displaying `cooldownBars봉 (cooldownBars×5m)` or `—` (RULE-07)
  - Three-button row: 편집 | 복제 | 삭제

## Commits

| Task | Commit | Message |
|------|--------|---------|
| 1 | d1ea88a | feat(06-02): add paper/rules/new and paper/rules/edit/:id routes |
| 2 | 9bc8052 | feat(06-02): refactor PaperRulesPage — remove modal, add navigate/copy/cooldown |

## Verification

- `npx tsc --noEmit` exits 0 after both tasks
- `grep` confirms zero occurrences of EditorState/TEMPLATE/fixed-inset-0 in PaperRulesPage
- `grep` confirms copyMutation, navigate calls, cooldownBars, 쿨다운 all present at expected lines

## Deviations from Plan

None — plan executed exactly as written.

## Requirements Fulfilled

- RULE-06: 복제 button calls `copyRule(id)` creating DRAFT copy
- RULE-07: 쿨다운 column shows `N봉 (Xm)` format

## Self-Check: PASSED

- `frontend/src/router/index.tsx` — FOUND (modified, contains paper/rules/new and paper/rules/edit/:id)
- `frontend/src/pages/trading/paper/PaperRulesPage.tsx` — FOUND (rewritten, no modal, has copyMutation + navigate + cooldown)
- Commit d1ea88a — FOUND
- Commit 9bc8052 — FOUND
