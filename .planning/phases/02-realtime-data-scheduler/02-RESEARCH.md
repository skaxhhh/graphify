# Phase 2: 실시간 데이터 수집 & 스케줄러 인프라 - Research

**Researched:** 2026-06-21
**Domain:** Spring Boot @Scheduled + ShedLock + KRX market calendar + JDBC
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **스케줄러 트리거:** @Scheduled(cron) + ShedLock — in-process, 외부 인프라 불필요
- **cron 범위:** 월~금 09:00–15:30 KST, 공휴일은 KrxMarketCalendar로 별도 필터
- **기존 InternalMarketController HTTP 트리거 유지** — 백테스팅/수동 트리거 공존
- **PAPER_LIVE/LIVE 실시간 수집 전용**
- **종목 선정 시점:** 룰 PAPER_LIVE 승격 시점에 volume_top_n 선정 → DB 저장, 매 틱마다 재계산 없음
- **다중 룰 동시 활성:** 유니온(합집합) — {A,B,C} ∪ {B,C,D} = {A,B,C,D}
- **룰 비활성화 시:** 해당 종목이 다른 활성 룰에도 없으면 수집 목록에서 제거
- **공휴일 관리:** `market_holidays` DB 테이블 + Flyway V31 마이그레이션 (DDL + 2026년 KRX 공휴일 INSERT)
- **KrxMarketCalendar 서비스:** 해당 날짜가 주말이거나 market_holidays에 있으면 skip
- **수집 실패 처리:** 즉시 재시도 없음 — 다음 5분 틱 자동 재시도
- **Staleness 감지(LIVE-04):** 최신 봉이 10분 이상 오래된 경우 WARNING 로그 + 해당 틱 평가 건너뜀

### Claude's Discretion
- cron 표현식 구체 값 (`"0 */5 9-15 * * MON-FRI"` 권장이나 15:30 절사 로직은 플래너 결정)
- `paper_live_symbols` 저장 방식 (별도 테이블 vs TradingRule 엔티티 컬럼)
- ShedLock 잠금 테이블 이름 및 잠금 유지 시간
- @EnableScheduling 설정 위치 (기존 Config 클래스 vs 신규)

### Deferred Ideas (OUT OF SCOPE)
- 공휴일 자동 갱신 (공공데이터포털 API 연동)
- 수집 실패 알림 (Slack/이메일)
- 15분봉/1시간봉 수집 지원
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| LIVE-01 | KRX 장 중(09:00–15:30 KST, 거래일)에 한해 5분마다 활성 종목의 인트라데이 봉을 수집한다 | cron + KrxMarketCalendar + 15:30 guard 패턴 |
| LIVE-02 | KRX 공휴일 목록을 유지하고, 장 외에는 평가를 건너뛴다 | market_holidays 테이블 + V31 마이그레이션 |
| LIVE-03 | 다중 인스턴스 환경에서 이중 평가가 발생하지 않도록 분산 잠금(ShedLock)을 적용한다 | ShedLock 7.7.0 JDBC provider |
| LIVE-04 | 데이터 수집 후 최신 봉이 10분 이상 오래된 경우 평가를 건너뛰고 경고를 기록한다 | MarketBarIntradayRepository + Instant 비교 |
</phase_requirements>

---

## Summary

Phase 2는 Spring Boot 내장 스케줄러(@Scheduled)와 ShedLock 분산 잠금을 결합하여 KRX 장 중(09:00–15:30 KST, 월~금, 공휴일 제외)에만 PAPER_LIVE/LIVE 룰의 활성 종목 5분봉을 수집하는 파이프라인을 구축한다. 기존 `MarketDataIngestionService.ingestIntraday()` 로직은 이미 완성되어 있으므로, 이 Phase의 핵심 작업은 (1) 스케줄러 어댑터 레이어 추가, (2) ShedLock 분산 잠금 설정, (3) KRX 공휴일 캘린더 서비스, (4) `paper_live_symbols` 종목 목록 관리, (5) V31 Flyway 마이그레이션 3종(market_holidays, shedlock, paper_live_symbols) 이다.

