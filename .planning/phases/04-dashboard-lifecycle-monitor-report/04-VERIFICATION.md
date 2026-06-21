---
phase: 04-dashboard-lifecycle-monitor-report
verified: 2026-06-21T00:00:00Z
status: gaps_found
score: 3/6 must-haves verified
gaps:
  - truth: "Promoting a rule to PAPER_LIVE actually starts live evaluation (scheduler ingests symbols + LiveEvaluationService evaluates the rule)"
    status: failed
    reason: "PaperLifecycleService.promote() flips status to PAPER_LIVE but never calls PaperLiveSymbolService.assignSymbols(). The scheduler's Guard 3 (symbolService.activeSymbolsUnion(), reading paper_live_symbols) returns empty, so the WHOLE tick is skipped — no ingestion, no evaluateTick. paper_live_symbols is never populated by any production caller, so a promoted rule never runs live."
    artifacts:
      - path: "backend/src/main/java/com/graphify/trading/paper/PaperLifecycleService.java"
        issue: "promote()/pause()/resume() do not inject or call PaperLiveSymbolService.assignSymbols()/deactivateRule(); status change is the only side effect"
      - path: "backend/src/main/java/com/graphify/market/LiveDataScheduler.java"
        issue: "Guard 3 short-circuits the entire tick when activeSymbolsUnion() is empty, which it always is because nothing populates paper_live_symbols"
      - path: "backend/src/main/java/com/graphify/trading/rule/PaperLiveSymbolService.java"
        issue: "assignSymbols()/deactivateRule() have ZERO production callers (carried over unfixed from Phase 2 note)"
    missing:
      - "PaperLifecycleService.promote(): parse rule.definition, resolve its symbols, call paperLiveSymbolService.assignSymbols(ruleId, symbols)"
      - "PaperLifecycleService.pause()/copy()-on-paused: call paperLiveSymbolService.deactivateRule(ruleId)"
      - "PaperLifecycleService.resume(): re-call assignSymbols(ruleId, symbols)"
      - "Inject PaperLiveSymbolService into PaperLifecycleService (currently only TradingRuleRepository + ObjectMapper)"
      - "Reconcile symbol source: LiveEvaluationService.resolveSymbols() reads from rule.definition.universe while scheduler ingests from paper_live_symbols — both must derive from the same source so the symbols ingested are the symbols evaluated"
  - truth: "There is a reachable place in the PAPER-mode UI to promote a rule to PAPER_LIVE"
    status: failed
    reason: "The only UI with a promote button (TradingRulesPage at /trading/rules) is wrapped in ModeGuard mode=\"LIVE\" and only appears in the LIVE-mode sidebar. In the default PAPER mode, navigating to /trading/rules redirects to /trading/paper/dashboard. The PAPER-mode rule page (PaperRulesPage at /trading/paper/rules) is a CRUD editor whose status dropdown only offers DRAFT/ACTIVE/PAUSED — no PAPER_LIVE promote action exists there."
    artifacts:
      - path: "frontend/src/router/index.tsx"
        issue: "TradingRulesPage (the lifecycle/promote UI) is routed at /trading/rules under ModeGuard mode=\"LIVE\" — unreachable while mode=PAPER"
      - path: "frontend/src/layouts/TradingLayout.tsx"
        issue: "\"현재 룰\" (/trading/rules) is in liveItems only; paperItems links to /trading/paper/rules (PaperRulesPage, no promote)"
      - path: "frontend/src/pages/trading/paper/PaperRulesPage.tsx"
        issue: "STATUS_OPTIONS = [DRAFT, ACTIVE, PAUSED]; no promote/PAPER_LIVE control"
    missing:
      - "Expose TradingRulesPage (lifecycle list with promote/pause/resume/copy) in PAPER mode — either move it to /trading/paper/rules-lifecycle and add to paperItems, or change the ModeGuard so the lifecycle page is reachable in PAPER mode"
      - "Add a 'PAPER_LIVE 승격' affordance reachable from the PAPER-mode navigation"
  - truth: "모의 거래 이력 페이지가 라이브 루프가 기록한 체결 내역을 보여준다"
    status: failed
    reason: "PaperHistoryPage is an 8-line placeholder ('모의 체결 내역 — 준비 중'). There is no trade-history REST endpoint and no API client function; the page renders nothing. Live paper trades written to paper_trades by PaperExecutor never surface in a history view."
    artifacts:
      - path: "frontend/src/pages/trading/paper/PaperHistoryPage.tsx"
        issue: "8-line stub, no useQuery, no table — '준비 중' placeholder only"
      - path: "backend/src/main/java/com/graphify/trading/paper/PaperDashboardController.java"
        issue: "No trade-history endpoint exists (dashboard/monitor/report/lifecycle controllers only); paper_trades is not exposed as a paginated history list"
      - path: "frontend/src/lib/paperApi.ts"
        issue: "No fetchPaperHistory/fetchPaperTrades client function"
    missing:
      - "Backend: GET paper trade-history endpoint returning paper_trades for the current user (time/symbol/side/qty/price/fee/realized PnL), newest first"
      - "Frontend: paperApi fetch function + types"
      - "Frontend: implement PaperHistoryPage with a trades table + empty state (replace the '준비 중' stub)"
