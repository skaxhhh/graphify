# Phase 1: 백테스트 시각화 - Research

**Researched:** 2026-06-20
**Domain:** KRX 인트라데이 데이터 소스 선택 + 백테스트 시각화 (recharts) + 고급 통계 지표 계산
**Confidence:** HIGH (데이터 소스 분석), HIGH (recharts API), HIGH (수식 계산)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **5분봉 단일 표준** — 백테스팅과 PAPER_LIVE 모두 동일 인터벌
- Yahoo Finance 60일 제약은 인지된 상태로 진행 (A-2 결정)
- 백테스팅 가능 기간: 최대 60일 (사용자 인지 완료)
- 백테스팅 시간대: **09:00–12:00 KST** 구간만 평가
- `BacktestRequest`에 `timeFrom`(기본 "09:00"), `timeTo`(기본 "12:00") 파라미터 추가
- 유니버스: 기존 `volume_top_n` 유지 — 거래량은 일봉 기준, 5분봉으로 신호 평가
- 수익 곡선: 날짜 범위 전체를 하나의 연속 곡선 (x축: datetime)
- `EquityPoint.date: LocalDate` → `EquityPoint.datetime: LocalDateTime`으로 변경
- 드로우다운: 같은 차트에 오버레이 — recharts `ReferenceArea` 사용, `rgba(239, 68, 68, 0.15)`
- 드로우다운 구간(`DrawdownSegment`) 서버사이드 계산
- **차트 라이브러리: recharts** (아직 설치 안 됨)
- 고급 통계: 차트 아래 별도 섹션 — Sharpe, Sortino, Profit Factor 3개 카드
- 고급 통계는 **서버사이드 계산** — `BacktestResult`에 필드 추가

### Claude's Discretion
- 차트 높이 (350px 권장)
- x축 날짜+시간 레이블 밀도 (세션 경계마다 날짜 표시 권장)
- 툴팁 스타일링 (dark theme 기존 패턴 따름)
- Sharpe/Sortino 계산 시 무위험수익률 기본값 (0% 또는 연 3.5% — planner 결정)

### Deferred Ideas (OUT OF SCOPE)
- 줌/Brush 범위 선택 — v2
- 차트 hover 시 거래 테이블 행 하이라이트 연동 — v2
- 15분봉/1시간봉 인터벌 선택 — v2
- 세션별(일자별) 수익 분리 뷰 — v2
- 백테스팅 60일 이상 지원 (외부 유료 데이터 소스 연동) — 별도 마일스톤
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| CHART-01 | 백테스트 결과 수익 곡선(equity curve) 라인 차트 | recharts LineChart + 데이터 구조 변경 (LocalDate→LocalDateTime) |
| CHART-02 | 드로우다운 구간을 수익 곡선 위에 음영으로 표시 | recharts ReferenceArea, 서버사이드 DrawdownSegment 계산 |
| CHART-03 | Sharpe Ratio, Sortino Ratio, Profit Factor 서버사이드 계산 | 수식 및 Java 구현 패턴 확인 |
</phase_requirements>

---

## Summary

Phase 1은 세 가지 별개 문제로 구성된다: (1) 5분봉 인트라데이 데이터 수집 방식, (2) 수익 곡선 + 드로우다운 차트 시각화, (3) 서버사이드 고급 통계 계산. 이 세 문제는 독립적이며 병렬로 구현 가능하다.

**데이터 소스 결론:** Yahoo Finance v8 `/v8/finance/chart/{symbol}?interval=5m` API는 이미 `YahooFinanceChartClient.fetchIntraday()`로 연결되어 있다. 60일 히스토리 제약은 사용자가 인지했고, 대안 소스들은 Java Spring Boot에서 직접 사용 불가능하거나(Python 전용 pykrx/FinanceDataReader), 히스토리가 당일만 가능하거나(KIS OpenAPI 분봉 API), 비용이 발생한다. Yahoo Finance는 `.KS` 심볼로 KRX 주식을 지원하며, `includePrePost=false` 파라미터로 정규 장 데이터만 수신 가능하다. 알려진 이슈는 주로 주식 분할 조정 누락(가끔 발생) 및 2025년 2월 이후 간헐적 rate limiting인데, 60일 백테스트 용도에서는 수용 가능하다.

