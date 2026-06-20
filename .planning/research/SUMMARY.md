# Research Summary: Graphify 모의투자 고도화 & 토스증권 연동

*Synthesized: 2026-06-20 from STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md*

---

## Recommended Stack

| 영역 | 선택 | 버전 | 이유 |
|------|------|------|------|
| 주가 차트 (캔들) | lightweight-charts | 5.2.0 | Canvas 기반, 390개 1분봉도 빠름. TradingView 공식 |
| 지표 차트 (수익 곡선) | recharts | 2.15.0 | React 선언형, Tailwind 친화, SVG로 100-500 포인트 충분 |
| 실시간 시세 | Yahoo Finance polling (5m) | 기존 사용 중 | `MarketDataIngestionService.ingestIntraday()` 이미 존재. PAPER_LIVE에 충분 |
| LIVE 시세 | KIS WebSocket | - | LIVE 주문 단계에서 추가. 토스증권 시세 REST도 대안 |
| 스케줄러 | Spring @Scheduled | 기존 | `zone="Asia/Seoul"` + ShedLock. Quartz/Spring Batch 불필요 |
| 분산 잠금 | ShedLock | 5.x | PostgreSQL 기반, 새 인프라 없음. LIVE 이전에 필수 |
| OAuth 토큰 암호화 | JPA AttributeConverter + AES-256-GCM | JDK 내장 | 외부 의존성 없음. 환경변수에서 키 로드 |

**설치 명령:**
```bash
npm install lightweight-charts@5.2.0 recharts@2.15.0
# backend: shedlock-spring + shedlock-provider-jdbc-template (build.gradle.kts에 추가)
```

---

## Table Stakes Features (없으면 서비스 불가)

**백테스트 페이지 (엔진 존재, UI만 부족):**
- 수익 곡선 차트 (equityCurve 데이터 이미 API 응답에 있음, 차트만 없음)
- Sharpe Ratio, Sortino Ratio, Profit Factor (서버사이드 계산 추가)
- 드로우다운 차트 (수익 곡선에 오버레이)

**모의 대시보드 (현재 완전 비어있음):**
- 가상 잔고 + 평가금액 표시
- 활성 포지션 목록 (종목, 수량, 평균가, 평가손익)
- 오늘 실현손익 요약
- 활성 룰 목록 (PAPER_LIVE 상태인 것)

**룰 생애주기 (현재 DRAFT/ACTIVE/PAUSED만 있음):**
- `DRAFT → BACKTESTED → PAPER_LIVE → LIVE` 명시적 상태 전환
- PAPER_LIVE 승격 버튼 (백테스트 최소 1회 완료 검증)
- LIVE 승격 게이트 (토스증권 인증, 최소 5거래일 운영 확인)

**실시간 모니터 (현재 완전 비어있음):**
- 신호 평가 로그 (어떤 룰이 언제 신호를 냈는지)
- 스케줄러 하트비트 ("마지막 평가: 10:33:05 KST")
- 장중/장외 상태 표시

**성과 리포트 (현재 완전 비어있음):**
- 기간별 수익 곡선 (paper_equity_snapshots 기반)
- 거래 내역 테이블 (paginated)
- 기간별 수익 요약 (월별)

---

## Architecture Highlights

### 핵심 설계 원칙: RuleEvaluator는 변경 없음

```
BacktestService    LiveEvaluationService (NEW)
     └─ RuleEvaluator (UNCHANGED, stateless) ─┘
              │
    MarketDataPort (interface)
    ├── DbMarketDataAdapter (기존: historicalDailyBars)
    └── LiveIntradayAdapter (추가: recentIntradayBars) 
              │
    OrderExecutorPort (NEW interface)
    ├── PaperExecutor → paper_trades (DB)
    └── TossOrderExecutor → 토스증권 REST API
```

