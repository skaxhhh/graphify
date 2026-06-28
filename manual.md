# 모의투자 · 백테스트 시스템 매뉴얼

이 문서는 룰 기반 모의투자(Paper Trading)와 백테스트의 **핵심 동작 로직**을 정리한다.
"데이터를 언제·어디서·어떻게 수집하는지", "백테스트와 라이브가 각각 무슨 데이터를 쓰는지"를
실제 코드 기준으로 설명한다. (근거: `backend/src/main/java/com/graphify/...`)

---

## 0. 한눈에 보기

| 구분 | 데이터 | 수집 시점 | 종목 결정 |
|---|---|---|---|
| **일봉 적재** (EOD) | `market_bars` (일봉) | 수동(Admin) / 내부 스케줄러 / 룰 시작 시 | KOSPI200 또는 룰 유니버스 |
| **백테스트** | **5분봉**(`market_bars_intraday`)으로 시뮬레이션 + 일봉은 종목선정용 | 실행 시 DB캐시 우선, 없으면 Yahoo 즉시 조회 | 룰 유니버스 (symbols / volume_top_n) |
| **모의 라이브** | `market_bars_intraday` (5분봉) | 장중 **5분마다** 자동 | 룰 유니버스를 매 틱 재선정 |

> ⚠️ **자주 하는 오해**: "백테스트는 일봉으로 한다"는 정확하지 않다. 백테스트의 매매 시뮬레이션은
> **5분봉**으로 돌아간다. 일봉은 `volume_top_n` 룰에서 "그날 거래대금 상위 종목"을 고르는 용도로만 쓰인다.

---

## 1. 핵심 개념

### 1.1 룰(Rule)과 유니버스(Universe)
룰 정의(JSON)는 `entry`(진입), `exit`(청산: 조건 + `takeProfitPct`/`stopLossPct`), `sizing`(주문수량),
`constraints`(`cooldownBars` 등), 그리고 **`universe`(대상 종목)** 로 구성된다.
(`trading/rule/definition/RuleDefinition.java`)

유니버스 타입은 3가지:
- **`symbols`**: 고정 종목 리스트(`universe.symbols`). 사용자가 직접 지정한 종목만 대상.
- **`volume_top_n`**: "그 시점 거래대금 상위 N종목"을 **동적으로** 발굴. `market`(기본 `KOSPI`), `topN`(기본 10),
  `additionalSymbols`(수동 추가) 옵션.
- **`watchlist`**: `symbols`와 동일하게 취급(고정 목록).

### 1.2 데이터 종류와 테이블
- **일봉**: `market_bars` 테이블 (`MarketBarRepository`). 외부 소스 = Yahoo Finance(`fetchDailyOhlcv`).
- **5분봉(분봉)**: `market_bars_intraday` 테이블 (`MarketBarIntradayRepository`). interval=`"5m"`, 소스 = Yahoo Finance.
- 종목코드 → Yahoo 심볼 변환은 KRX 종목에 `.KS` 접미사를 붙인다(예: `005930` → `005930.KS`).

---

## 2. 일봉(EOD) 수집 — 언제, 어떻게

일봉은 **자동으로 매일 도는 인앱 스케줄러가 없다.** 다음 3가지 경로로 적재된다.

### 2.1 관리자 수동 트리거 (`/admin/market`)
`AdminMarketController` (`/api/v1/admin/market`):
- `POST /seed-kospi200` — KOSPI200 종목 마스터(`companies`)를 `kospi200.csv` 기준 UPSERT(멱등).
- `POST /ingest-kospi200/batch?offset&size` — `in_kospi200=true` 종목의 일봉을 **청크(기본 10종목)** 단위로 적재.
  프론트(`/admin/market`)가 `nextOffset`으로 `done=true`까지 순회한다.
  (한 요청에 99종목을 동기 처리하면 게이트웨이 타임아웃 → "CORS 에러"가 나므로 청크로 분할.)