**시각화 결론:** recharts 2.x는 `LineChart` + `ReferenceArea` 조합으로 드로우다운 음영 오버레이를 지원한다. `ReferenceArea`는 `x1`, `x2` 데이터 도메인 값(ISO datetime 문자열)과 `fill`, `stroke` prop을 지원한다. 이미 결정된 패턴이므로 탐색 불필요.

**통계 지표 결론:** Sharpe, Sortino, Profit Factor 모두 표준 수식이 확립되어 있으며 Java로 직접 구현한다. 기존 `BacktestResult.buildResult()` 내부에 추가한다.

**Primary recommendation:** Yahoo Finance 5분봉을 유지하되, `fetchIntraday()` 메서드를 DB write-through 패턴으로 확장하여 `market_bars_intraday` 테이블에 캐싱한다. 백테스트 실행 시 DB에서 읽고, DB 미스 시 Yahoo API 폴백. 이 패턴이 이미 `MarketBarIntraday` 엔티티로 준비되어 있다.

---

## Standard Stack

### Core

| Library/API | Version | Purpose | Why Standard |
|-------------|---------|---------|--------------|
| Yahoo Finance v8 API | (REST, no version) | 5분봉 OHLCV 수집 | 이미 연결됨, `fetchIntraday()` 존재, 60일 히스토리 무료 |
| recharts | 2.15.0 | 수익 곡선 + 드로우다운 차트 | 로드맵 결정, React/TS 친화적 SVG |
| Spring Boot 3.4.5 / Java 21 | (기존) | 백엔드 API + 통계 계산 | 기존 스택 |
| PostgreSQL | (기존) | 분봉 캐시 저장 (`market_bars_intraday`) | `MarketBarIntraday` 엔티티 이미 존재 |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| recharts `ReferenceArea` | 2.15.0 포함 | 드로우다운 음영 오버레이 | 같은 차트 패널 내 오버레이 항상 |
| recharts `Tooltip` | 2.15.0 포함 | hover 툴팁 (datetime + 평가액 + 수익률) | 항상 |
| `@tanstack/react-query` | ^5.62.8 (이미 설치) | 백테스트 실행 + 결과 페칭 (`useMutation`) | 이미 사용 중 |

### Alternatives Considered (Data Sources)

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Yahoo Finance | KIS OpenAPI 분봉 | KIS 분봉은 **당일 데이터만** 제공, 히스토리 백테스팅 불가 |
| Yahoo Finance | pykrx | Python 전용 라이브러리, Java 스택에서 직접 사용 불가. Python 마이크로서비스 브릿지 필요 — 과잉 |
| Yahoo Finance | FinanceDataReader | Python 전용, 5분봉 지원 불명확 |
| Yahoo Finance | KRX DataPortal | 공식 EOD 데이터만, 분봉 API 없음 |
| Yahoo Finance | TickData | 유료 엔터프라이즈 서비스, 이 마일스톤 범위 초과 |
| recharts | Chart.js / Nivo | recharts가 React 친화적이고 로드맵에 이미 결정됨 |

**Installation:**
```bash
# 프론트엔드
cd frontend
npm install recharts@2.15.0
```

---

## Data Source Deep Dive: Yahoo Finance for KRX 5분봉

### 알려진 정확도 이슈 (Confidence: MEDIUM)

Yahoo Finance v8 API는 KRX 주식(`.KS` suffix)의 5분봉을 제공하지만 다음 이슈가 있다:

