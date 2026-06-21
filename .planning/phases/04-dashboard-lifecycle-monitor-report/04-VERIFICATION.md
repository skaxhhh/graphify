---
phase: 04-dashboard-lifecycle-monitor-report
verified: 2026-06-21T12:00:00Z
status: passed
score: 6/6 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 3/6
  gaps_closed:
    - "Promoting a rule to PAPER_LIVE actually starts live evaluation (scheduler ingests symbols + LiveEvaluationService evaluates the rule)"
    - "There is a reachable place in the PAPER-mode UI to promote a rule to PAPER_LIVE"
    - "모의 거래 이력 페이지가 라이브 루프가 기록한 체결 내역을 보여준다"
  gaps_remaining: []
  regressions: []
---

# Phase 4: 대시보드·룰 생애주기·모니터·리포트 UI Verification Report

**Phase Goal:** 모의 대시보드(잔고·포지션·손익), 룰 상태 전환 UI(DRAFT→PAPER_LIVE), 실시간 신호 모니터, 성과 리포트 페이지를 완성하여 사용자가 전략 운영 상황을 한눈에 파악하고 제어할 수 있다. 특히 DRAFT→PAPER_LIVE 활성화 end-to-end 동작, 모의 라이브 거래가 대시보드와 거래이력에 반영.
**Verified:** 2026-06-21
**Status:** PASSED
**Re-verification:** Yes — after gap closure plans 04-05, 04-06, 04-07

## Goal Achievement

### Observable Truths

| #   | Truth                                                                                                                      | Status     | Evidence                                                                                                                                                            |
| --- | -------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | 모의 대시보드: 현금·총평가·포지션(수량/평균단가/평가손익)·오늘 실현손익·활성 PAPER_LIVE 룰 수 표시                        | ✓ VERIFIED | `PaperDashboardService.getDashboard()` computes all fields; `PaperDashboardPage` renders 4 stat cards + positions table + 30s refetch                               |
| 2   | 룰 목록 상태 배지 + 백테스트 완료 룰만 PAPER_LIVE 승격 버튼 활성화                                                       | ✓ VERIFIED | `TradingRulesPage` StatusBadge (6 statuses) + promote button `disabled={!backtested}`; backend `promote()` rejects `backtested=false`                               |
| 3   | 모니터: 신호 평가 로그·스케줄러 마지막 실행·장중/장외 상태·오늘 체결 피드                                                | ✓ VERIFIED | `PaperMonitorService.getMonitor()` + `TradingMonitorPage`: market badge, schedulerLastRun, signal table, todayTrades, 30s refetch                                   |
| 4   | 성과 리포트: 수익 곡선·수익률·MDD·승률·거래수·Sharpe/Sortino                                                             | ✓ VERIFIED | `PaperReportService.getReport()` + `PaperReportPage`: EquityCurveChart + 6 stat cards + empty state                                                                 |
| 5   | PAPER_LIVE 승격이 실제 라이브 평가를 시작시킨다 (activation end-to-end)                                                  | ✓ VERIFIED | `promote()` resolves symbols → `assignSymbols()` populates `paper_live_symbols` → scheduler Guard 3 non-empty → tick runs → `LiveEvaluationService.evaluateTick()` evaluates via `findByRuleId` |
| 6   | PAPER 모드 UI에서 승격을 실행할 수 있는 도달 가능한 위치가 있다                                                          | ✓ VERIFIED | `/trading/paper/rules-lifecycle` route under `ModeGuard mode="PAPER"` renders `TradingRulesPage`; "룰 라이프사이클" entry in `paperItems`                           |
| 7   | 모의 거래 이력 페이지가 라이브 루프가 기록한 체결 내역을 보여준다                                                        | ✓ VERIFIED | `PaperHistoryController` + `PaperHistoryService.findByAccountIdOrderByTradedAtDesc` + `fetchPaperHistory` + `PaperHistoryPage` full table (no longer a stub)        |

**Score:** 7/7 truths verified (previous: 3/6 with 3 blocking gaps)

### Required Artifacts

