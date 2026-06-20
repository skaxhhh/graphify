---
phase: 1
slug: backtest-visualization
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-20
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito (Spring Boot 3.4.5 기본 포함) |
| **Config file** | `backend/src/test/resources/application-test.properties` |
| **Quick run command** | `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest -q` |
| **Full suite command** | `./mvnw test -pl backend -q` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest -q`
- **After every plan wave:** Run `./mvnw test -pl backend -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 1 | CHART-01 | unit | `./mvnw test -pl backend -Dtest=BacktestResultSerializationTest -q` | ❌ W0 | ⬜ pending |
| 1-02-01 | 02 | 1 | CHART-02 | unit | `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest#testDrawdownSegments -q` | ❌ W0 | ⬜ pending |
| 1-02-02 | 02 | 1 | CHART-03 | unit | `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest#testStatsCalculation -q` | ❌ W0 | ⬜ pending |
| 1-03-01 | 03 | 2 | CHART-01, CHART-02 | manual | 브라우저에서 수익곡선 + 드로우다운 음영 확인 | N/A | ⬜ pending |
| 1-04-01 | 04 | 2 | CHART-03 | manual | 브라우저에서 Sharpe/Sortino/PF 카드 렌더링 확인 | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/java/com/graphify/trading/backtest/BacktestServiceIntradayTest.java` — stubs for CHART-02, CHART-03
- [ ] `backend/src/test/java/com/graphify/trading/backtest/BacktestResultSerializationTest.java` — covers CHART-01 (EquityPoint datetime JSON)
- [ ] recharts 설치: `cd frontend && npm install recharts@2.15.0`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| 드로우다운 음영 오버레이 시각 확인 | CHART-02 | SVG 렌더링 결과는 브라우저에서만 확인 가능 | 백테스트 실행 후 차트에 붉은 음영(rgba(239,68,68,0.15)) 표시 여부 확인 |
| hover 툴팁 datetime + 평가액 + 수익률 | CHART-01 | 인터랙션 동작은 자동화 불가 | 차트 포인트 hover 시 3-line 툴팁 표시 확인 |
| Sharpe/Sortino/PF 카드 레이아웃 | CHART-03 | 스타일/레이아웃 검증은 브라우저에서 | 차트 아래 3개 카드(dark theme) 정상 렌더링 확인 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