### 2.2 내부(Cloud Scheduler) 트리거
`InternalMarketController` (`/internal/market/ingest`, 헤더 `X-Internal-Token` 필요):
- `interval=EOD`(기본) → `ingestDailyForActiveSymbols()` — **활성 룰 유니버스에 등장하는 종목**의 일봉.
- `interval=KOSPI200` → `ingestDailyForKospi200()` — KOSPI200 전체 일봉.
- `interval=MINUTE` → `ingestIntradayForActiveSymbols("5m","1d")` — 활성 룰 종목 5분봉.

> 일봉의 "매일 자동 적재"는 **Cloud Scheduler가 위 엔드포인트를 주기 호출하도록 설정돼 있어야** 동작한다.

### 2.3 룰 시작 시 즉시 수집(eager)
룰을 시작(RUNNING)하면 선정된 종목에 대해 **일봉+5분봉을 즉시 동기 수집**한다
(`PaperLifecycleService.eagerIngest` → `ingestDailyInNewTx` + `ingestIntradayInNewTx`).
단 종목 수가 `EAGER_INGEST_LIMIT`를 넘으면(예: KOSPI200 폴백) 즉시 수집을 건너뛰고 스케줄러 틱에 맡긴다.

각 종목 적재는 독립 트랜잭션(REQUIRES_NEW) + try/catch로 격리되어, **한 종목 실패가 전체를 중단시키지 않는다.**

---

## 3. 백테스트 — 무슨 데이터로, 어떻게

엔진: `IntradayBacktestEngine` (이름 그대로 **5분봉 인트라데이 백테스트**). 서비스: `BacktestService`.

### 3.1 데이터 소스
- 시뮬레이션 봉 = **5분봉** (`market_bars_intraday`).
- 로딩 순서(`loadBars`): **① DB 캐시 우선** 조회 → ② DB에 그 날짜가 아예 없으면 **Yahoo 5분봉 즉시 조회 후 DB 저장**.
- ⚠️ **한계**: Yahoo 인트라데이는 **최근 며칠치만** 제공한다. 따라서 과거 날짜를 백테스트하려면
  그 구간의 5분봉이 **이미 DB에 적재돼 있어야** 한다(없으면 그 날짜는 건너뜀 → 거래 없음).

### 3.2 기간 · 시간창
- 날짜 범위: 요청 `from`~`to`. 미지정 시 **최근 60일**(`to = 오늘`, `from = to-60`).
- 일중 시간창: 요청 `timeFrom`~`timeTo`. 미지정 시 **09:00–12:00 KST**. 이 창 안의 5분봉만 평가.

### 3.3 종목(유니버스) 해석 — 일봉이 쓰이는 지점
- **`symbols`/`watchlist`**: 유니버스 종목을 그대로 사용.
- **`volume_top_n`**: **여기서 일봉이 사용된다.** 후보군(해당 `market`의 KOSPI200 + `additionalSymbols`)의
  **일봉**을 미리 로드해두고, **날짜별로 그날 거래대금 상위 N종목**(`DbVolumeRankingAdapter`, 완결 일봉 기준)을 골라
  그 종목들만 그날 5분봉으로 평가한다.
- 사용자가 종목을 직접 고른 경우(`overrideSymbols`)는 유니버스 타입과 무관하게 그 종목으로 실행.

### 3.4 매매 로직
각 5분봉마다: 보유 중이면 `exit`(조건/익절/손절) 평가 → 충족 시 매도, 미보유면 `cooldownBars` 확인 후
`entry` 평가 → 충족 시 `sizing` 수량으로 매수. (`RuleEvaluator` + `PaperLedger`)
- 결과 지표: 최종 평가액, 수익률, MDD, 승률, **청산 횟수**(매도=라운드트립 수), Sharpe/Sortino/Profit Factor.
  - 참고: "청산 횟수"는 매도(SELL) 체결 수다. 매매이력 표는 매수+매도를 모두 보여주므로 행 수가 더 많다.
- 백테스트를 1회 실행하면 룰에 `backtested=true`가 찍혀 **라이브 승격(시작)** 이 가능해진다.

---

## 4. 모의 라이브(Paper-Live) — 종목 조사와 수집 주기

룰을 **시작(RUNNING)** 하면, 장중 동안 시스템이 자동으로 시세를 수집하고 룰을 평가·체결한다.