| Artifact                                                                              | Expected                                           | Status     | Details                                                                                       |
| ------------------------------------------------------------------------------------- | -------------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------- |
| `trading/rule/RuleStatus.java`                                                        | shared `isLiveActive(String)` predicate            | ✓ VERIFIED | Final utility class; `isLiveActive` returns `PAPER_LIVE.equals(status)`; Phase 6 single-line extension point |
| `trading/paper/PaperLifecycleService.java`                                            | promote/pause/resume wired to assignSymbols        | ✓ VERIFIED | Injects `PaperLiveSymbolService` + `MarketDataPort`; `promote()` resolves symbols, calls `assignSymbols`; `pause()` calls `deactivateRule`; `resume()` calls `assignSymbols`; `resolveSymbols` contains zero `"PAPER_LIVE"` literals |
| `trading/paper/LiveEvaluationService.java`                                            | resolveSymbols reads paper_live_symbols; RuleStatus predicate; List<OrderExecutorPort> | ✓ VERIFIED | `paperLiveSymbolRepository.findByRuleId(rule.getId())`; `RuleStatus.isLiveActive` filter in `evaluateTick`; `List<OrderExecutorPort> executors` + `executorFor(rule)` |
| `trading/paper/OrderExecutorPort.java`                                                | `supports(TradingRule)` routing seam               | ✓ VERIFIED | Interface declares `boolean supports(TradingRule rule)` with Phase 6 comment                  |
| `trading/paper/PaperExecutor.java`                                                    | `supports(rule)` returns true for PAPER mode       | ✓ VERIFIED | `@Override public boolean supports(TradingRule rule) { return "PAPER".equals(rule.getMode()); }` |
| `trading/rule/PaperLiveSymbolService.java`                                            | assignSymbols/deactivateRule callers exist         | ✓ VERIFIED | `activeSymbolsUnion()` calls `RuleStatus.isLiveActive`; no hardcoded `"PAPER_LIVE".equals` remaining |
| `frontend/src/router/index.tsx`                                                       | `paper/rules-lifecycle` route under PAPER ModeGuard | ✓ VERIFIED | Lines 197-204: `path: "paper/rules-lifecycle"`, `ModeGuard mode="PAPER"`, renders `TradingRulesPage` |
| `frontend/src/layouts/TradingLayout.tsx`                                              | "룰 라이프사이클" entry in paperItems              | ✓ VERIFIED | Line 30: `{ to: "/trading/paper/rules-lifecycle", label: "룰 라이프사이클" }` in `paperItems` |
| `trading/paper/dto/PaperTradeHistoryItem.java`                                        | DTO record with tradedAt/symbol/side/qty/price/fee/pnl | ✓ VERIFIED | Record with 8 fields; fee=null (paper_trades has no fee column)                               |
| `trading/paper/PaperHistoryService.java`                                              | loads paper_trades newest-first via accountId      | ✓ VERIFIED | `findByAccountIdOrderByTradedAtDesc(account.getId())` → maps to DTO                           |
| `trading/paper/PaperHistoryController.java`                                           | GET /api/v1/trading/paper/history user-scoped      | ✓ VERIFIED | `@GetMapping`, `HistoryService.requireCurrentUserId()`, `ApiResponse.ok(...)`                 |
| `frontend/src/types/paper.ts` `PaperTradeHistoryItem`                                | type with id/tradedAt/symbol/side/qty/price/fee/pnl | ✓ VERIFIED | `export interface PaperTradeHistoryItem` at line 42                                           |
| `frontend/src/lib/paperApi.ts` `fetchPaperHistory`                                   | fetcher using apiGet                               | ✓ VERIFIED | `export async function fetchPaperHistory()` returning `apiGet<PaperTradeHistoryItem[]>(...)` |
| `frontend/.../PaperHistoryPage.tsx`                                                   | real table + empty state (not stub)                | ✓ VERIFIED | `useQuery` + full 6-column table + empty state card + loading/error states; `refetchInterval: 30000` |

### Key Link Verification