---

# Phase 4: 대시보드·룰 생애주기·모니터·리포트 UI Verification Report

**Phase Goal:** 모의 대시보드(잔고·포지션·손익), 룰 상태 전환 UI(DRAFT→PAPER_LIVE→LIVE), 실시간 신호 모니터, 성과 리포트 페이지를 완성하여 사용자가 전략 운영 상황을 한눈에 파악하고 제어할 수 있다.
**Verified:** 2026-06-21
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (from ROADMAP Success Criteria)

| #   | Truth                                                                                                       | Status     | Evidence                                                                                                                                                            |
| --- | --------------------------------------------------------------------------------------------------------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | 모의 대시보드: 현금·총평가·포지션(수량/평균단가/평가손익)·오늘 실현손익·활성 PAPER_LIVE 룰 수 표시 | ✓ VERIFIED | `PaperDashboardService.getDashboard()` computes all fields incl. mark-to-market; `PaperDashboardPage` renders 4 stat cards + positions table + 30s refetch         |
| 2   | 룰 목록 상태 배지 + 백테스트 완료 룰만 PAPER_LIVE 승격 버튼 활성화                                  | ✓ VERIFIED | `TradingRulesPage` StatusBadge (6 statuses) + promote button `disabled={!backtested}`; backend `promote()` rejects `backtested=false` (ERR_LIFECYCLE_002)           |
| 3   | 모니터: 신호 평가 로그·스케줄러 마지막 실행·장중/장외 상태·오늘 체결 피드                            | ✓ VERIFIED | `PaperMonitorService.getMonitor()` + `TradingMonitorPage`: market badge, schedulerLastRun, signal table (rsi14/sma20/price), today trades, 30s refetch             |
| 4   | 성과 리포트: 수익 곡선·수익률·MDD·승률·거래수·Sharpe/Sortino                                         | ✓ VERIFIED | `PaperReportService.getReport()` + `PaperReportPage`: EquityCurveChart reuse + 6 stat cards + empty state                                                          |
| 5   | LIVE 룰 편집 비활성 + DRAFT 복사본 안내                                                              | ✓ VERIFIED | `TradingRulesPage` LIVE → "편집 불가" with tooltip; `copy()` endpoint creates "복사본 - {name}" DRAFT                                                               |
| —   | **PAPER_LIVE 승격이 실제 라이브 평가를 시작시킨다 (activation end-to-end)**                          | ✗ FAILED   | promote() never assigns symbols → scheduler Guard 3 skips every tick → no ingestion/evaluation/signals/trades/snapshots (see Gaps)                                  |
| —   | **PAPER 모드 UI에서 승격을 실행할 수 있는 도달 가능한 위치가 있다**                                  | ✗ FAILED   | promote UI is LIVE-mode-gated and absent from PAPER nav (see Gaps)                                                                                                  |

**Score:** 5/5 ROADMAP page-level criteria render correctly, BUT 2/2 derived activation truths FAIL. Overall: **3/5 must-haves** (the lifecycle promo is cosmetically present but non-functional end-to-end and unreachable in PAPER mode).

### Required Artifacts

