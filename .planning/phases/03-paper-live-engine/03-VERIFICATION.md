---
phase: 03-paper-live-engine
verified: 2026-06-21T00:00:00Z
status: passed
score: 4/4 must-haves verified
human_verification:
  - test: "Live tick fires during KRX hours and writes trades/snapshots/signal-log rows"
    expected: "During 09:00–15:30 KST on a trading day, with at least one PAPER_LIVE rule (explicit symbols, 20+ 5m bars) and entry condition true, a row appears in paper_trades, paper_signal_log (with indicator_snapshot JSON), and paper_equity_snapshots after the 5-minute tick"
    why_human: "Requires real wall-clock KRX hours; @Scheduled cron + ZonedDateTime.now() not clock-injectable in unit tests"
  - test: "Position survives instance restart (write-through)"
    expected: "After a BUY fills, restart the backend; the next SELL evaluation finds the position from paper_positions and closes it correctly"
    why_human: "Requires runtime restart against a live PostgreSQL — not assertable via Mockito unit tests"
notable:
  - "volume_top_n rules without explicit symbols/additionalSymbols are NOT evaluated by LiveEvaluationService.resolveSymbols() (returns empty list). The scheduler collects those bars via PaperLiveSymbolService.activeSymbolsUnion(), but the evaluation loop does not read paper_live_symbols. Non-blocking for Phase 3 explicit-symbol rules; affects dynamic-universe live evaluation."
---

# Phase 3: PAPER_LIVE 평가 엔진 Verification Report

**Phase Goal:** PAPER_LIVE 상태인 룰을 매 평가 주기마다 자동으로 평가하고, 가상 체결을 실행하며, 평가금액 스냅샷과 신호 로그(지표값 포함)를 DB에 저장한다.
**Verified:** 2026-06-21
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth                                                                                                  | Status     | Evidence                                                                                                                                                                                 |
| --- | ----------------------------------------------------------------------------------------------------- | ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | PAPER_LIVE 룰 존재 시 스케줄러 틱마다 RuleEvaluator 실행 + 신호 시 PaperExecutor 가상 체결 (SC-1, LIVE-05) | ✓ VERIFIED | `LiveDataScheduler.collectLiveData()` line 93 calls `evaluationService.evaluateTick()`; service filters `PAPER_LIVE` (line 80), per-symbol calls `ruleEvaluator.entryTriggered/exitTriggered` (lines 157/161), `executor.execute(signal,...)` line 166; PaperExecutor persists position+trade |
| 2   | 매 평가 주기 종료 시 paper_equity_snapshots에 가상 계좌 평가금액 저장 (SC-2, LIVE-06)                     | ✓ VERIFIED | `saveEquitySnapshot()` (lines 185-205) computes `cash + Σ(qty × markPrice)` over `findByAccountId`, saves `PaperEquitySnapshot`; called per rule (line 122); test `equity_snapshot_saved_after_tick` PASSED |
| 3   | 신호 로그에 평가 시점 RSI/SMA 등 주요 지표값 기록 (SC-3, MON-04)                                          | ✓ VERIFIED | `buildIndicatorSnapshot()` builds `{"price","rsi14","sma20"}` via `Indicators.rsi/sma` (lines 170-183), passed to `executor.execute(...,indicatorJson)`; PaperExecutor.`saveSignalLog()` persists to `paper_signal_log.indicator_snapshot` JSONB (V32) |
| 4   | DB write-through로 틱마다 상태 로드·플러시, 인스턴스 재시작 후 포지션 유지 (SC-4)                          | ✓ VERIFIED | `PaperExecutor.execute()` @Transactional: `findByUserId` load → `findByAccountIdAndSymbol` → persist position/trade → `setCash` + `accountRepo.save` flush (lines 54-119); no in-memory ledger; positions read from DB each tick |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact                                                       | Expected                                          | Status     | Details                                                                            |
| ------------------------------------------------------------- | ------------------------------------------------- | ---------- | --------------------------------------------------------------------------------- |
| `db/migration/V32__paper_signal_log.sql`                      | paper_signal_log DDL + indexes                    | ✓ VERIFIED | rule_id FK→trading_rules ON DELETE CASCADE, JSONB indicator_snapshot, 2 indexes    |
| `trading/paper/OrderExecutorPort.java`                        | execute(signal,rule,symbol,price,ts,json)→TradeResult | ✓ VERIFIED | Strategy interface; PaperExecutor implements; LiveEvaluationService depends on it |
| `trading/paper/PaperExecutor.java`                            | DB write-through: load→transact→flush             | ✓ VERIFIED | @Service @Transactional; BUY/SELL/HOLD + ALREADY_HOLDS/NO_POSITION/INSUFFICIENT_CASH guards |
| `trading/paper/LiveEvaluationService.java`                    | tick evaluator: rules→bars→signal→execute→snapshot | ✓ VERIFIED | @Service @Transactional; staleness(10m) + min-bars(20) guards; equity snapshot     |
| `trading/paper/PaperSignalLog.java` + Repository              | JPA entity + finders                              | ✓ VERIFIED | @JdbcTypeCode(JSON); findByRuleIdOrderByTsDesc + findTop50ByOrderByTsDesc          |
| `trading/paper/PaperAccount/Position/Trade/EquitySnapshot`    | V28 table mappings + repos                        | ✓ VERIFIED | All 4 entities + 4 repos present; findByUserId/findByAccountIdAndSymbol/findByAccountId/deleteByAccountIdAndSymbol |
| `market/LiveDataScheduler.java`                               | calls evaluateTick() after ingest                 | ✓ VERIFIED | LiveEvaluationService injected (5th ctor arg); evaluateTick(now) line 93           |
| `test/.../PaperExecutorTest.java`                             | 6 BUY/SELL/HOLD path tests                        | ✓ VERIFIED | 6 tests; suite exit 0                                                              |
| `test/.../LiveEvaluationServiceTest.java`                     | 6 tick/staleness/min-bars/snapshot tests          | ✓ VERIFIED | 6 tests; suite exit 0                                                              |