15:30 절사 문제: Spring cron `"0 */5 9-15 * * MON-FRI"`는 시간 범위로 `9-15` 시를 지원하지만 분 단위 절사(`15:30`이후 제외)는 불가능하다. 표준 해결책은 cron을 `9-14` 시 + 15시 전용 별도 cron `"0 0,5,10,15,20,25,30 15 * * MON-FRI"`를 조합하거나, 단순하게 `"0 */5 9-15 * * MON-FRI"`를 사용하고 스케줄러 메서드 내부에서 현재 시각이 15:30 이후면 즉시 return하는 guard 로직을 사용하는 것이다. 후자(내부 guard)가 더 단순하고 유지보수가 쉽다.

`paper_live_symbols`는 별도 DB 테이블(`paper_live_symbols`)로 관리하는 것이 권장된다. TradingRule 엔티티 컬럼(JSONB)에 embed하면 동시 수정 시 충돌 위험이 있고, 합집합 계산이 복잡해진다. 별도 테이블은 SQL로 단순 SELECT DISTINCT가 가능하다.

**Primary recommendation:** ShedLock 7.7.0 + JDBC provider, V31 단일 마이그레이션에 3개 테이블 DDL 포함, 스케줄러 내부 15:30 guard 사용, paper_live_symbols 별도 테이블 방식.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| shedlock-spring | 7.7.0 | @SchedulerLock 어노테이션 + AOP 연동 | 사실상 표준 Spring 분산 잠금 라이브러리 |
| shedlock-provider-jdbc-template | 7.7.0 | JdbcTemplate 기반 잠금 저장소 | 기존 PostgreSQL DataSource 재사용, 외부 인프라 불필요 |
| spring-boot-starter-web (기존) | 3.4.5 | - | 이미 build.gradle.kts에 포함 |
| spring-boot-starter-data-jpa (기존) | 3.4.5 | MarketBarIntradayRepository | 이미 포함 |

**ShedLock은 spring-boot-starter-jdbc가 별도 필요 없다** — JdbcTemplate은 spring-boot-starter-data-jpa가 이미 제공하는 DataSource에서 생성 가능.

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Flyway (기존) | Spring Boot 관리 | V31 마이그레이션 | 이미 포함, 추가 불필요 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| ShedLock JDBC | ShedLock Redis | Redis 추가 인프라 필요 — JDBC가 현재 스택에 맞음 |
| ShedLock JDBC | Quartz Scheduler | Quartz는 자체 10개+ 테이블 필요, 과잉 |
| 내부 guard (15:30) | 두 개 cron 조합 | 두 cron은 유지보수 복잡 — 내부 guard가 더 명확 |

**Installation (build.gradle.kts에 추가):**
```kotlin
implementation("net.javacrumbs.shedlock:shedlock-spring:7.7.0")
implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.7.0")
```

---

## Architecture Patterns

### Recommended Project Structure
```
backend/src/main/java/com/graphify/
├── config/
│   └── SchedulerConfig.java         # @EnableScheduling + @EnableSchedulerLock + LockProvider Bean
├── market/
│   ├── MarketDataIngestionService.java  # 기존 — 수정 없이 재사용
│   ├── KrxMarketCalendar.java           # NEW: 장중 판단, 공휴일 조회
│   ├── MarketHoliday.java               # NEW: JPA 엔티티 (market_holidays 테이블)
│   ├── MarketHolidayRepository.java     # NEW: findByHolidayDate()
│   └── LiveDataScheduler.java           # NEW: @Scheduled + @SchedulerLock 어댑터
├── trading/
│   └── rule/
│       ├── PaperLiveSymbol.java         # NEW: JPA 엔티티 (paper_live_symbols 테이블)
│       ├── PaperLiveSymbolRepository.java  # NEW: findByRuleId(), deleteByRuleId()
│       └── PaperLiveSymbolService.java  # NEW: 룰 승격 시 종목 선정 + 저장, 합집합 조회
└── resources/db/migration/
    └── V31__live_scheduler_infra.sql    # NEW: market_holidays + shedlock + paper_live_symbols DDL
```

### Pattern 1: ShedLock 설정

**What:** `@EnableSchedulerLock` + `LockProvider` Bean을 별도 `SchedulerConfig`에 정의
**When to use:** @EnableScheduling과 함께, 기존 SecurityConfig/MarketClientConfig와 분리
**Example:**
```java
// Source: https://github.com/lukas-krecan/shedlock (v7.7.0 README)
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "4m")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()   // DB 시각 사용 — 강력 권장
                .build()
        );
    }
}
```

`defaultLockAtMostFor = "4m"`: 5분 주기 태스크에서 이전 잠금이 해제되지 않은 채 다음 틱이 실행되지 않도록 틱 간격보다 짧게 설정.