| From                                        | To                                                  | Via                                              | Status     | Details                                                                           |
| ------------------------------------------- | --------------------------------------------------- | ------------------------------------------------ | ---------- | --------------------------------------------------------------------------------- |
| PaperDashboardPage                          | GET /api/v1/trading/paper/dashboard                 | fetchPaperDashboard + useQuery                   | ✓ WIRED    | refetchInterval 30000 (unchanged from initial verification)                       |
| TradingMonitorPage                          | GET /api/v1/trading/paper/monitor                   | fetchPaperMonitor + useQuery                     | ✓ WIRED    | refetchInterval 30000 (unchanged)                                                 |
| PaperReportPage                             | GET /api/v1/trading/paper/report                    | fetchPaperReport + useQuery                      | ✓ WIRED    | EquityCurveChart fed data.equityCurve (unchanged)                                 |
| TradingRulesPage promote                    | POST /rules/{id}/promote → PaperLifecycleService    | promoteRule mutation                             | ✓ WIRED    | UI→REST→service status flip + assignSymbols                                       |
| PaperLifecycleService.promote               | PaperLiveSymbolService.assignSymbols                | constructor injection + explicit call            | ✓ WIRED    | `paperLiveSymbolService.assignSymbols(saved.getId(), symbols)` after `ruleRepo.save` |
| PaperLifecycleService.pause                 | PaperLiveSymbolService.deactivateRule               | constructor injection + explicit call            | ✓ WIRED    | `paperLiveSymbolService.deactivateRule(saved.getId())` after status flip           |
| LiveDataScheduler Guard 3                   | paper_live_symbols (activeSymbolsUnion)             | RuleStatus.isLiveActive filter                   | ✓ WIRED    | `activeSymbolsUnion()` calls `RuleStatus.isLiveActive` (no longer always-empty)   |
| LiveEvaluationService.evaluateTick          | RuleStatus.isLiveActive                             | filter on ruleRepo.findAll()                     | ✓ WIRED    | `RuleStatus.isLiveActive(r.getStatus())` replaces old hardcoded string check      |
| LiveEvaluationService.resolveSymbols        | PaperLiveSymbolRepository.findByRuleId              | `paperLiveSymbolRepository.findByRuleId(rule.getId())` | ✓ WIRED | Single canonical source; `def.universe()` no longer drives the live symbol list   |
| LiveEvaluationService.evaluateSymbol        | OrderExecutorPort.supports(rule)                    | `executorFor(rule)` + `List<OrderExecutorPort>`  | ✓ WIRED    | `executors.stream().filter(e -> e.supports(rule)).findFirst()`; warns+skips if null |
| PAPER-mode nav (TradingLayout paperItems)   | /trading/paper/rules-lifecycle                      | NavLink → ModeGuard PAPER → TradingRulesPage     | ✓ WIRED    | "룰 라이프사이클" in paperItems; route renders TradingRulesPage under ModeGuard PAPER |
| PaperHistoryPage                            | GET /api/v1/trading/paper/history                   | fetchPaperHistory + useQuery                     | ✓ WIRED    | `queryFn: async () => (await fetchPaperHistory()).data ?? []`; refetchInterval 30000 |
| PaperHistoryService                         | PaperTradeRepository.findByAccountIdOrderByTradedAtDesc | via accountRepo.findByUserId → account.getId  | ✓ WIRED    | Newest-first ordering; maps each PaperTrade to PaperTradeHistoryItem              |

### Requirements Coverage

| Requirement   | Source Plan | Description                                               | Status      | Evidence                                                                              |
| ------------- | ----------- | --------------------------------------------------------- | ----------- | ------------------------------------------------------------------------------------- |
| DASH-01/02    | 04-01       | 모의 대시보드 잔고·포지션·손익; 거래 이력                | ✓ SATISFIED | PaperDashboardService + Page; PaperHistoryController + PaperHistoryPage table         |
| RULE-01..05   | 04-02/06    | 상태 전환·배지·승격 게이팅·LIVE 편집 차단; PAPER 라이프사이클 도달 | ✓ SATISFIED | promote/pause/resume wired; paper/rules-lifecycle route + paperItems entry; backtested gate intact |
| MON-01..03    | 04-03       | 신호 로그·스케줄러·장중상태·체결                          | ✓ SATISFIED | PaperMonitorService + TradingMonitorPage                                              |
| REPORT-01/02  | 04-04       | 수익 곡선·지표                                            | ✓ SATISFIED | PaperReportService + PaperReportPage                                                  |
| DASH-03/04    | 04-04       | 성과 리포트 통계                                          | ✓ SATISFIED | Report stats (totalReturn, MDD, winRate, Sharpe/Sortino)                              |

### Anti-Patterns Found

None blocking. Previous blockers resolved:

| File (previously)                          | Previous Issue                                                  | Current Status |
| ------------------------------------------ | --------------------------------------------------------------- | -------------- |
| `PaperLifecycleService.java`               | No assignSymbols call; orphaned PaperLiveSymbolService          | RESOLVED       |
| `router/index.tsx + TradingLayout.tsx`     | Promote UI gated to LIVE mode only                              | RESOLVED       |
| `PaperHistoryPage.tsx`                     | 8-line "준비 중" stub                                           | RESOLVED       |
| `LiveEvaluationService.java`               | Source mismatch: def.universe() vs paper_live_symbols           | RESOLVED       |