| Artifact                                                                  | Expected                                  | Status     | Details                                                                  |
| ------------------------------------------------------------------------- | ----------------------------------------- | ---------- | ----------------------------------------------------------------------- |
| `paper/PaperDashboardController.java` + `PaperDashboardService.java`       | GET /dashboard, mark-to-market            | ✓ VERIFIED | All fields computed; latest 5m bar mark price; KST midnight today filter |
| `paper/dto/PaperDashboardDto.java` + `PaperPositionItem.java`             | response DTOs                             | ✓ VERIFIED | Records present incl. `empty()` factory                                  |
| `paper/PaperLifecycleController.java`                                      | promote/pause/resume/copy endpoints       | ✓ VERIFIED | 4 POST endpoints under /api/v1/trading/paper/rules/{id}                  |
| `paper/PaperLifecycleService.java`                                        | state machine + assignSymbols wiring      | ⚠️ STUB    | State transitions correct BUT does NOT wire into live evaluation        |
| `db/migration/V33__rule_backtested_flag.sql`                             | backtested column                         | ✓ VERIFIED | ADD COLUMN IF NOT EXISTS backtested BOOLEAN NOT NULL DEFAULT FALSE       |
| `backtest/BacktestService.java`                                          | sets backtested=true                      | ✓ VERIFIED | Lazy idempotent set on successful run when ruleId provided (lines 103-112)|
| `paper/PaperMonitorController.java` + `PaperMonitorService.java` + DTOs   | GET /monitor                              | ✓ VERIFIED | market status, schedulerLastRun, recentSignals (JSON parsed), todayTrades |
| `paper/PaperReportController.java` + `PaperReportService.java` + ReportDto | GET /report                               | ✓ VERIFIED | equity curve (30d), totalReturn, MDD, winRate, Sharpe/Sortino           |
| `frontend/.../PaperDashboardPage.tsx`                                     | dashboard UI                              | ✓ VERIFIED | Routed, navigated (모의 대시보드), useQuery 30s                          |
| `frontend/.../TradingRulesPage.tsx`                                       | lifecycle list + promote                  | ⚠️ ORPHANED| Fully implemented BUT unreachable in PAPER mode (ModeGuard LIVE)         |
| `frontend/.../TradingMonitorPage.tsx`                                     | monitor UI                                | ⚠️ PARTIAL | Implemented + routed; nav link only in LIVE mode (동작 모니터링)        |
| `frontend/.../PaperReportPage.tsx`                                        | report UI                                 | ✓ VERIFIED | Routed, navigated (모의 성과 리포트), EquityCurveChart reuse             |
| `frontend/src/lib/paperApi.ts` + `types/paper.ts`                        | API + types                               | ✓ VERIFIED | All fetchers + lifecycle calls; correct base paths                      |

### Key Link Verification

| From                       | To                                         | Via                                | Status      | Details                                                                 |
| -------------------------- | ------------------------------------------ | ---------------------------------- | ----------- | ---------------------------------------------------------------------- |
| PaperDashboardPage         | GET /api/v1/trading/paper/dashboard        | fetchPaperDashboard + useQuery     | ✓ WIRED     | refetchInterval 30000                                                  |
| TradingMonitorPage         | GET /api/v1/trading/paper/monitor          | fetchPaperMonitor + useQuery       | ✓ WIRED     | refetchInterval 30000                                                  |
| PaperReportPage            | GET /api/v1/trading/paper/report           | fetchPaperReport + useQuery        | ✓ WIRED     | EquityCurveChart fed data.equityCurve                                  |
| TradingRulesPage promote   | POST /rules/{id}/promote → PaperLifecycleService.promote | promoteRule mutation     | ✓ WIRED     | UI→REST→service status flip works                                      |
| PaperLifecycleService.promote | PaperLiveSymbolService.assignSymbols   | (should populate paper_live_symbols)| ✗ NOT_WIRED | **Service not injected; method never called — activation breaks here** |
| LiveDataScheduler Guard 3  | paper_live_symbols (activeSymbolsUnion)    | symbolService.activeSymbolsUnion() | ✗ NOT_WIRED | Always empty → whole tick skipped → no ingestion/evaluation           |
| PAPER-mode nav             | TradingRulesPage (promote UI)              | TradingLayout paperItems + router  | ✗ NOT_WIRED | promote page is LIVE-gated; PAPER nav points to PaperRulesPage (no promote)|

