---
phase: 01-backtest-visualization
verified: 2026-06-21T00:00:00Z
status: passed
score: 5/5 success criteria verified
re_verification:
  previous_status: gaps_found
  previous_score: 2/5
  gaps_closed:
    - "수익 곡선이 datetime x축으로 렌더링되며 드로우다운 구간에 연한 붉은 음영이 오버레이된다"
    - "Sharpe Ratio, Sortino Ratio, Profit Factor가 서버에서 계산되어 차트 아래 별도 섹션에 표시된다"
    - "hover 툴팁에 datetime + 평가액 + 누적 수익률이 표시된다"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "백테스트 실행 후 수익 곡선 X축 레이블 확인"
    expected: "09:00 세션 오픈 시각에만 날짜 레이블이 표시되고 날짜가 바뀔 때 자연스럽게 연결된다"
    why_human: "XAxis tickFormatter 로직과 실제 렌더링은 시각적으로만 검증 가능"
  - test: "드로우다운 발생 구간에 붉은 음영 오버레이 확인"
    expected: "MDD 구간에 반투명 붉은 ReferenceArea가 수익 곡선 위에 오버레이된다"
    why_human: "CSS/opacity 시각적 확인 필요"
---

# Phase 1: 5분봉 인트라데이 백테스팅 & 시각화 Verification Report

**Phase Goal:** 일봉 백테스팅을 5분봉 인트라데이 모드로 전환한다. 사용자가 날짜 범위를 선택하면 해당 기간 거래량 상위 종목(volume_top_n)을 자동 선정하고 각 거래일 09:00–12:00 KST 구간 5분봉으로 백테스팅을 실행한다. 결과는 datetime 축 수익 곡선 + 드로우다운 음영 + Sharpe/Sortino/Profit Factor로 시각화한다.
**Verified:** 2026-06-21
**Status:** passed
**Re-verification:** Yes — after gap closure (plans 01-03 and 01-04)

## Goal Achievement

### Observable Truths (Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | BacktestRequest에 timeFrom(기본 09:00), timeTo(기본 12:00) 파라미터가 추가되고, BacktestService가 해당 구간 5분봉으로 평가를 실행한다 | VERIFIED | `BacktestRequest.java` lines 16-18에 `timeFrom`/`timeTo` 필드 존재. `BacktestService` line 94에서 `intradayEngine.run()` 위임 확인 (regression OK). |
| 2 | 유니버스 선정은 일봉 거래량(volume_top_n) 기준을 유지하고, 선정된 종목의 5분봉 데이터를 Yahoo Finance에서 수집한다 | VERIFIED | `IntradayBacktestEngine.loadBars()`: DB 우선 조회 → 미스 시 `yahooClient.fetchIntradayForDateRange()` fallback → DB 저장. `BacktestService`는 `resolveInitialSymbols`/`resolveSymbolsForDate`로 volume_top_n 유지. |
| 3 | 수익 곡선이 datetime x축(각 세션 09:00–12:00 연속 연결)으로 렌더링되며, 드로우다운 구간에 연한 붉은 음영이 오버레이된다 | VERIFIED | `EquityCurveChart.tsx`: recharts `LineChart` + `XAxis dataKey="datetime"` + `ReferenceArea fill="rgba(239,68,68,0.15)"` per `DrawdownSegment`. `PaperBacktestPage.tsx` lines 159-163에서 `result.equityCurve` + `result.drawdownSegments` 전달. |
| 4 | Sharpe Ratio, Sortino Ratio, Profit Factor가 서버에서 계산되어 차트 아래 별도 섹션에 표시된다 | VERIFIED | `BacktestResult.java` lines 13-15에 3개 필드 존재. `trading.ts` `BacktestResult` 인터페이스에 동일 필드 추가(lines 108-110). `PaperBacktestPage.tsx` lines 167-185에 "고급 통계" 섹션으로 3개 `StatCard` 렌더링. |
| 5 | hover 툴팁에 datetime + 평가액 + 누적 수익률이 표시된다 | VERIFIED | `EquityCurveChart.tsx` lines 53-81: `CustomTooltip` — formattedDt(datetime 포맷), equity(원), cumulative returnPct(%) 세 줄 렌더링. `LineChart`에 `<Tooltip content={<CustomTooltip initialCash={initialCash} />} />` 연결. |

