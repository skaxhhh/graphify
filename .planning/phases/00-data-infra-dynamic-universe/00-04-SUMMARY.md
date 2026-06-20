---
phase: 00-data-infra-dynamic-universe
plan: "04"
subsystem: trading-backtest-engine
tags: [tdd, dynamic-universe, volume_top_n, backtest, jpa, kospi200]
dependency_graph:
  requires: [00-01, 00-02]
  provides: [volume_top_n-universe, BacktestService-dynamic-selection, MarketBarRepository-topVolume]
  affects: [BacktestService, RuleDefinition, RuleDefinitionValidator, MarketDataPort, MarketBarRepository, DbMarketDataAdapter]
tech_stack:
  added: []
  patterns: [TDD RED-GREEN, JPA JPQL JOIN, Port-Adapter pattern, LinkedHashSet dedup]
key_files:
  created:
    - backend/src/test/java/com/graphify/trading/rule/definition/RuleDefinitionUniverseTest.java
    - backend/src/test/java/com/graphify/market/MarketBarRepositoryTopVolumeTest.java
  modified:
    - backend/src/main/java/com/graphify/trading/rule/definition/RuleDefinition.java
    - backend/src/main/java/com/graphify/trading/rule/definition/RuleDefinitionValidator.java
    - backend/src/main/java/com/graphify/trading/engine/MarketDataPort.java
    - backend/src/main/java/com/graphify/market/MarketBarRepository.java
    - backend/src/main/java/com/graphify/market/DbMarketDataAdapter.java
    - backend/src/main/java/com/graphify/trading/backtest/BacktestService.java
decisions:
  - "MarketDataPort에 default 메서드로 topVolumeSymbols()/symbolsByMarket() 추가 — 기존 구현체(테스트 스텁 등) 깨짐 없이 확장"
  - "resolveInitialSymbols(): volume_top_n이면 전체 KOSPI 200 후보군 미리 로드, symbols이면 기존 리스트 반환"
  - "resolveSymbolsForDate(): 날짜 루프 내에서 topVolumeSymbols(date, topN) 호출 — look-ahead bias 없이 해당 날짜 기준 상위 N 선정"
  - "LinkedHashSet으로 dynamic + additionalSymbols 합산 후 closesBySymbol 필터링 — 데이터 없는 종목 자동 제외"
metrics:
  duration: "4 minutes"
  completed_date: "2026-06-20"
  tasks_completed: 3
  files_changed: 8
---

# Phase 0 Plan 04: volume_top_n 동적 유니버스 BacktestService 전체 스택 구현 Summary

**One-liner:** Universe record에 market/topN/additionalSymbols 필드를 추가하고 BacktestService가 날짜마다 KOSPI 200 거래량 상위 N 종목을 동적으로 선정해 룰을 평가하도록 전체 스택을 TDD RED-GREEN으로 구현 — look-ahead bias 없는 동적 유니버스의 기반 완성.

## What Was Built

Phase 0의 핵심 목표인 `volume_top_n` 동적 유니버스 타입을 RuleDefinition부터 BacktestService까지 전 계층에 추가했다.

**계층별 변경:**

1. **RuleDefinition.Universe** — `type`, `symbols` 외에 `market`, `topN`, `additionalSymbols` 3개 필드 추가. `@JsonIgnoreProperties`로 기존 symbols 타입 JSON 하위 호환 유지.

2. **RuleDefinitionValidator** — `UNIVERSE_TYPES`에 `"volume_top_n"` 추가. `volume_top_n` 타입은 `topN > 0` 검증, `symbols` 빈 검사는 건너뜀.

3. **MarketDataPort** — `topVolumeSymbols(LocalDate, int)` + `symbolsByMarket(String)` default 메서드 추가. 기존 구현체 영향 없음.

4. **MarketBarRepository** — `findTopVolumeSymbolsOnDate(date, Pageable)` JPQL 쿼리 (Company JOIN, inKospi200=TRUE, volume IS NOT NULL, ORDER BY volume DESC) + `findDistinctKospi200Symbols(market)` 추가.

5. **DbMarketDataAdapter** — `topVolumeSymbols()` → `PageRequest.of(0, topN)` 위임, `symbolsByMarket()` → `findDistinctKospi200Symbols()` 위임.