### Requirements Coverage

| Requirement | Source Plan | Description                              | Status        | Evidence                                                        |
| ----------- | ----------- | ---------------------------------------- | ------------- | -------------------------------------------------------------- |
| DASH-01/02  | 04-01       | 모의 대시보드 잔고·포지션·손익          | ✓ SATISFIED   | PaperDashboardService + Page                                   |
| RULE-01..05 | 04-02       | 상태 전환·배지·승격 게이팅·LIVE 편집 차단 | ⚠️ PARTIAL    | UI/API present; promote does NOT activate live eval; unreachable in PAPER |
| MON-01..04  | 04-03       | 신호 로그·스케줄러·장중상태·체결         | ✓ SATISFIED   | PaperMonitorService + TradingMonitorPage                       |
| REPORT-01/02 | 04-04      | 수익 곡선·지표                           | ✓ SATISFIED   | PaperReportService + PaperReportPage                           |
| DASH-03/04  | 04-04      | (mapped to report by plan)               | ✓ SATISFIED   | report stats                                                   |

### Anti-Patterns Found

| File                                  | Issue                                                                     | Severity   |
| ------------------------------------- | ------------------------------------------------------------------------ | ---------- |
| PaperLifecycleService.java            | Status-only transition; orphaned PaperLiveSymbolService (no assignSymbols)| 🛑 Blocker |
| router/index.tsx + TradingLayout.tsx  | Promote UI gated to LIVE mode, unreachable in default PAPER mode          | 🛑 Blocker |
| LiveEvaluationService.resolveSymbols  | Reads symbols from rule.definition.universe, not paper_live_symbols — source mismatch vs. scheduler ingestion | ⚠️ Warning |

### Build / Typecheck Results

- Frontend `npx tsc --noEmit`: **exit 0 (clean)**.
- Backend: per summaries `gradlew` reports BUILD SUCCESSFUL; all artifacts compile (not re-run here).

### Human Verification Required

Activation flow cannot be verified positively because it is structurally broken (see Gaps). The 4 read-only pages render and poll correctly per code inspection; live-data population depends on the broken activation flow, so dashboard/monitor/report will show empty/zero state until the activation gaps are fixed.

### Gaps Summary

The four pages (dashboard, monitor, report) and the lifecycle list page are **all implemented, typed, routed, and wired to their APIs** — the read side of Phase 4 is solid (criteria 1–5 render correctly). However, the phase GOAL ("사용자가 전략 운영 상황을 ... 제어할 수 있다") is **not achieved** because the single most important control — activating a rule into live paper trading — is broken in two independent ways:

1. **Backend activation never starts (Blocker).** `promote()` only flips `status='PAPER_LIVE'`. It does not populate `paper_live_symbols` via `PaperLiveSymbolService.assignSymbols()`. The scheduler's Guard 3 (`activeSymbolsUnion()`) reads that table and, finding it empty, skips the entire tick — so no bars are ingested and `evaluateTick()` is never run for the promoted rule. Result: zero signals, zero trades, zero equity snapshots → dashboard/monitor/report stay empty forever. (This is the exact unfixed risk flagged in 02-VERIFICATION.) There is also a source mismatch: even if symbols were assigned, `LiveEvaluationService.resolveSymbols()` reads symbols from the rule's JSON definition rather than from `paper_live_symbols`, so the two halves of the loop must be reconciled.

2. **Frontend has no reachable promote control in PAPER mode (Blocker).** The promote button lives only in `TradingRulesPage` at `/trading/rules`, which is wrapped in `ModeGuard mode="LIVE"` and listed only in the LIVE sidebar. In the default PAPER mode the route redirects away, and the PAPER nav's "모의 룰 설정" → `PaperRulesPage` offers only DRAFT/ACTIVE/PAUSED with no promote. So even setting aside the backend gap, a normal PAPER-mode user has no UI path to press "PAPER_LIVE 승격".

Net: read/reporting UI = done; the DRAFT→PAPER_LIVE activation that everything else depends on = not functional end-to-end.

---

_Verified: 2026-06-21_
_Verifier: Claude (gsd-verifier)_