### Pattern 2: @Scheduled + @SchedulerLock 어댑터

**What:** 스케줄러는 얇은 어댑터 레이어. 비즈니스 로직은 서비스로 위임
**When to use:** 항상 — 스케줄러 자체에 비즈니스 로직 넣지 않음
**Example:**
```java
// Source: https://github.com/lukas-krecan/shedlock
@Component
@RequiredArgsConstructor
public class LiveDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveDataScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private final KrxMarketCalendar calendar;
    private final PaperLiveSymbolService symbolService;
    private final MarketDataIngestionService ingestionService;

    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    @SchedulerLock(name = "liveDataIngestion", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void collectLiveData() {
        ZonedDateTime now = ZonedDateTime.now(KST);

        // 15:30 이후 guard
        if (now.toLocalTime().isAfter(MARKET_CLOSE)) {
            log.debug("Market closed (after 15:30), skipping tick");
            return;
        }

        // 공휴일/주말 guard
        if (!calendar.isTradingDay(now.toLocalDate())) {
            log.debug("Non-trading day {}, skipping tick", now.toLocalDate());
            return;
        }

        Set<String> symbols = symbolService.activeSymbolsUnion();
        if (symbols.isEmpty()) {
            log.debug("No active PAPER_LIVE symbols, skipping tick");
            return;
        }

        for (String symbol : symbols) {
            ingestionService.ingestIntraday(symbol, "5m", "1d");
        }
        log.info("Live intraday collection done: {} symbols at {}", symbols.size(), now);
    }
}
```

**핵심:** `zone = "Asia/Seoul"` 속성으로 Spring 스케줄러가 KST 기준으로 cron을 평가. 서버 TZ 설정과 무관하게 동작.

### Pattern 3: KrxMarketCalendar

**What:** 장중 판단 로직을 단일 서비스로 캡슐화
**When to use:** 스케줄러 + Phase 3 평가 엔진 양쪽에서 재사용
**Example:**
```java
@Service
public class KrxMarketCalendar {

    private final MarketHolidayRepository holidayRepository;

    public boolean isTradingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidayRepository.existsByHolidayDate(date);
    }
}
```

### Pattern 4: Staleness 감지 (LIVE-04)

**What:** 수집 직후 최신 봉 타임스탬프 검증
**When to use:** collectLiveData() 내에서 종목별 수집 후 또는 일괄 후처리
**Example:**
```java
// MarketBarIntradayRepository에 추가할 메서드
Optional<Instant> findMaxTsBySymbolAndInterval(String symbol, String interval);

// LiveDataScheduler에서 staleness 체크
Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
for (String symbol : symbols) {
    ingestionService.ingestIntraday(symbol, "5m", "1d");
    repository.findMaxTsBySymbolAndInterval(symbol, "5m")
        .filter(ts -> ts.isBefore(tenMinutesAgo))
        .ifPresent(ts -> log.warn(
            "STALE data for {}: latest bar at {} (>10m ago), skipping evaluation",
            symbol, ts));
}
```

### Pattern 5: PaperLiveSymbol — 별도 테이블 방식

**What:** PAPER_LIVE 룰의 종목 목록을 독립 테이블로 관리
**When to use:** 룰 승격(PAPER_LIVE) 시 INSERT, 룰 비활성화 시 DELETE, 스케줄러 틱마다 SELECT DISTINCT
**Example:**
```java
@Entity
@Table(name = "paper_live_symbols")
public class PaperLiveSymbol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rule_id", nullable = false) private Long ruleId;
    @Column(nullable = false) private String symbol;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    // ... getters, @PrePersist
}

// Repository
public interface PaperLiveSymbolRepository extends JpaRepository<PaperLiveSymbol, Long> {
    List<PaperLiveSymbol> findByRuleId(Long ruleId);
    void deleteByRuleId(Long ruleId);
    @Query("SELECT DISTINCT p.symbol FROM PaperLiveSymbol p " +
           "WHERE p.ruleId IN :ruleIds")
    List<String> findDistinctSymbolsByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
```

### Anti-Patterns to Avoid