### 4.1 시작 게이팅
- **운영 창(평일 09:00–18:00, 공휴일 제외)** 안에서만 시작 가능. 밖이면
  `현재는 폐장입니다. 평일 09:00–18:00에만 전략을 시작할 수 있습니다.`(`ERR_MARKET_CLOSED`).
  (`KrxMarketCalendar.isOperatingWindowOpen`)
- 시작 시: 종목 선정(`resolveSymbols`) → eager 수집 → `paper_live_symbols`에 저장 → `PaperRun`(운영 회차) 생성.

### 4.2 종목 조사 — "어디서, 어떻게"
- **`volume_top_n` 룰**: **Naver 모바일 랭킹 API**(`NaverTradingValueRankingAdapter`)로
  **시장 전체 종목의 당일 누적 거래대금**을 받아 → ETF 제외 → 거래대금 내림차순 정렬 → **상위 N종목**을 고른다.
  (1분 TTL 캐시) 여기에 `additionalSymbols`와 **현재 보유 종목**을 합집합한다.
  - 라이브 랭킹을 못 가져오면(장외/조회 실패) `companies(in_kospi200)` 후보군으로 폴백, 다음 장중 틱에서 보정.
- **`symbols` 룰**: 정의된 고정 종목 그대로.
- **보유 종목은 top-N에서 빠져도 항상 수집·청산 평가 대상으로 유지**된다(들고 있는 포지션을 청산하기 위해).

### 4.3 수집·평가 주기 — 5분마다
스케줄러 `LiveDataScheduler`, cron **`0 */5 9-15 * * MON-FRI` (KST)** = **평일 09:00–15:55, 5분 간격**.
매 틱마다 순서대로:
1. **15:30 이후면 중단**(가드). KRX 정규장 종료 절사.
2. **비거래일(주말/공휴일)이면 중단**(가드).
3. **`volume_top_n` 룰 재선정**(`VolumeRankRefresher`): 위 4.2 로직으로 `paper_live_symbols`를 매 틱 갱신.
4. **수집 대상 = 모든 RUNNING 룰의 종목 합집합**(`activeSymbolsUnion`).
5. 각 종목 **5분봉 수집**(`ingestIntraday("5m","1d")` → Yahoo → `market_bars_intraday`).
   - **신선도 체크**: 최신 봉이 **25분 이상** 오래됐으면 경고(WARNING) 후 그 종목 평가 건너뜀.
     (Yahoo 5분봉이 KRX 대비 ~15분 지연되므로 임계 25분.)
6. **룰 평가·체결**(`LiveEvaluationService.evaluateTick` → `PaperExecutor`): 방금 적재된 봉으로 진입/청산 판단.
- 다중 인스턴스 환경에서 ShedLock으로 **이중 실행 방지**.

### 4.4 자동 중지(폐장)
스케줄러 `MarketCloseScheduler`, cron **`0 0 * * * *` (KST)** = **매시 정각** 점검.
운영 창(평일 09:00–18:00) **밖이면**(예: 18:00, 또는 공휴일) RUNNING 중인 모든 PAPER 룰을
**STOPPED로 일괄 전환**하고 열린 운영 회차(`PaperRun`)를 종료한다.

### 4.5 예시 시나리오 — "내일 09:00에 top10 룰 시작"

| 시각(KST) | 일어나는 일 |
|---|---|
| **09:00 시작 클릭** | 그 순간 거래대금 상위 10종목 선정 → 일봉·5분봉 즉시 수집 → 운영 회차 시작. **매매는 아직 안 함.** 단, 장 시작 직후라 누적 거래대금이 미미해 랭킹이 부실하면 `in_kospi200` 폴백. |
| **09:00 / 09:05 틱** | top-10 재선정 + 수집 + 평가 시도. 그러나 **9시봉(09:00–09:05)은 아직 Yahoo에 안 떴거나 ~15분 지연** → 신선도 가드(25분)에 걸려 평가가 비거나 직전 봉만 본다. |
| **약 09:15 틱** | 이 무렵 **9시봉이 Yahoo에 들어온다.** 비로소 09:00대 봉으로 진입/청산 판단이 정상적으로 돌기 시작. |
| **09:15 ~ 15:30** | 5분마다 (재선정 → 수집 → 평가 → 가상 매수/매도) 반복. |
| **15:30** | 시세 수집·평가 종료(틱 가드). 보유 포지션은 그대로 유지(평가만 멈춤). |
| **18:00** | 운영 창 종료 → 룰 자동 STOPPED, 회차 종료. |