### 스케줄러 패턴
```java
@Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
@SchedulerLock(name = "liveEvaluation", lockAtMostFor = "PT55S")
public void tick() {
    if (!krxCalendar.isTradingDay(LocalDate.now())) return;
    if (LocalTime.now(KST).isAfter(15, 30)) return;
    ingest → evaluate → flush;
}
```

### Paper Account: DB 쓰기 관통(Write-Through) 방식
- 틱마다 DB에서 상태 로드 → PaperLedger 인스턴스 생성 → 평가 → DB 플러시
- `paper_accounts`, `paper_positions`, `paper_trades`, `paper_equity_snapshots` (V28 스키마 이미 존재)

### 룰 승격 아키텍처
- PAPER_LIVE → LIVE 승격 시 룰 복사 생성 (`promotedFrom` 필드 이미 있음)
- 같은 스케줄러 루프, `rule.mode`로 executor 라우팅
- RuleEvaluator는 PAPER와 LIVE 모두 동일 코드 사용

---

## Top Pitfalls to Avoid

### 즉시 대응 필요 (구현 시작 전)

| 함정 | 해결책 |
|------|--------|
| `@Scheduled` timezone 기본값 = JVM/UTC | `zone = "Asia/Seoul"` 명시 필수. 컨테이너에 `TZ=Asia/Seoul` 환경변수도 설정 |
| 다중 인스턴스 이중 발주 | ShedLock 적용 후에만 LIVE 룰 활성화 |
| KRX 공휴일 미감지 → 장 외 평가 | `market_holidays` 테이블 + `KrxMarketCalendar.isTradingDay()` 가드 |
| Yahoo Finance 데이터 공백 | 최신 bar 기준 staleness 체크 (`now - last_bar_ts > 2 * interval`) |
| 토큰 평문 저장 | `AesGcmAttributeConverter` 적용. DB에 평문 토큰 절대 불가 |
| 토큰 세션 중 만료 (TTL ~1h) | `TossTokenManager` — 50분째 선제 갱신 |

### 나중에 대응 가능

| 함정 | 단계 |
|------|------|
| Look-ahead bias (종가 vs 장중봉) | 룰 `evalTiming: EOD/INTRADAY` 선택 필드 추가 |
| 슬리피지 미반영 → 낙관적 백테스트 | `FillSimulator` 수수료+슬리피지 모델 추가 |
| 상한가/하한가 주문 무한 루프 | `PRICE_LIMIT_PENDING` 상태 + 주문 중복 방지 |
| 생존편향 (상장폐지 종목 제외) | UI에 경고 표시로 우선 대응 |
| 크로스유저 주문 위험 | 주문 실행 전 `rule.userId == credentials.userId` 검증 |

---

## Phase Implications

연구 결과에서 도출한 빌드 순서 (의존성 기반):

```
Phase 1: 백테스트 시각화 완성
  → 이미 동작하는 엔진, 차트만 추가. 가장 높은 ROI.
  → recharts + 서버사이드 Sharpe/Sortino/Profit Factor

Phase 2: 실시간 데이터 수집 + 스케줄러 셸
  → MarketDataPort.recentIntradayBars() 추가
  → @Scheduled + KrxMarketCalendar + ShedLock
  → 데이터 staleness 감지

Phase 3: PAPER_LIVE 평가 엔진
  → OrderExecutorPort + PaperExecutor
  → LiveEvaluationService (load → evaluate → flush)
  → paper_signal_log 테이블

Phase 4: 모의 대시보드 + 룰 생애주기 UI
  → PaperDashboardPage, TradingMonitorPage, PaperReportPage
  → DRAFT → BACKTESTED → PAPER_LIVE 전환 UI

Phase 5: 토스증권 OAuth + 자격증명 저장
  → toss_credentials 테이블 (AES-256 암호화)
  → TossCredentialService + TossTokenManager

Phase 6: 실투자 주문 실행 + LIVE 승격
  → TossOrderExecutor
  → PAPER_LIVE → LIVE 승격 게이트
  → live_accounts, live_trades 테이블
```

---

*Sources: STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md (각 2026-06-20 연구)*