1. **주식 분할 미조정 (Split adjustment lag):** 가끔 과거 분봉 데이터에 분할 조정이 반영되지 않는 케이스가 보고됨 (yfinance issue #1531). 백테스트 60일 창 내에서 분할이 발생한 종목 결과는 신뢰도 LOW.
   - **대응:** 분할 발생 종목은 해당 분할일 이전 구간을 제외하는 로직 추가 검토 (Phase 2 이후)

2. **2025년 2월 이후 rate limiting 강화:** Yahoo Finance의 2025년 2월 사이트 개편 이후 API 호출 쿼터 제한이 강화됨. 대량 종목 동시 요청 시 HTTP 429 가능.
   - **대응:** 이미 `RestClientException` catch로 빈 리스트 반환하는 fallback 있음. KOSPI 200 중 volume_top_n 10종목만 분봉 요청하므로 현실적 부하 낮음.

3. **장 시간대 봉 누락:** `includePrePost=false` 설정 시 정규장(09:00–15:30 KST)의 봉만 수신. 그러나 변동성 낮은 봉(거래 없는 5분 구간)은 Yahoo가 생략하는 경우 있음.
   - **대응:** 시간 필터(`timeFrom`–`timeTo`) 적용 시 봉 수 체크, 부족 시 해당 날짜 건너뜀.

4. **60일 히스토리 제약 (확정):** `interval=5m&range=60d`가 최대. 이 이상은 Yahoo Finance가 응답하지 않거나 일봉으로 자동 다운그레이드됨.

### API 파라미터 (Confirmed, Confidence: HIGH)

```
GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}
    ?interval=5m
    &range=60d          # 또는 period1/period2로 특정 날짜 범위
    &includePrePost=false
```

Yahoo Finance는 `range` 파라미터 대신 `period1`(epoch seconds)+`period2`(epoch seconds)로 특정 날짜 범위를 요청 가능. 백테스트 날짜 범위가 60일 이하임을 전제로, 시작일~종료일을 epoch로 변환해 요청하는 것이 더 정확함.

```java
// period1/period2 방식 예시
.queryParam("period1", from.atStartOfDay(ZoneId.of("Asia/Seoul")).toEpochSecond())
.queryParam("period2", to.atTime(23,59).atZone(ZoneId.of("Asia/Seoul")).toEpochSecond())
.queryParam("interval", "5m")
.queryParam("includePrePost", "false")
```

### DB Write-Through 캐싱 패턴 (필수)

백테스트 실행 시마다 Yahoo API를 호출하면 rate limit에 걸릴 수 있다. `market_bars_intraday` 테이블을 캐시로 활용한다:

```
백테스트 요청 → 날짜 범위+종목 목록 결정
    → market_bars_intraday에서 해당 (symbol, ts, interval) 조회
    → DB 히트: DB에서 읽음
    → DB 미스: Yahoo API fetchIntraday() → DB 저장 → 읽음
```

`MarketBarIntraday.source = "YAHOO"` 필드가 이미 존재하므로 소스 추적 가능.

---

## Architecture Patterns

### Recommended Project Structure (변경 사항 중심)

```
backend/src/main/java/com/graphify/
├── trading/backtest/
│   ├── BacktestService.java          # 5분봉 모드 추가 (기존 일봉 로직 병행)
│   ├── IntradayBacktestEngine.java   # 신규: 분봉 루프 책임 분리
│   └── dto/
│       ├── BacktestRequest.java      # interval, timeFrom, timeTo 필드 추가
│       └── BacktestResult.java       # sharpeRatio, sortinoRatio, profitFactor,
│                                     # drawdownSegments 필드 추가
│                                     # EquityPoint.date → datetime (LocalDateTime)
├── market/
│   ├── MarketBarIntraday.java        # 이미 존재 — 분봉 캐시 엔티티
│   └── MarketBarIntradayRepository.java  # 이미 존재
└── company/market/
    └── YahooFinanceChartClient.java  # fetchIntraday() 이미 존재 — 확장만

frontend/src/
├── pages/PaperBacktestPage.tsx       # 폼 + 메트릭 카드 + 차트 + 통계 카드
├── components/backtest/
│   ├── EquityCurveChart.tsx          # 신규: LineChart + ReferenceArea 조합
│   └── AdvancedStatsCards.tsx        # 신규: Sharpe/Sortino/PF 3개 카드
└── types/backtest.ts                 # EquityPoint.datetime, DrawdownSegment 타입
```

### Pattern 1: 5분봉 백테스트 루프 구조

**What:** 기존 BacktestService 일봉 루프를 5분봉 루프로 교체. 일봉은 유니버스 선정에만 유지.

**When to use:** 항상 (이 Phase 이후 일봉 백테스트 제거)

```java
// BacktestRequest에 추가될 파라미터
public record BacktestRequest(
    Long ruleId,
    JsonNode definition,
    LocalDate from,
    LocalDate to,
    Double initialCash,
    String timeFrom,   // 기본 "09:00" (KST)
    String timeTo      // 기본 "12:00" (KST)
) {}

// 5분봉 루프 핵심 구조 (IntradayBacktestEngine)
for (LocalDate date : allDates) {
    List<String> symbols = resolveSymbolsForDate(def, date, ...); // 일봉 거래량 기준 (기존 로직)
    for (String symbol : symbols) {
        List<IntradayBar> bars = loadIntradayBars(symbol, date, timeFrom, timeTo);
        // bars: 09:00~12:00 KST의 5분봉 목록
        for (int i = 0; i < bars.size(); i++) {
            IntradayBar bar = bars.get(i);
            // RuleEvaluator 호출은 기존 close[], vols[], i 패턴 유지
            // Bar 타입이 Instant ts 기반이므로 변환 필요
        }
        // 세션 종료 시 equity 스냅샷 추가
        curve.add(new EquityPoint(date.atTime(timeTo), ledger.equity(lastPrices)));
    }
}
```

### Pattern 2: DrawdownSegment 서버사이드 계산

**What:** equity curve를 순회하며 고점 대비 하락 구간을 `(startDatetime, endDatetime)` 쌍으로 추출

```java
// BacktestResult에 추가
public record DrawdownSegment(LocalDateTime start, LocalDateTime end) {}

// buildResult() 내부에서 계산
private List<DrawdownSegment> computeDrawdownSegments(List<EquityPoint> curve) {
    List<DrawdownSegment> segments = new ArrayList<>();
    double peak = curve.get(0).equity();
    LocalDateTime ddStart = null;
    
    for (EquityPoint p : curve) {
        if (p.equity() > peak) {
            if (ddStart != null) {
                segments.add(new DrawdownSegment(ddStart, p.datetime()));
                ddStart = null;
            }
            peak = p.equity();
        } else if (p.equity() < peak && ddStart == null) {
            ddStart = p.datetime();
        }
    }
    if (ddStart != null) {
        segments.add(new DrawdownSegment(ddStart, curve.get(curve.size()-1).datetime()));
    }
    return segments;
}
```

### Pattern 3: recharts 드로우다운 오버레이

**What:** `LineChart` 안에 `DrawdownSegment` 목록을 `ReferenceArea`로 렌더링

```tsx
// EquityCurveChart.tsx
import { LineChart, Line, XAxis, YAxis, Tooltip, ReferenceArea, ResponsiveContainer } from 'recharts';

interface Props {
  data: EquityPoint[];          // { datetime: string; equity: number }[]
  drawdownSegments: DrawdownSegment[];  // { start: string; end: string }[]
}

export function EquityCurveChart({ data, drawdownSegments }: Props) {
  return (
    <ResponsiveContainer width="100%" height={350}>
      <LineChart data={data}>
        <XAxis dataKey="datetime" tickFormatter={formatDateLabel} />
        <YAxis tickFormatter={formatKRW} />
        <Tooltip content={<CustomTooltip />} />
        <Line
          type="monotone"
          dataKey="equity"
          stroke="#10b981"   // emerald-500
          dot={false}
          strokeWidth={2}
        />
        {drawdownSegments.map((seg, i) => (
          <ReferenceArea
            key={i}
            x1={seg.start}
            x2={seg.end}
            fill="rgba(239, 68, 68, 0.15)"
            stroke="none"
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );
}
```

**중요:** `ReferenceArea`의 `x1`/`x2`는 `XAxis`의 `dataKey` 도메인 값과 동일한 타입이어야 함. datetime을 ISO 문자열로 통일한다.

### Pattern 4: Sharpe / Sortino / Profit Factor 계산

**무위험수익률:** 0% 사용 (단순화, 인트라데이 단기 전략에서 0%가 관례적으로 더 많이 사용됨)

```java
// BacktestService.buildResult() 내 추가
private double computeSharpeRatio(List<EquityPoint> curve) {
    // 5분봉 단위 수익률 시계열
    List<Double> returns = new ArrayList<>();
    for (int i = 1; i < curve.size(); i++) {
        double prev = curve.get(i-1).equity();
        double curr = curve.get(i).equity();
        if (prev > 0) returns.add((curr - prev) / prev);
    }
    if (returns.size() < 2) return 0.0;
    
    double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double variance = returns.stream()
        .mapToDouble(r -> (r - mean) * (r - mean))
        .average().orElse(0.0);
    double stdDev = Math.sqrt(variance);
    
    if (stdDev == 0) return 0.0;
    // 연환산: 5분봉 기준 연간 거래 봉수 = (78봉/일 × 250일) = 19500
    // 단, 09:00~12:00 구간만 → 36봉/일 × 250일 = 9000
    double annualizationFactor = Math.sqrt(9000.0);
    return (mean / stdDev) * annualizationFactor;
}

private double computeSortinoRatio(List<EquityPoint> curve) {
    List<Double> returns = new ArrayList<>();
    for (int i = 1; i < curve.size(); i++) {
        double prev = curve.get(i-1).equity();
        double curr = curve.get(i).equity();
        if (prev > 0) returns.add((curr - prev) / prev);
    }
    if (returns.isEmpty()) return 0.0;
    double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    
    // 하방 편차만
    double downsideVariance = returns.stream()
        .filter(r -> r < 0)
        .mapToDouble(r -> r * r)
        .average().orElse(0.0);
    double downsideDev = Math.sqrt(downsideVariance);
    
    if (downsideDev == 0) return 0.0;
    return (mean / downsideDev) * Math.sqrt(9000.0);
}

private double computeProfitFactor(List<PaperLedger.TradeRecord> trades) {
    double grossProfit = trades.stream()
        .filter(t -> "SELL".equals(t.side()) && t.pnl() != null && t.pnl() > 0)
        .mapToDouble(PaperLedger.TradeRecord::pnl)
        .sum();
    double grossLoss = Math.abs(trades.stream()
        .filter(t -> "SELL".equals(t.side()) && t.pnl() != null && t.pnl() < 0)
        .mapToDouble(PaperLedger.TradeRecord::pnl)
        .sum());
    
    if (grossLoss == 0) return grossProfit > 0 ? Double.MAX_VALUE : 0.0;
    return grossProfit / grossLoss;
}
```

### Anti-Patterns to Avoid

- **매 백테스트 실행마다 Yahoo API 직접 호출:** DB 캐시 없이 10종목 × 60일 요청 시 rate limit 위험. 반드시 `market_bars_intraday` 캐시 경유.
- **EquityPoint를 날짜별(1점/일)로만 찍기:** 5분봉 모드에서는 세션 내 봉마다 equity를 찍어야 드로우다운 시각화가 정확함. 최소 세션 시작/종료 두 점 이상.
- **ReferenceArea x1/x2에 LocalDateTime 객체 직접 전달:** JSON 직렬화 후 ISO 문자열로 통일해야 XAxis 도메인과 매칭됨.
- **Sharpe 계산에 일봉 연환산(√252) 사용:** 5분봉 데이터 기반이므로 √9000 (09:00~12:00 세션, 36봉/일 × 250일) 사용.
- **일봉 BacktestService 로직을 완전 제거:** 일봉은 유니버스 선정(volume_top_n)에 여전히 필요. 제거하지 않고 5분봉 엔진을 별도로 추가.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 차트 렌더링 | SVG 직접 드로잉 | recharts | 반응형, 툴팁, 애니메이션 내장 |
| 드로우다운 음영 | Canvas overlay | recharts ReferenceArea | x1/x2 데이터 도메인 좌표로 자동 위치 계산 |
| HTTP 클라이언트 (Yahoo) | 직접 구현 | 기존 RestClient (yahooRestClient 빈) | 이미 구성됨, 타임아웃/에러 처리 포함 |
| 분봉 캐시 저장 | 별도 캐시 서버 | PostgreSQL market_bars_intraday | MarketBarIntraday 엔티티 이미 존재 |

**Key insight:** 이 Phase의 모든 필수 인프라(Yahoo 클라이언트, 분봉 엔티티, React Query, 스타일 시스템)가 이미 존재한다. 새로 만들어야 하는 것은 비즈니스 로직(5분봉 루프, 통계 계산)과 UI 컴포넌트뿐이다.

---

## Common Pitfalls

### Pitfall 1: Yahoo Finance 5분봉 타임스탬프 시간대 혼동
**What goes wrong:** Yahoo 반환 epoch를 UTC로 파싱하면 09:00 KST가 00:00 UTC로 보여 시간 필터(`timeFrom`/`timeTo`) 적용 시 오분류
**Why it happens:** `IntradayBar.ts`가 `Instant` 타입이므로 시간대 없음. 필터링 시 KST 변환 필요
**How to avoid:** 시간 필터 적용 시 항상 `ZoneId.of("Asia/Seoul")` 변환 후 LocalTime 비교
```java
LocalTime barTime = bar.ts().atZone(ZoneId.of("Asia/Seoul")).toLocalTime();
return !barTime.isBefore(timeFrom) && !barTime.isAfter(timeTo);
```
**Warning signs:** 09:00 이전 봉이 결과에 포함되거나 12:00 이후 봉이 누락되는 현상

### Pitfall 2: ReferenceArea x1/x2 타입 미스매치
**What goes wrong:** XAxis `dataKey="datetime"`이 ISO 문자열인데 ReferenceArea에 다른 포맷 전달 시 오버레이 미표시
**Why it happens:** recharts는 x1/x2를 XAxis 도메인 값과 string equality로 매칭
**How to avoid:** 서버에서 `LocalDateTime`을 `"2026-01-02T09:00:00"` 형식 ISO 문자열로 직렬화, 프론트에서도 동일 포맷 유지
**Warning signs:** 드로우다운 음영이 전혀 보이지 않는 현상

### Pitfall 3: volume_top_n 종목에 5분봉 데이터 없음
**What goes wrong:** 일봉 거래량 상위 10종목이 해당 날짜 5분봉 캐시에 없으면 백테스트 해당일 건너뜀
**Why it happens:** 분봉 데이터는 요청 전에 수집되어 있어야 함
**How to avoid:** 백테스트 실행 전 해당 날짜 범위의 volume_top_n 후보군 분봉 사전 수집 트리거. 또는 백테스트 서비스 내에서 DB 미스 시 Yahoo API 즉시 폴백하여 저장
**Warning signs:** 트레이드가 0건 나오거나 특정 날짜들이 equity curve에서 평탄한 현상

### Pitfall 4: Sharpe/Sortino NaN 반환
**What goes wrong:** 트레이드가 전혀 없거나 수익률 변동이 없으면 표준편차 0으로 나눔
**Why it happens:** 짧은 기간 or 신호 없는 전략
**How to avoid:** 표준편차 0 체크 후 0.0 반환. UI에서 0.00 표시 (NaN/null 방어)

### Pitfall 5: recharts 미설치 상태에서 TypeScript 컴파일 실패
**What goes wrong:** `package.json`에 recharts 없음 — import 시 즉시 컴파일 에러
**Why it happens:** 로드맵에서 결정됐지만 아직 설치 안 됨
**How to avoid:** Wave 0에서 `npm install recharts@2.15.0` 선행

---

## Code Examples

### Yahoo Finance 날짜 범위 지정 분봉 요청

```java
// YahooFinanceChartClient에 추가할 오버로드
public List<IntradayBar> fetchIntradayForDateRange(
        String yahooSymbol, LocalDate from, LocalDate to) {
    ZoneId kst = ZoneId.of("Asia/Seoul");
    long period1 = from.atStartOfDay(kst).toEpochSecond();
    long period2 = to.atTime(23, 59, 59).atZone(kst).toEpochSecond();
    try {
        JsonNode root = yahooRestClient.get()
                .uri(u -> u.path("/v8/finance/chart/{symbol}")
                        .queryParam("period1", period1)
                        .queryParam("period2", period2)
                        .queryParam("interval", "5m")
                        .queryParam("includePrePost", "false")
                        .build(yahooSymbol))
                .retrieve()
                .body(JsonNode.class);
        return parseOhlcv(root, false).stream()
                .map(r -> new IntradayBar(
                        Instant.ofEpochSecond(r.epoch()), r.open(), r.high(), r.low(), r.close(), r.volume()))
                .toList();
    } catch (RestClientException ex) {
        log.warn("Yahoo intraday range fetch failed symbol={}: {}", yahooSymbol, ex.getMessage());
        return List.of();
    }
}
```

### DB 캐시 조회 (MarketBarIntradayRepository)

```java
// MarketBarIntradayRepository에 추가할 쿼리
@Query("""
    SELECT m FROM MarketBarIntraday m
    WHERE m.symbol = :symbol
      AND m.interval = '5m'
      AND m.ts >= :from
      AND m.ts <= :to
    ORDER BY m.ts ASC
    """)
List<MarketBarIntraday> findBySymbolAndRange(
    @Param("symbol") String symbol,
    @Param("from") Instant from,
    @Param("to") Instant to
);
```

### BacktestResult DTO 변경

```java
// 변경 후 BacktestResult
public record BacktestResult(
    double initialCash,
    double finalEquity,
    double returnPct,
    double maxDrawdownPct,
    double winRate,
    int tradeCount,
    double sharpeRatio,       // 신규
    double sortinoRatio,      // 신규
    double profitFactor,      // 신규
    List<DrawdownSegment> drawdownSegments,  // 신규
    List<TradeDto> trades,
    List<EquityPoint> equityCurve
) {
    public record EquityPoint(LocalDateTime datetime, double equity) {} // date→datetime
    public record DrawdownSegment(LocalDateTime start, LocalDateTime end) {} // 신규
    public record TradeDto(LocalDateTime datetime, String symbol,
                           String side, double qty, double price, Double pnl) {}
}
```

### recharts x축 세션 경계 레이블

```tsx
// XAxis tickFormatter: 세션 시작(09:00)에만 날짜 표시
const formatDateLabel = (value: string) => {
  const dt = new Date(value);
  const time = dt.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
  if (time === '09:00') {
    return dt.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
  }
  return '';
};
```

### AdvancedStatsCards 컴포넌트 구조

```tsx
// 기존 다크 테마 패턴 (gray-800/900, border-white/10) 적용
function StatCard({ label, value, description }: StatCardProps) {
  return (
    <div className="bg-gray-800 rounded-lg border border-white/10 p-4">
      <p className="text-sm text-gray-400">{label}</p>
      <p className="text-2xl font-bold text-white mt-1">
        {typeof value === 'number' && isFinite(value) ? value.toFixed(2) : '—'}
      </p>
      <p className="text-xs text-gray-500 mt-1">{description}</p>
    </div>
  );
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|-----------------|--------------|--------|
| 일봉 BacktestService (현재 상태) | 5분봉 IntradayBacktestEngine 추가 | Phase 1 | 전략 이식성 보장 (백테스트 = PAPER_LIVE) |
| `EquityPoint.date: LocalDate` | `EquityPoint.datetime: LocalDateTime` | Phase 1 | x축 세밀도 확보 (분봉 시각화) |
| equity curve 없음 | recharts LineChart + ReferenceArea | Phase 1 | 결과 시각화 |
| 기본 3개 통계(수익률/MDD/승률) | +Sharpe/Sortino/PF 추가 | Phase 1 | 전략 품질 평가 고도화 |

**Deprecated/outdated:**
- `BacktestResult.EquityPoint(LocalDate date, ...)`: Phase 1에서 `LocalDateTime datetime`으로 교체
- `BacktestResult.TradeDto(LocalDate date, ...)`: 동일하게 LocalDateTime으로 교체

---

## Open Questions

1. **분봉 캐시 선행 수집 vs 온디맨드 수집**
   - What we know: 백테스트 첫 실행 시 해당 날짜 범위+종목의 5분봉이 DB에 없을 수 있음
   - What's unclear: 백테스트 서비스 내에서 즉시 Yahoo API 폴백+저장을 할지, 아니면 별도 "사전 수집" 엔드포인트를 두어 UI에서 먼저 호출하게 할지
   - Recommendation: 백테스트 서비스 내 온디맨드 폴백 구현. 사용자에게 "데이터 준비 중" 상태를 숨기는 것이 UX 관점에서 낫고, Phase 2 스케줄러 도입 전 가장 단순한 경로.

2. **TradeDto.date도 LocalDateTime으로 변경할지**
   - What we know: 5분봉 체결이므로 시각(시간+분)이 의미 있음
   - What's unclear: 거래 내역 테이블 UI 표시 포맷 (Phase 1 거래 테이블은 기존 유지)
   - Recommendation: LocalDateTime으로 변경. 어차피 JSON 직렬화 시 문자열이므로 기존 테이블 렌더링 코드에 영향 최소. 변경하지 않으면 Phase 3에서 다시 마이그레이션.

3. **Sharpe 연환산 계수**
   - What we know: 09:00–12:00 KST = 3시간 = 36개 5분봉/일, 연 약 250 거래일
   - What's unclear: KRX 실제 거래일 수(250일 vs 248일 vs 252일)
   - Recommendation: 250일 사용 (√(36×250) = √9000 ≈ 94.9). 정확도보다 일관성이 중요하며, Sharpe의 절대값보다 전략 간 상대 비교에 사용.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (Spring Boot 3.4.5 기본 포함) |
| Config file | `backend/src/test/resources/application-test.properties` (기존) |
| Quick run command | `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest -q` |
| Full suite command | `./mvnw test -pl backend -q` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CHART-01 | EquityPoint.datetime 필드 직렬화 | unit | `./mvnw test -pl backend -Dtest=BacktestResultSerializationTest -q` | ❌ Wave 0 |
| CHART-02 | DrawdownSegment 계산 정확성 | unit | `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest#testDrawdownSegments -q` | ❌ Wave 0 |
| CHART-03 | Sharpe/Sortino/PF 계산값 검증 | unit | `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest#testStatsCalculation -q` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./mvnw test -pl backend -Dtest=BacktestServiceIntradayTest -q`
- **Per wave merge:** `./mvnw test -pl backend -q`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/java/com/graphify/trading/backtest/BacktestServiceIntradayTest.java` — covers CHART-02, CHART-03
- [ ] `backend/src/test/java/com/graphify/trading/backtest/BacktestResultSerializationTest.java` — covers CHART-01 (EquityPoint datetime JSON)
- [ ] recharts 설치: `npm install recharts@2.15.0` (frontend/)

---

## Sources

### Primary (HIGH confidence)
- YahooFinanceChartClient.java (프로젝트 내 기존 구현) — fetchIntraday() 메서드 확인
- MarketBarIntraday.java (프로젝트 내) — 엔티티 구조 확인
- BacktestService.java (프로젝트 내) — 기존 일봉 루프 구조 확인
- BacktestResult.java (프로젝트 내) — 현재 DTO 필드 확인
- recharts API docs (recharts.github.io/en-US/api/ReferenceArea) — ReferenceArea props 확인

### Secondary (MEDIUM confidence)
- coffee4m.com KIS API 분봉 분석 — KIS 분봉 당일 한정, 30건/쿼리 한계 확인
- publicapis.io Yahoo Finance API Guide 2026 — 5m 60일 제약 확인
- yfinance GitHub issues (#1531, #2607, #2621) — split adjustment lag 이슈 패턴 확인
- wallstreetprep.com Sortino Ratio — 공식 수식 확인
- quantifiedstrategies.com Profit Factor — 공식 수식 확인

### Tertiary (LOW confidence)
- WebSearch: pykrx intraday 5분봉 지원 여부 — Python 전용, Java 브릿지 불필요 결론 도출

---

## Metadata

**Confidence breakdown:**
- Data source recommendation: HIGH — KIS/pykrx 대안 실용 불가 확인됨, Yahoo Finance 기존 연결 확인됨
- recharts API: HIGH — 공식 문서 직접 확인
- Sharpe/Sortino/PF 수식: HIGH — 다중 출처 일치
- Yahoo Finance KRX accuracy: MEDIUM — 알려진 이슈 확인됐으나 KRX 전용 이슈는 일반 Yahoo 이슈와 동일 패턴

**Research date:** 2026-06-20
**Valid until:** 2026-07-20 (Yahoo Finance API 구조는 안정적이나 rate limit 정책은 변동 가능)