**Score:** 5/5 success criteria verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `backend/.../YahooFinanceChartClient.java` | `fetchIntradayForDateRange` 메서드 | VERIFIED | regression — unchanged |
| `backend/.../MarketBarIntradayRepository.java` | `findBySymbolAndRange` JPQL 쿼리 | VERIFIED | regression — unchanged |
| `backend/.../backtest/dto/BacktestRequest.java` | `timeFrom`, `timeTo` String 필드 | VERIFIED | regression — unchanged |
| `backend/.../trading/backtest/IntradayBacktestEngine.java` | 5분봉 백테스트 루프, DB 캐시 + Yahoo fallback | VERIFIED | regression — unchanged |
| `backend/.../backtest/dto/BacktestResult.java` | `EquityPoint(LocalDateTime datetime)`, sharpeRatio/sortinoRatio/profitFactor/drawdownSegments | VERIFIED | lines 13-16, 22, 30 confirmed |
| `frontend/src/types/trading.ts` | `BacktestEquityPoint.datetime`, `DrawdownSegment`, `BacktestResult` 통계 3개 필드 + drawdownSegments | VERIFIED | `datetime` line 92, `DrawdownSegment` lines 96-99, stats lines 108-111 |
| `frontend/src/components/backtest/EquityCurveChart.tsx` | recharts LineChart + ReferenceArea drawdown + CustomTooltip | VERIFIED | 137줄. `XAxis dataKey="datetime"`, `ReferenceArea fill="rgba(239,68,68,0.15)"`, CustomTooltip 3-line render — commit 0aa61a7 |
| `frontend/src/pages/trading/paper/PaperBacktestPage.tsx` | EquityCurveChart wiring + 고급 통계 섹션 + timeFrom/timeTo 폼 | VERIFIED | 293줄. import line 6, render lines 159-163, 고급 통계 lines 167-185, 시각 입력 lines 100-116 — commit ec70a74 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `IntradayBacktestEngine` | `MarketBarIntradayRepository.findBySymbolAndRange()` | primary DB read | WIRED | regression — unchanged |
| `IntradayBacktestEngine` | `YahooFinanceChartClient.fetchIntradayForDateRange()` | DB miss fallback | WIRED | regression — unchanged |
| `BacktestService.run()` | `IntradayBacktestEngine.run()` | delegates intraday evaluation | WIRED | `BacktestService` line 94 confirmed |
| `BacktestResult.EquityPoint.datetime` | `EquityCurveChart XAxis dataKey` | ISO LocalDateTime JSON field name match | WIRED | `BacktestEquityPoint.datetime` (types) + `XAxis dataKey="datetime"` (chart) |
| `BacktestResult.drawdownSegments` | `ReferenceArea x1/x2` | `DrawdownSegment.start/end` ISO strings | WIRED | `EquityCurveChart.tsx` lines 117-125 |
| `PaperBacktestPage` | `EquityCurveChart` | import + JSX render with all props | WIRED | line 6 import, lines 159-163 render |
| `PaperBacktestPage timeFrom/timeTo` | `runBacktest()` payload | useState → mutationFn | WIRED | state lines 19-20, payload lines 39-40 |

### Requirements Coverage

| Requirement | Description | Plans | Status | Evidence |
|-------------|-------------|-------|--------|----------|
| CHART-01 | 사용자는 백테스트 결과 페이지에서 수익 곡선(equity curve) 라인 차트를 볼 수 있다 | 01-01, 01-02, 01-03, 01-04 | SATISFIED | `EquityCurveChart.tsx` recharts LineChart, `PaperBacktestPage.tsx` lines 159-163에서 `result.equityCurve` 렌더링 |
| CHART-02 | 사용자는 백테스트 결과에서 드로우다운 구간을 수익 곡선 위에 음영으로 확인할 수 있다 | 01-01, 01-02, 01-03, 01-04 | SATISFIED | `computeDrawdownSegments()` 서버 계산 + `BacktestResult.drawdownSegments` → `EquityCurveChart` `ReferenceArea` 오버레이 |
| CHART-03 | 사용자는 백테스트 결과에서 Sharpe Ratio, Sortino Ratio, Profit Factor를 확인할 수 있다 (서버사이드 계산) | 01-01, 01-02, 01-03, 01-04 | SATISFIED | `IntradayBacktestEngine` 계산, `BacktestResult` DTO 포함, `PaperBacktestPage` "고급 통계" 섹션 3개 StatCard |

### Anti-Patterns Found

None. No blockers or warnings in new files.

The only `return null` in `EquityCurveChart.tsx` (line 54) is the recharts CustomTooltip early-return guard (`!active || !payload`) — correct recharts pattern, not a stub.

### Human Verification Required

#### 1. 차트 렌더링 후 datetime 축 포맷

**Test:** 백테스트 실행 후 차트의 X축 레이블 확인
**Expected:** 09:00 세션 오픈 시각에만 "M월 D일" 날짜 레이블이 표시되고, 날짜가 바뀔 때 자연스럽게 연결된다
**Why human:** XAxis tickFormatter 로직과 실제 렌더링은 시각적으로만 검증 가능

#### 2. 드로우다운 음영 색상

**Test:** MDD가 발생한 기간에 차트에 붉은 음영이 표시되는지 확인
**Expected:** 드로우다운 구간에 `rgba(239,68,68,0.15)` 반투명 붉은 ReferenceArea 오버레이
**Why human:** CSS/opacity 시각적 확인 필요

### Re-verification Summary

**Previous status:** gaps_found (2/5) — plans 01-03 and 01-04 were missing.

**Gaps closed:**

1. `frontend/src/types/trading.ts` — `BacktestEquityPoint.date` → `datetime` 필드명 수정, `DrawdownSegment` 인터페이스 추가, `BacktestResult`에 `sharpeRatio`/`sortinoRatio`/`profitFactor`/`drawdownSegments` 4개 필드 추가 (commit e63e54f)
2. `frontend/src/components/backtest/EquityCurveChart.tsx` — 신규 생성. recharts `LineChart` + `ReferenceArea` 드로우다운 오버레이 + `CustomTooltip` (datetime, 원, 수익률 3줄) (commit 0aa61a7)
3. `frontend/src/pages/trading/paper/PaperBacktestPage.tsx` — `EquityCurveChart` import 및 render, "고급 통계" Sharpe/Sortino/PF StatCard 섹션 추가, `timeFrom`/`timeTo` 폼 입력 및 payload 연결, `t.date` → `t.datetime` TypeScript 오류 해소 (commit ec70a74)

**Regressions:** None. Backend layer (IntradayBacktestEngine, BacktestService, BacktestResult DTO, MarketBarIntradayRepository, YahooFinanceChartClient) confirmed unchanged.

**Current status:** passed (5/5)

---

_Verified: 2026-06-21_
_Verifier: Claude (gsd-verifier)_