- **@SchedulerLock 없이 @Scheduled만 사용:** 다중 인스턴스에서 이중 실행 → LIVE-03 위반
- **스케줄러에 직접 비즈니스 로직 작성:** 테스트 불가, 재사용 불가
- **매 틱마다 volume_top_n 재계산:** API 호출 폭발, look-ahead bias 위험
- **TradingRule 엔티티에 symbols JSONB 컬럼으로 embed:** 다중 룰 합집합 계산 복잡, 동시성 문제
- **zone 속성 없이 cron 사용:** 서버 TZ 변경 시 스케줄 오작동
- **lockAtMostFor > 틱 간격(5m):** 이전 실패 잠금이 다음 틱을 막음

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 분산 잠금 | 직접 DB row lock 구현 | ShedLock 7.7.0 | 잠금 만료, AOP 연동, 테스트 지원 모두 포함 |
| 스케줄러 중복 방지 | synchronized / volatile 플래그 | ShedLock | 프로세스 간 공유 안 됨 |
| 공휴일 판단 로직 | 인라인 if-else in scheduler | KrxMarketCalendar 서비스 | Phase 3 평가 엔진 재사용 필요 |

**Key insight:** ShedLock의 JDBC provider는 기존 PostgreSQL DataSource를 그대로 사용하므로 인프라 추가 없이 완전한 분산 잠금을 제공한다.

---

## Common Pitfalls

### Pitfall 1: cron zone 미지정

**What goes wrong:** `@Scheduled(cron = "0 */5 9-15 * * MON-FRI")`만 사용 시 서버 UTC 기준으로 평가 — KST 09:00 = UTC 00:00으로 동작해 실제 KRX 장 시간과 불일치
**Why it happens:** Spring의 기본 cron 평가는 JVM 기본 TZ 사용
**How to avoid:** 반드시 `zone = "Asia/Seoul"` 속성 추가
**Warning signs:** 스케줄러가 새벽에 실행되는 로그

### Pitfall 2: ShedLock lockAtMostFor > 틱 간격

**What goes wrong:** 인스턴스가 중간에 죽으면 잠금이 `lockAtMostFor` 동안 유지 — 다음 틱(5분 후)에 다른 인스턴스가 실행 불가
**Why it happens:** 기본값이 너무 길게 설정
**How to avoid:** `lockAtMostFor = "4m"` — 틱 간격(5분)보다 짧게
**Warning signs:** 배포 후 스케줄러가 4분 이상 멈추는 현상

### Pitfall 3: Spring cron 6-field vs Unix cron 5-field

**What goes wrong:** Unix cron 형식(`*/5 9-15 * * 1-5`)을 Spring에 그대로 사용 — `@Scheduled`는 6-field (첫 번째 필드가 초)
**Why it happens:** Unix cron 습관
**How to avoid:** Spring cron 형식: `"초 분 시 일 월 요일"` → `"0 */5 9-15 * * MON-FRI"`

### Pitfall 4: ShedLock 테이블 DDL 누락

**What goes wrong:** shedlock 테이블 없이 실행 시 `Table 'shedlock' doesn't exist` 에러 → 전체 스케줄러 실패
**Why it happens:** Flyway 마이그레이션에 포함 안 함
**How to avoid:** V31 마이그레이션에 shedlock 테이블 DDL 포함 (배포 시 자동 생성)

### Pitfall 5: `ingestIntraday` 는 Yahoo `range=1d` — 당일 봉만 반환

**What goes wrong:** 이전 미수집 봉이 누락될 수 있음 (장 시작 직후 인스턴스 재시작 등)
**Why it happens:** Yahoo `range=1d`는 당일 분봉만 반환
**How to avoid:** Phase 2 범위에서는 허용 가능 — 다음 틱 재시도로 커버. Gap fill은 Phase 3에서 필요시 처리.

### Pitfall 6: @Transactional + @SchedulerLock 순서

**What goes wrong:** @Transactional이 @SchedulerLock보다 외부에 있으면 트랜잭션 커밋 전 잠금 해제 → 다른 인스턴스 중복 실행
**Why it happens:** AOP 프록시 순서
**How to avoid:** ShedLock은 @SchedulerLock이 @Transactional 외부를 감싸도록 설계됨 — 스케줄러 메서드에만 @SchedulerLock 적용, @Transactional은 서비스 레이어에 위치

---

## Code Examples