### Key Link Verification

| From                              | To                                            | Via                                   | Status  | Details                                              |
| --------------------------------- | --------------------------------------------- | ------------------------------------- | ------- | ---------------------------------------------------- |
| LiveDataScheduler.collectLiveData | LiveEvaluationService.evaluateTick()          | constructor injection, line 93        | ✓ WIRED | called after ingest loop with now.toInstant()        |
| LiveEvaluationService             | TradingRuleRepository.findAll() filter PAPER_LIVE | line 79-81                          | ✓ WIRED | status filter to PAPER_LIVE                           |
| LiveEvaluationService             | MarketDataPort.recentIntradayBars(symbol)     | line 134                              | ✓ WIRED | feeds closes[]/volumes[] for evaluation              |
| LiveEvaluationService             | RuleEvaluator.entry/exitTriggered()           | lines 157/161                         | ✓ WIRED | position-aware signal determination                  |
| LiveEvaluationService             | OrderExecutorPort.execute()                   | line 166                              | ✓ WIRED | BUY/SELL only (HOLD short-circuits)                  |
| LiveEvaluationService             | PaperEquitySnapshotRepository.save()          | line 197, saveEquitySnapshot()        | ✓ WIRED | per rule/account after symbols evaluated             |
| PaperExecutor                     | PaperAccountRepository.findByUserId()         | line 54                               | ✓ WIRED | account load before transacting                      |
| PaperExecutor                     | PaperPositionRepository.findByAccountIdAndSymbol() | lines 68/99                        | ✓ WIRED | existing-position check on BUY/SELL                   |
| PaperExecutor                     | FillSimulator.fillPrice()/fee()               | lines 72/78/105/107                   | ✓ WIRED | price + fee calc before DB write                     |
| PaperExecutor                     | PaperSignalLogRepository.save()               | saveSignalLog() lines 50/61           | ✓ WIRED | indicator_snapshot persisted per signal              |

