---
phase: 04-dashboard-lifecycle-monitor-report
plan: "06"
subsystem: frontend-routing
tags: [routing, navigation, paper-mode, lifecycle, promote]
dependency_graph:
  requires: []
  provides: [paper/rules-lifecycle route, PAPER sidebar lifecycle entry]
  affects: [frontend/src/router/index.tsx, frontend/src/layouts/TradingLayout.tsx]
tech_stack:
  added: []
  patterns: [ModeGuard mode=PAPER, reuse existing page component, NavItem sidebar extension]
key_files:
  created: []
  modified:
    - frontend/src/router/index.tsx
    - frontend/src/layouts/TradingLayout.tsx
decisions:
  - "Reuse TradingRulesPage unchanged under a new PAPER-mode route rather than creating a new component"
  - "Insert lifecycle sidebar entry between 모의 룰 설정 and 백테스트 so promote action is adjacent to rule editing"
metrics:
  duration: "31s"
  completed_date: "2026-06-21"
  tasks_completed: 1
  files_modified: 2
requirements:
  - RULE-01
  - RULE-02
  - RULE-03
---

# Phase 4 Plan 06: PAPER-mode Lifecycle Route Summary

**One-liner:** Expose existing TradingRulesPage (promote/pause/resume/copy) to PAPER-mode users via a new `/trading/paper/rules-lifecycle` route and "룰 라이프사이클" sidebar entry.

## What Was Built

PAPER-mode users previously had no path to the "PAPER_LIVE 승격" (promote) action because `TradingRulesPage` was gated under `ModeGuard mode="LIVE"` and only linked from the LIVE sidebar. This plan adds:

1. A new route `paper/rules-lifecycle` under `ModeGuard mode="PAPER"` that renders the existing `TradingRulesPage` component with zero component changes.
2. A new PAPER sidebar entry `{ to: "/trading/paper/rules-lifecycle", label: "룰 라이프사이클" }` inserted after "모의 룰 설정" in `paperItems`.

LIVE routes (`rules` under `ModeGuard mode="LIVE"`) and `liveItems` are unchanged.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Add PAPER-mode lifecycle route + sidebar entry | 0aa8098 | frontend/src/router/index.tsx, frontend/src/layouts/TradingLayout.tsx |

## Verification

- `npx tsc --noEmit` — passed clean (no output)
- New route `paper/rules-lifecycle` renders `TradingRulesPage` under `ModeGuard mode="PAPER"`
- `paperItems` contains "룰 라이프사이클" entry after "모의 룰 설정"
- `liveItems` and existing LIVE `rules` route are unchanged

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

- [x] `frontend/src/router/index.tsx` modified — confirmed (9 lines added)
- [x] `frontend/src/layouts/TradingLayout.tsx` modified — confirmed (1 line added)
- [x] Commit 0aa8098 exists — confirmed
- [x] `tsc --noEmit` clean — confirmed