### V31 Flyway 마이그레이션 DDL
```sql
-- Source: ShedLock README (GitHub lukas-krecan/shedlock)
-- + project pattern from V29__market_bars.sql

-- 1. KRX 공휴일 테이블
CREATE TABLE IF NOT EXISTS market_holidays (
    id           BIGSERIAL PRIMARY KEY,
    holiday_date DATE        NOT NULL,
    description  VARCHAR(100),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_market_holidays_date UNIQUE (holiday_date)
);

-- 2026년 KRX 공휴일 (연말 연초 + 명절 + 공휴일)
INSERT INTO market_holidays (holiday_date, description) VALUES
    ('2026-01-01', '신정'),
    ('2026-01-28', '설날 연휴'),
    ('2026-01-29', '설날'),
    ('2026-01-30', '설날 연휴'),
    ('2026-03-01', '삼일절'),
    ('2026-05-05', '어린이날'),
    ('2026-05-25', '부처님오신날'),
    ('2026-06-06', '현충일'),
    ('2026-08-15', '광복절'),
    ('2026-09-24', '추석 연휴'),
    ('2026-09-25', '추석'),
    ('2026-09-26', '추석 연휴'),
    ('2026-10-03', '개천절'),
    ('2026-10-09', '한글날'),
    ('2026-12-25', '크리스마스'),
    ('2026-12-31', '연말 휴장')
ON CONFLICT (holiday_date) DO NOTHING;

-- 2. ShedLock 테이블 (PostgreSQL)
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- 3. PAPER_LIVE 활성 종목 목록
CREATE TABLE IF NOT EXISTS paper_live_symbols (
    id         BIGSERIAL PRIMARY KEY,
    rule_id    BIGINT       NOT NULL REFERENCES trading_rules(id) ON DELETE CASCADE,
    symbol     VARCHAR(32)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_paper_live_symbols UNIQUE (rule_id, symbol)
);
CREATE INDEX IF NOT EXISTS idx_paper_live_symbols_rule
    ON paper_live_symbols (rule_id);
```

### build.gradle.kts 변경
```kotlin
// 기존 dependencies 블록에 추가
implementation("net.javacrumbs.shedlock:shedlock-spring:7.7.0")
implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.7.0")
```

### MarketHolidayRepository
```java
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, Long> {
    boolean existsByHolidayDate(LocalDate date);
}
```