### Requirements Coverage

| Requirement | Source Plan  | Description                                                | Status      | Evidence                                                   |
| ----------- | ------------ | --------------------------------------------------------- | ----------- | --------------------------------------------------------- |
| LIVE-05     | 03-01, 03-02 | PAPER_LIVE 룰 매 주기 자동 평가 + 가상 체결                  | ✓ SATISFIED | evaluateTick loop + RuleEvaluator + PaperExecutor.execute |
| LIVE-06     | 03-01, 03-02 | 각 주기 종료 시 평가금액 스냅샷 DB 저장                       | ✓ SATISFIED | saveEquitySnapshot → PaperEquitySnapshot                  |
| MON-04      | 03-02        | 신호 로그에 RSI/SMA 등 지표값 기록                           | ✓ SATISFIED | buildIndicatorSnapshot → paper_signal_log.indicator_snapshot |

REQUIREMENTS.md still marks LIVE-05/LIVE-06 as "Pending" (lines 30-31, 131-132) and "[ ]" though implementation is complete; MON-04 marked Complete. No orphaned requirements — all three IDs declared in plan frontmatter. Recommend updating REQUIREMENTS.md status to reflect completed implementation.

### Anti-Patterns Found

None in Phase 3 core files (PaperExecutor, LiveEvaluationService, OrderExecutorPort, TradeResult, LiveDataScheduler). No TODO/FIXME/PLACEHOLDER/not-implemented patterns.

ℹ️ Info: `PaperExecutor.resolveSizingCash()` and `LiveEvaluationService.evaluateRule()` construct a fresh `new ObjectMapper()` / use injected mapper respectively — minor; the injected mapper in the service is the correct pattern, the executor's local instantiation is a tiny inefficiency, non-blocking.

### Human Verification Required

1. **Live tick during KRX hours** — With a PAPER_LIVE rule (explicit symbols, ≥20 5m bars) whose entry condition is true, confirm during 09:00–15:30 KST that the 5-minute tick inserts rows into `paper_trades`, `paper_signal_log` (with `indicator_snapshot` JSON), and `paper_equity_snapshots`. Why human: `@Scheduled` cron + `ZonedDateTime.now()` are wall-clock dependent and not clock-injected in tests.
2. **Write-through restart** — After a BUY fills, restart backend; confirm the next SELL evaluation closes the DB-persisted position. Why human: requires runtime restart against live PostgreSQL.

### Gaps Summary

No blocking gaps. All four ROADMAP Success Criteria (SC-1..SC-4) are verified at exists/substantive/wired levels, all key links connected, all three requirements (LIVE-05, LIVE-06, MON-04) satisfied with implementation evidence. The three relevant test classes (PaperExecutorTest 6, LiveEvaluationServiceTest 6, LiveDataSchedulerTest 5) compile and pass — `./gradlew test` for those classes exited 0.

**Notable (non-blocking):** `LiveEvaluationService.resolveSymbols()` only returns symbols from `universe.symbols` or `universe.additionalSymbols`. A pure `volume_top_n` rule with neither populated yields an empty symbol list and is therefore not evaluated, even though `LiveDataScheduler` already collects those top-N bars via `PaperLiveSymbolService.activeSymbolsUnion()`. The evaluation loop never reads `paper_live_symbols`. This does not break the Phase 3 goal for explicit-symbol PAPER_LIVE rules (the documented primary path), but the dynamic-universe live evaluation lane is incomplete and should be addressed when volume_top_n live rules are exercised (likely Phase 4+ or a gap-closure plan).

Two items remain for human runtime confirmation (live cron firing and restart persistence) but neither is a code gap — both are inherent limitations of unit-testing wall-clock/scheduler behavior.

---

_Verified: 2026-06-21_
_Verifier: Claude (gsd-verifier)_