6. **BacktestService** — `resolveSymbols()` → `resolveInitialSymbols()` (volume_top_n이면 전체 후보군 로드) + `resolveSymbolsForDate()` 신규 추가 (날짜 루프 내에서 동적 선정).

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| RED  | 테스트 스텁 작성 | 0a9e683 | RuleDefinitionUniverseTest.java, MarketBarRepositoryTopVolumeTest.java |
| GREEN (Task 2) | Universe 확장 + Repository + 포트 + Validator | 4e1d4fe | RuleDefinition.java, RuleDefinitionValidator.java, MarketDataPort.java, MarketBarRepository.java, DbMarketDataAdapter.java |
| GREEN (Task 3) | BacktestService 동적 유니버스 선정 로직 | 1b66945 | BacktestService.java |

## Test Results

- `RuleDefinitionUniverseTest` (4 tests): volume_top_n 전체 필드, 기존 symbols 하위호환, symbols 없어도 역직렬화, additionalSymbols null — 모두 GREEN
- `MarketBarRepositoryTopVolumeTest` (4 tests): 거래량 내림차순 상위 N, in_kospi200=false 제외, volume=null 제외, 다른 날짜 제외 — 모두 GREEN
- 기존 `BacktestServiceVolumeTest` (2 tests), `RuleEvaluatorVolumeTest` (3 tests): 회귀 없음
- `./gradlew test` 전체 BUILD SUCCESSFUL

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] MarketBarRepository already partially updated by formatter**
- **Found during:** Task 2 구현 중 Edit 시도
- **Issue:** MarketBarRepository.java가 이미 `findTopVolumeSymbolsOnDate` 메서드를 포함한 상태로 파일 수정됨 (linter/formatter 실행 추정)
- **Fix:** Read로 현재 상태 재확인 후 `findDistinctKospi200Symbols`만 추가
- **Files modified:** MarketBarRepository.java

### Out-of-scope Items

None.

## Decisions Made

1. **MarketDataPort default 메서드 전략:** `topVolumeSymbols()`, `symbolsByMarket()`을 `default List.of()` 반환으로 추가 — 백테스트 외 컨텍스트(테스트 스텁, 향후 실시간 포트)가 구현 강제 없이 동작
2. **resolveInitialSymbols() 분리:** 전체 후보군(KOSPI 200 전체) 데이터 로드와 날짜별 평가 종목 선정을 분리 — 효율적 사전 캐싱 + look-ahead bias 방지
3. **LinkedHashSet 사용:** dynamic 결과 + additionalSymbols 합산 시 순서 유지 + 중복 제거
4. **빈 날짜 처리:** `resolveSymbolsForDate`가 빈 리스트 반환 시 날짜 루프 내 symbol 루프가 건너뜀 — equity curve는 이전 포지션 값으로 유지 (별도 처리 불필요)

## Self-Check: PASSED

Files exist:
- FOUND: backend/src/main/java/com/graphify/trading/rule/definition/RuleDefinition.java (market, topN, additionalSymbols 필드 포함)
- FOUND: backend/src/main/java/com/graphify/trading/rule/definition/RuleDefinitionValidator.java (volume_top_n 포함)
- FOUND: backend/src/main/java/com/graphify/trading/engine/MarketDataPort.java (topVolumeSymbols, symbolsByMarket 포함)
- FOUND: backend/src/main/java/com/graphify/market/MarketBarRepository.java (findTopVolumeSymbolsOnDate, findDistinctKospi200Symbols 포함)
- FOUND: backend/src/main/java/com/graphify/market/DbMarketDataAdapter.java (topVolumeSymbols, symbolsByMarket 구현 포함)
- FOUND: backend/src/main/java/com/graphify/trading/backtest/BacktestService.java (resolveInitialSymbols, resolveSymbolsForDate 포함)
- FOUND: backend/src/test/java/com/graphify/trading/rule/definition/RuleDefinitionUniverseTest.java
- FOUND: backend/src/test/java/com/graphify/market/MarketBarRepositoryTopVolumeTest.java

Commits:
- FOUND: 0a9e683 (RED tests)
- FOUND: 4e1d4fe (GREEN contracts)
- FOUND: 1b66945 (GREEN BacktestService)

Tests: BUILD SUCCESSFUL — ./gradlew test