### MarketBarIntradayRepository 추가 쿼리 (staleness용)
```java
// Source: 기존 MarketBarIntradayRepository 패턴 확장
@Query("SELECT MAX(m.ts) FROM MarketBarIntraday m WHERE m.symbol = :symbol AND m.interval = :interval")
Optional<Instant> findMaxTsBySymbolAndInterval(
    @Param("symbol") String symbol,
    @Param("interval") String interval
);
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ShedLock 4.x (Spring Boot 2) | ShedLock 7.7.0 (Spring Boot 3) | 2023-2024 | API 호환, 어노테이션 동일 |
| `@EnableSchedulerLock(mode=PROXY_METHOD)` | 기본 PROXY_SUBCLASS | ShedLock 5.x+ | 기본값으로 충분 |
| Quartz Scheduler | ShedLock | 2020년대 | ShedLock이 가볍고 Spring 친화적 |

**Deprecated/outdated:**
- `net.javacrumbs.shedlock:shedlock-provider-jdbc:*` (직접 JDBC) → `shedlock-provider-jdbc-template` (JdbcTemplate) 사용
- ShedLock 4.x의 `@SchedulerLock(name, lockAtMostFor, lockAtLeastFor)` 문자열 형식 → 동일하게 유지됨, 호환

---

## Open Questions

1. **2026년 KRX 공휴일 목록 정확성**
   - What we know: 법정 공휴일 기반 추정 목록 작성 가능
   - What's unclear: KRX 자체 임시 휴장일 (예: 국가 행사) 및 대체 공휴일 정확한 날짜
   - Recommendation: V31에 알려진 공휴일 INSERT 후, KRX 공시 확인 시 V32로 추가. 임시 휴장은 수동 INSERT 허용.

2. **Yahoo Finance `range=1d` 5분봉 반환 시각 범위**
   - What we know: `includePrePost=false`로 정규 장 시간 봉만 반환
   - What's unclear: Yahoo가 KRX 정규장 09:00–15:30 봉을 정확히 커버하는지, 또는 마지막 봉이 15:25인지
   - Recommendation: 15:25 봉이 마지막 정상 봉 — staleness check 시 15:35 이후 최신봉 없으면 정상으로 간주하는 로직 필요. 실제 동작은 첫 실행에서 확인.

3. **`PaperLiveSymbolService`의 종목 선정 트리거 시점**
   - What we know: 룰 PAPER_LIVE 승격 시점에 선정 — Phase 4(RULE-01)에서 승격 API 구현
   - What's unclear: Phase 2에서 승격 API가 아직 없으므로 테스트용 수동 INSERT 또는 stub 필요
   - Recommendation: Phase 2에서 `PaperLiveSymbolService.assignSymbols(ruleId, symbols)` 메서드만 구현. 호출은 Phase 4 RULE-01 구현 시 연결.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (Spring Boot Test 3.4.5) |
| Config file | `backend/src/test/resources/application.properties` (존재) |
| Quick run command | `./gradlew test --tests "com.graphify.market.*" --tests "com.graphify.trading.rule.*"` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| LIVE-01 | isTradingDay()가 평일을 true 반환 | unit | `./gradlew test --tests "*.KrxMarketCalendarTest"` | ❌ Wave 0 |
| LIVE-01 | isTradingDay()가 주말을 false 반환 | unit | `./gradlew test --tests "*.KrxMarketCalendarTest"` | ❌ Wave 0 |
| LIVE-01 | 15:30 이후 guard — collectLiveData() 즉시 return | unit | `./gradlew test --tests "*.LiveDataSchedulerTest"` | ❌ Wave 0 |
| LIVE-02 | isTradingDay()가 market_holidays에 있는 날 false 반환 | unit | `./gradlew test --tests "*.KrxMarketCalendarTest"` | ❌ Wave 0 |
| LIVE-03 | @SchedulerLock 설정 검증 (어노테이션 존재) | unit (reflection) | `./gradlew test --tests "*.LiveDataSchedulerTest"` | ❌ Wave 0 |
| LIVE-04 | 최신 봉이 10분 초과 시 WARNING 로그 + skip | unit | `./gradlew test --tests "*.LiveDataSchedulerTest"` | ❌ Wave 0 |
| LIVE-04 | findMaxTsBySymbolAndInterval JPQL 쿼리 정확성 | unit (H2) | `./gradlew test --tests "*.MarketBarIntradayRepositoryTest"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew test --tests "com.graphify.market.*" --tests "com.graphify.trading.rule.PaperLiveSymbol*"`
- **Per wave merge:** `./gradlew test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/java/com/graphify/market/KrxMarketCalendarTest.java` — covers LIVE-01, LIVE-02 (Mockito, no DB)
- [ ] `backend/src/test/java/com/graphify/market/LiveDataSchedulerTest.java` — covers LIVE-01 (15:30 guard), LIVE-03 (annotation check), LIVE-04 (staleness warning)
- [ ] `backend/src/test/java/com/graphify/market/MarketBarIntradayRepositoryMaxTsTest.java` — covers LIVE-04 JPQL (H2 @DataJpaTest + `spring.flyway.enabled=false`)

---

## Sources

### Primary (HIGH confidence)
- [GitHub: lukas-krecan/shedlock](https://github.com/lukas-krecan/shedlock) — 버전(7.7.0), DDL, 어노테이션, LockProvider 설정 직접 확인
- 프로젝트 소스 코드 직접 분석: `MarketDataIngestionService`, `MarketBarIntraday`, `MarketBarIntradayRepository`, `TradingRule`, `YahooFinanceChartClient`, `build.gradle.kts`, `application.yml`, `V29__market_bars.sql`, `V30__kospi200_universe.sql`

### Secondary (MEDIUM confidence)
- [Spring.io Blog: New in Spring 5.3 — Improved Cron Expressions](https://spring.io/blog/2020/11/10/new-in-spring-5-3-improved-cron-expressions/) — Spring cron 6-field 형식 및 `zone` 속성 확인
- WebSearch: ShedLock Spring Boot 3 설정 — 여러 소스 cross-reference 완료

### Tertiary (LOW confidence)
- 2026년 KRX 공휴일 목록 — 법정 공휴일 기반 추정, KRX 공식 공시로 최종 확인 필요

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — ShedLock 7.7.0 GitHub README 직접 확인, 버전/DDL/어노테이션 검증
- Architecture: HIGH — 기존 코드베이스 패턴(V29 DDL, MarketDataIngestionService, TestConfig) 분석 기반
- Pitfalls: HIGH — Spring cron zone, ShedLock lockAtMostFor, 6-field 형식은 공식 문서 기반
- KRX 공휴일 목록: LOW — 공식 KRX 공시 확인 필요

**Research date:** 2026-06-21
**Valid until:** 2026-07-21 (ShedLock은 안정적, 30일 유효)
