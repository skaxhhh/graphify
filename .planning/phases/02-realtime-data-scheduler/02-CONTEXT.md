# Phase 2: 실시간 데이터 수집 & 스케줄러 인프라 - Context

**Gathered:** 2026-06-21
**Status:** Ready for planning

<domain>
## Phase Boundary

KRX 장 중(09:00–15:30 KST, 거래일)에만 PAPER_LIVE 활성 룰의 종목을 5분마다 수집하는 스케줄러 인프라를 구축한다. 다중 인스턴스 환경에서 이중 수집이 발생하지 않도록 ShedLock 분산 잠금을 적용한다.

**백테스팅은 이 Phase 범위 밖:** 백테스팅은 on-demand Yahoo Finance 폴백(Phase 1 완성)으로 처리. 스케줄러는 PAPER_LIVE / LIVE 전용이다.

**이 Phase가 제공하는 것:** Phase 3(PAPER_LIVE 평가 엔진)이 소비할 실시간 분봉 데이터 수집 파이프라인.

</domain>

<decisions>
## Implementation Decisions

### 스케줄러 트리거 방식
- **@Scheduled(cron) + ShedLock** — in-process 스케줄러, 외부 인프라(Cloud Scheduler 등) 불필요
- cron 표현식 설정으로 월–금 09:00–15:30 구간만 실행 (공휴일은 KrxMarketCalendar로 별도 필터)
- 기존 `InternalMarketController` HTTP 트리거 패턴과 **공존** — 백테스팅/수동 트리거용 HTTP 엔드포인트는 유지
- PAPER_LIVE / LIVE 실시간 수집에만 @Scheduled 사용

### PAPER_LIVE 종목 선정 및 수집 시작 시점
- **룰 PAPER_LIVE 승격(활성화) 시점**에 그날의 volume_top_n 종목을 선정하여 DB에 저장
- 이후 cron은 저장된 종목 목록만 조회하여 수집 (매 틱마다 volume_top_n 재계산 안 함)
- **다중 룰 동시 활성**: 유니온(합집합) — 룰 A 종목 {A,B,C} + 룰 B 종목 {B,C,D} → 수집 {A,B,C,D}
- 룰 비활성화(PAUSED/종료) 시 해당 룰 종목이 다른 활성 룰에도 없으면 수집 목록에서 제거

### KRX 공휴일 관리
- **`market_holidays` DB 테이블** — 연 1회 수동 등록 방식
- **Flyway V31 마이그레이션**에 테이블 DDL + 2026년 KRX 공휴일 INSERT 포함 (배포 시 자동 적용)
- `KrxMarketCalendar` 서비스: 해당 날짜가 주말이거나 market_holidays에 있으면 skip
- 다음 연도 공휴일은 연말에 V32 마이그레이션 추가로 관리

### 수집 실패 처리
- **Claude's Discretion**: 실패 시 즉시 재시도 없음 — 다음 5분 틱에 자연스럽게 재시도
- Staleness 감지(LIVE-04): 최신 봉이 10분 이상 오래된 경우 WARNING 로그 + 해당 틱 평가 건너뜀 (요건 명시)

### Claude's Discretion
- cron 표현식 구체 값 (`"0 */5 9-15 * * MON-FRI"` 권장이나 15:30 절사 로직은 플래너 결정)
- `paper_live_symbols` 저장 방식 (별도 테이블 vs TradingRule 엔티티 컬럼)
- ShedLock 잠금 테이블 이름 및 잠금 유지 시간
- @EnableScheduling 설정 위치 (기존 Config 클래스 vs 신규)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MarketDataIngestionService` — 종목별 분봉 수집 로직 이미 구현. `@Scheduled` 어댑터를 위에 씌우면 됨
- `MarketBarIntraday` + `MarketBarIntradayRepository` — 분봉 저장 엔티티/저장소 이미 존재
- `YahooFinanceChartClient.fetchIntradayForDateRange()` — Phase 1에서 추가된 날짜범위 수집 메서드
- `InternalMarketController` — 기존 HTTP 트리거, 유지하며 공존
- `TradingRuleRepository` — PAPER_LIVE 상태 룰 조회용

### Established Patterns
- Flyway 마이그레이션: V30까지 존재 → V31이 next
- 서비스 계층: `@Service` + `@Transactional` + 생성자 주입
- 로깅: SLF4J `LoggerFactory.getLogger()`
- 설정: `application.properties` 기반, `GraphifyMarketProperties` 패턴

### Integration Points
- `MarketDataPort` 인터페이스 — `historicalDailyBars()` 외 `recentIntradayBars()` 메서드 추가 필요 (Phase 3에서 소비)
- `TradingRule` 엔티티 — PAPER_LIVE 상태 전환 시 종목 선정 트리거 연결점
- Flyway: V31 마이그레이션으로 `market_holidays` + `shedlock` 테이블 추가

</code_context>

<specifics>
## Specific Ideas

- 스케줄러는 PAPER_LIVE/LIVE 전용 — 백테스팅과 완전히 분리된 파이프라인
- 활성화 시점 volume_top_n으로 종목을 고정하는 이유: look-ahead bias 방지 + API 호출 최소화
- ShedLock은 단일 인스턴스에도 적용 — 향후 스케일아웃 대비

</specifics>

<deferred>
## Deferred Ideas

- 공휴일 자동 갱신 (공공데이터포털 API 연동) — 외부 API 의존성, 별도 마일스톤
- 수집 실패 알림(Slack/이메일) — 모니터링 인프라 별도 구성
- 15분봉/1시간봉 수집 지원 — Phase 1 deferred와 동일, v2

</deferred>

---

*Phase: 02-realtime-data-scheduler*
*Context gathered: 2026-06-21*