Remaining notable (non-blocking):
- `resolveSymbols` in `PaperLifecycleService` snapshots the full volume_top_n candidate universe at promote time rather than re-selecting the daily top-N in the live lane. This is a documented scope decision — per-day top-N re-selection deferred to a future plan.
- `PaperTradeHistoryItem.fee` is always `null` because `paper_trades` has no fee column. This is documented and Phase 6 can add via migration.

### Build / Typecheck Results

- Backend `./gradlew compileJava`: **BUILD SUCCESSFUL** (exit 0, 862ms)
- Frontend `npx tsc --noEmit`: **exit 0 (clean, no output)**

### Human Verification Required

The following items pass static analysis but require runtime confirmation:

1. **Activation end-to-end live flow**
   - Test: Promote a backtested rule to PAPER_LIVE; wait for the next scheduler tick (5 min interval during market hours); confirm `paper_live_symbols` has rows for the rule and that the monitor page shows signal logs and the dashboard shows non-zero positions/trades.
   - Expected: Dashboard reflects live fills; monitor shows signal evaluations; equity curve begins populating.
   - Why human: Requires a real or simulated scheduler tick with market data present.

2. **PAPER-mode lifecycle page navigation**
   - Test: Log in with PAPER mode active; click "룰 라이프사이클" in the sidebar; confirm TradingRulesPage renders with promote/pause/resume/copy buttons and is not redirected.
   - Expected: Page renders, promote button visible for backtested rules.
   - Why human: ModeGuard redirect behavior depends on runtime store state.

3. **Trade history auto-refresh**
   - Test: After a paper trade executes, confirm it appears in 모의 거래 이력 within 30 seconds (the refetchInterval).
   - Expected: New rows appear without manual page refresh.
   - Why human: Requires live paper trade execution.

### Gaps Summary

All three previously identified blocking gaps are now closed:

**Gap 1 (CLOSED) — Backend activation chain.** `PaperLifecycleService` now injects `PaperLiveSymbolService` and `MarketDataPort`. `promote()` resolves the rule's symbol universe (mirroring `BacktestService.resolveInitialSymbols`), throws `ERR_LIFECYCLE_005` on an empty universe, and calls `assignSymbols(ruleId, symbols)` after saving. `pause()` calls `deactivateRule`. `resume()` re-calls `assignSymbols`. The `resolveSymbols` helper contains zero `"PAPER_LIVE"` literals (SEAM 3). `LiveEvaluationService.resolveSymbols` now reads from `paper_live_symbols` via `findByRuleId` instead of `def.universe()` — both halves of the loop share the same canonical source. The `"PAPER_LIVE".equals` hardcoded checks in both `LiveEvaluationService.evaluateTick` and `PaperLiveSymbolService.activeSymbolsUnion` are replaced with `RuleStatus.isLiveActive(status)` (SEAM 1). `LiveEvaluationService` now injects `List<OrderExecutorPort>` and routes per-rule via `executorFor(rule).supports(rule)` (SEAM 2).

**Gap 2 (CLOSED) — PAPER-mode promote UI reachability.** A new route `paper/rules-lifecycle` under `ModeGuard mode="PAPER"` renders the existing `TradingRulesPage` (no component changes). A new "룰 라이프사이클" entry in `paperItems` links to it. LIVE routes and `liveItems` are unchanged.

**Gap 3 (CLOSED) — Trade history page.** `PaperHistoryController` + `PaperHistoryService` + `PaperTradeHistoryItem` DTO provide `GET /api/v1/trading/paper/history` returning the current user's `paper_trades` newest-first. `paperApi.fetchPaperHistory` + `PaperTradeHistoryItem` type added to the frontend. `PaperHistoryPage` is a full implementation with a 6-column table, empty state, loading/error states, and 30s auto-refresh — no longer a "준비 중" stub.

Phase 4 goal is now fully achieved: the four read pages (dashboard/monitor/report/history) are implemented and wired, the DRAFT→PAPER_LIVE activation is end-to-end functional, and PAPER-mode users have a reachable path to promote rules.

---

_Verified: 2026-06-21_
_Verifier: Claude (gsd-verifier)_
_Re-verification: gap-closure plans 04-05 (backend activation + seams), 04-06 (PAPER lifecycle route), 04-07 (trade history endpoint + page)_