> **"15분 지연이라 09:15부터 9시봉 수집"** — 맞다. 모의 라이브는 이 지연을 감수하는 대신, 각 의사결정이
> **확정된(closed) 봉** 기준으로 일관되게 이뤄진다는 점에서 의미가 있다(룩어헤드/체결가 왜곡 없음).
> 실거래로 가면 이 지연이 치명적이므로 §8의 실시간 시세 설계가 필요하다.

### 4.6 매매 디테일 (참고)
- **주문 수량(`sizing`)**: `fixed_cash`(지정 금액어치) / `full_cash`(가용현금 전액). 미지정·파싱실패 시 기본 100만원.
- **상·하한가**: 직전 거래일 일봉 종가 대비 **±29.5% 이상**이면 체결 불가로 판정해 그 틱 체결을 건너뛴다(`PaperExecutor.isPriceLimitPending`).
- **재진입 쿨다운(`constraints.cooldownBars`)**: 청산 후 N봉 동안 같은 종목 재진입 금지.
- **보유 종목 유지**: top-N에서 빠져도 보유 중이면 계속 수집·청산 평가 대상.

---

## 5. 시간/캘린더 정리 (중요)

두 개의 시간창이 **다르다**:

| 창 | 범위 | 용도 |
|---|---|---|
| **운영 창** | 평일 **09:00–18:00** (공휴일 제외) | 룰 **시작 가능** 여부 / 18:00 자동 중지 기준 |
| **라이브 수집·평가 창** | 평일 **09:00–15:30** (KRX 정규장) | 실제 5분봉 수집 + 룰 평가가 도는 시간 |

즉, 룰은 18:00까지 RUNNING으로 남아 있을 수 있지만, **새 시세 수집·평가는 15:30에 멈춘다**
(15:30–18:00 사이엔 RUNNING이어도 신규 틱이 없다). 거래일 판정은 주말 + `market_holidays` 테이블 기준.

---

## 6. 스케줄 · 엔드포인트 요약

### 6.1 인앱 스케줄러
| 스케줄러 | cron (KST) | 동작 |
|---|---|---|
| `LiveDataScheduler` | `0 */5 9-15 * * MON-FRI` | 5분마다 라이브 5분봉 수집 + 룰 평가 (15:30 절사) |
| `MarketCloseScheduler` | `0 0 * * * *` | 매시 정각, 운영창 밖이면 RUNNING 룰 자동 중지 |

### 6.2 주요 엔드포인트
| 메서드/경로 | 설명 |
|---|---|
| `POST /api/v1/admin/market/seed-kospi200` | KOSPI200 종목 마스터 시드(멱등) |
| `POST /api/v1/admin/market/ingest-kospi200/batch?offset&size` | KOSPI200 일봉 청크 적재(프론트가 순회) |
| `POST /internal/market/ingest?interval=EOD\|MINUTE\|KOSPI200` | Cloud Scheduler용 적재 트리거(내부 토큰 필요) |
| `POST .../backtest` (룰 백테스트) | 5분봉 백테스트 실행 |
| 룰 시작/중지 (`PaperLifecycleService`) | 운영 창 검사 + 종목 선정 + eager 수집 |

---

## 7. 운영 팁 · 주의

- **과거 백테스트가 "거래 0"이면** 그 구간 5분봉이 DB에 없을 가능성이 크다(Yahoo 인트라데이는 최근만 제공).
  먼저 해당 종목/기간의 5분봉을 적재하거나, 최근 구간으로 백테스트하라.
- **일봉 매일 자동 적재**는 Cloud Scheduler가 `/internal/market/ingest`를 호출하도록 설정돼 있어야 동작한다.
  설정이 없으면 `/admin/market`에서 수동 적재한다.
- **모의 라이브는 평일 09:00–15:30에만 실제로 시세를 수집·평가**한다. 그 외 시간엔 RUNNING이어도 틱이 없다.
- 외부 호출(Yahoo/Naver) 실패는 종목 단위로 격리되어 전체를 멈추지 않는다(로그 경고 후 건너뜀).

---

## 8. 실제 라이브(실거래) 전환 시 — 실시간 시세 설계 (미구현, 검토)

현재 모의 라이브는 **Yahoo 5분봉(~15분 지연)** 을 쓴다. 실거래에선 이 지연이 치명적이라 실시간 시세 소스가 필요하다.
아직 구현 전이며, 아래는 설계 방향 정리다.

### 8.1 이미 깔린 발판(seam)
- **`MarketDataPort`** — 백테스트/평가가 시세를 가져오는 추상 포트. 구현체만 교체하면 소스 전환 가능
  (현재 Yahoo/DB 어댑터 → 실시간 어댑터로 교체 지점).
- **`OrderExecutorPort`** — 주문 실행 추상 포트. 가상 체결(`PaperExecutor`) → 실제 주문 어댑터로 교체 지점.
- **Toss Invest OpenAPI 연동(부분)** — `openapi.tossinvest.com` OAuth 토큰 발급 + 계좌(잔고) 조회까지 구현돼 있다
  (`toss/` 패키지, 사용자별 암호화 토큰 저장 + 5분마다 자동 갱신). **단, 실시간 시세·주문 API는 아직 미연동.**

### 8.2 실시간 KRX 시세 옵션
- **증권사 OpenAPI 실시간 WebSocket** (가장 현실적)
  - 한국투자증권(KIS) OpenAPI: 실시간 체결가 WebSocket 제공(계좌 필요, 사실상 무료). 주문 API도 동일 플랫폼.
  - 토스증권 OpenAPI: 이미 OAuth/계좌가 붙어 있으니 **시세·주문 스펙이 제공된다면 가장 매끄러운 확장**. (제공 범위 확인 필요)
  - LS증권(xingAPI)/키움 OpenAPI+ 등도 가능하나 Windows/COM 종속 등 제약 있음.
- **유료 정식 시세**: 코스콤(KRX 공식) 실시간 시세 — 비용 큼, 개인/소규모엔 과함.
- **Yahoo/Naver**: 지연·비공식이라 **실거래 부적합**(현 모의 전용).

### 8.3 봉(bar) 만드는 방식
실시간 피드는 보통 **체결(tick) 스트림**이라, 두 가지 중 택일:
1. **틱 → 봉 직접 집계**: WebSocket으로 받은 체결을 메모리에서 1분/5분봉으로 합성, **봉 마감 시 평가 트리거**(이벤트 드리븐).
2. **증권사 분봉 API 폴링**: 분봉 조회 API를 짧은 주기로 폴링(WebSocket보다 단순하나 지연·호출제한 존재).

### 8.4 전환 시 함께 바뀌어야 할 것
- **수집 주기 모델**: 현재 5분 cron 폴링 → 실시간은 **봉 마감 이벤트 기반**으로 평가 트리거가 자연스럽다.
- **체결 모델**: 가상 체결(현재 봉 종가 기준) → 실제 주문/체결(부분체결·호가·슬리피지·수수료/세금 반영).
- **시간창**: 정규장 09:00–15:30(+ 동시호가) 정밀화, 장 시작/마감 동시호가 처리.
- **리스크/안전장치**: 일일 한도, 종목별 한도, 중복주문 방지, 장애 시 자동 정지 등.

> 요약: **포트(`MarketDataPort`/`OrderExecutorPort`) 뒤에 증권사 실시간 어댑터를 끼우고, 5분 폴링을 봉-마감 이벤트로
> 바꾸는 것**이 실거래 전환의 골격이다. Toss 연동이 이미 있으니 토스 OpenAPI의 시세/주문 스펙부터 확인하는 게 출발점.
