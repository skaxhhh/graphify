# Release Notes

---

## 다음 계획 (Roadmap)

**Phase 7 — TradingView webhook 연동** (예정)
- 새 룰 타입 `TRADINGVIEW` + 룰별 `webhook_token`. `POST /api/webhook/tv/{token}`이 페이로드를 즉시 큐잉하고 100ms 내 200 반환(비동기 처리).
- 신호 파서: JSON(`{symbol, action}`) 우선 → 실패 시 LLM(Claude API) fallback으로 BUY/SELL/UNKNOWN 판정. UNKNOWN은 로그만.
- 종목은 사전 등록된 KOSPI 시총 상위 풀(`tv_supported_symbols`)에서 선택, 기존 `PaperExecutor`로 가상 체결. TRADINGVIEW 룰은 "TradingView에서 보기" 링크 제공.

**Phase 8 — 실투자 주문 실행 & LIVE 승격** (예정)
- 토스증권 REST 실주문 발행, 실시간 시세 LIVE 평가, API 연속 실패 시 서킷 브레이커.

**기술 부채 / 후속 (백로그)**
- **종목 마스터 동기화**: Naver 거래대금 상위로 선정된 종목이 `companies`에 없으면 RSI 평가용 분봉 적재(Yahoo 심볼 해석)가 막힘 → 마스터 자동 적재 필요.
- **401 자동 토큰 갱신**: access token 15분 만료 시 refresh token으로 자동 재발급(현재는 재로그인 필요).
- **우선주 제외**: Naver `stockEndType`이 우선주를 stock으로 분류 → 보통주 한정 필터 추가.
- **미사용 정리**: `YahooCumulativeVolumeAdapter` + `in_kospi200` 광역 ingest 경로(현재 fallback 보존).

---

## v1.6.0 — 운영 빈 DB 내성: KOSPI200 시드 + 유니버스 폴백 (2026-06-27)

### Overview
운영 빈 DB에서 `volume_top_n` 백테스트("유니버스에 수집된 종목이 없습니다")와 모의 start("유니버스 종목을 확인할 수 없습니다")가 실패하던 문제 해결. 근인은 운영 `companies.in_kospi200` 미시드(V30 UPDATE가 사전 존재 행에만 적용) + KOSPI200 일봉 적재 경로 부재(`ingestDailyForKospi200()`가 호출처 없는 dead code). 백테스트의 과거 거래대금 상위 선정 로직 자체는 이미 정상 — 데이터·부트스트랩만 빠져 있었음.

### 관리자 부트스트랩 (Piece 1·2)
- **KOSPI200 마스터 시드:** 정적 리소스 `resources/data/kospi200.csv`(99종목, SoT)를 ticker 기준 UPSERT — `market=KOSPI`, `instrumentType=COMMON_STOCK`, `in_kospi200=true`. 멱등. `POST /api/v1/admin/market/seed-kospi200` (ROLE_ADMIN).
- **KOSPI200 일봉 적재:** `POST /api/v1/admin/market/ingest-kospi200` (ROLE_ADMIN) + 외부 스케줄러용 `POST /internal/market/ingest?interval=KOSPI200` (X-Internal-Token). dead code였던 `ingestDailyForKospi200()` 연결.
- 관리자 UI: 시장데이터 패널(`AdminMarketDataPage`)에 시드/적재 버튼.

### 유니버스 폴백 (Piece 3)
- 빈 유니버스 에러를 사유별로 분리 — 백테스트 `ERR_BACKTEST_UNIVERSE_EMPTY`("거래대금 데이터 없음"), 모의 `ERR_LIFECYCLE_005`("실시간 랭킹 실패"). 둘 다 "종목을 직접 선택하세요" 안내.
- 백테스트 `overrideSymbols`, 모의 start 바디 `{overrideSymbols}` 추가 — 비어있지 않으면 유니버스 자동해석을 우회해 선택 종목 사용(self-heal/eagerIngest로 적재). 기존 무바디 호출 호환.
- 프론트 공통 `shared/CompanyPickerModal` — 회사 검색(다중선택·페이지네이션) 폴백 모달. 백테스트·모의 start 양쪽 재사용, 재시도 루프 가드 포함.

### 검증
- backend `./gradlew compileJava compileTestJava test` = BUILD SUCCESSFUL (128 tests). frontend `tsc -b && vite build` = clean.

### 운영 적용 절차
1. `POST /api/v1/admin/market/seed-kospi200` (또는 관리자 화면 버튼) → companies 플래그 복구.
2. `POST /api/v1/admin/market/ingest-kospi200` → KOSPI200 일봉 적재.
3. 이후 `volume_top_n` 백테스트/모의 start 정상화. (선택) `interval=KOSPI200` 일배치를 외부 스케줄러에 등록해 자동화.

---

## v1.5.3 — 룰 중지 회귀 수정 & 백테스트 차트 매매 근거 (2026-06-24)

### Overview
사용자 UI 테스트 피드백 반영. 룰 중지가 화면에 반영되지 않던 회귀 버그를 Playwright로 재현·수정하고, 종목명 표시·백테스트 시간 정밀도·차트 매매 근거 표시를 보강했다.

### 버그 수정
- **룰 중지 미반영 (v1.5.1/BUG-1 회귀):** `PaperLiveSymbolRepository.deleteByRuleId`의 `@Modifying(clearAutomatically=true)`가 `stop()`에서 아직 flush되지 않은 `run_status=STOPPED` UPDATE를 영속성 컨텍스트 clear로 폐기 → HTTP 200·응답 STOPPED이지만 DB는 RUNNING 잔류. `flushAutomatically=true` 추가로 삭제 직전 flush 보장. Playwright로 배지("실행 중"→"중지됨")·버튼("중지"→"시작")·DB(STOPPED) 검증.

### 종목명 표시 (#1)
- `SymbolNameService`: `companies` 테이블 → 없으면 Naver 개별종목 API(`/api/stock/{code}/basic`) 폴백 → 인메모리 캐시. 대시보드·거래이력·백테스트에서 사용 → 마스터 미적재 종목(SK, LS ELECTRIC 등)도 종목명 표시.
- `PaperPositionItem`/`PaperTradeHistoryItem` DTO에 `companyName` 추가, 프론트 렌더 `{명 ? "명 (코드)" : 코드}` 폴백.

### 백테스트 시간 정밀도 & 차트 (#3)
- `PaperLedger` 체결 타임스탬프 `LocalDate`→`LocalDateTime`, 엔진이 5분봉 시각(`barDt`) 전달 → 일자만(00:00)에서 5분봉 정밀도로 복원.
- 차트: 클릭한 체결 봉을 중앙(±20봉)에 정렬(`setVisibleLogicalRange`, 기존 `scrollToRealTime` 제거).

### 매매 근거 차트 표시 (#4)
- 거래 클릭 시 rationale 기반 지표 렌더: RSI → 하단 서브차트(lightweight-charts v5 `addPane`, 30/70 기준선), SMA/EMA → 메인 오버레이.
- rationale 파서가 백테스트(최상위 `conditions`)·라이브 이력(`rationale.conditions`) 두 구조 모두 지원.
- 차트 위 **근거 패널**: 저장된 실제 지표값 명시 — `RSI(14) < 30 (실제 RSI(14) = 18.3) ✓`.

---

## v1.5.2 — 시장 전체 장중 거래대금 랭킹 소스 (Naver) (2026-06-23)

### Overview
거래량 상위 유니버스가 사전 적재 후보 풀(`in_kospi200`)에 갇혀 "시장 전체에서 거래대금 상위를 그때그때 발굴"하지 못하던 한계를 해소. Naver 모바일 증권 API로 시장 전체 장중 거래대금 랭킹을 직접 받아 후보 풀 의존을 제거했다.

### 백엔드
- `NaverStockRankingClient`: `m.stock.naver.com/api/stocks/marketValue/{market}` 페이징 조회 → 종목별 누적 거래대금(`accumulatedTradingValueRaw`)·ETF 구분 파싱(connect/read 타임아웃 적용).
- `NaverTradingValueRankingAdapter` (`VolumeRankingProvider` 구현): ETF 제외 → 거래대금 DESC → topN, 시장별 1분 TTL 캐시.
- 라이브 랭킹 주입을 Yahoo→Naver 어댑터로 교체(`VolumeRankRefresher`, `LiveEvaluationService`). 후보 풀 사전 적재 불필요 → 부트스트랩 의존성 해소. 선정 종목만 `paper_live_symbols`에 저장.

### 검증
- 라이브 13:50 틱에서 Naver 거래대금 top-10 정확 선정(보유 포지션 union 포함), DB 후보 풀 밖 종목 포함 확인.

---

## v1.5.1 — 라이브 유니버스 후속 수정 (2026-06-23)

### 버그 수정
- **BUG-1 매 틱 재선정 실패:** `assignSymbols`의 파생 삭제+insert가 Hibernate insert-before-delete flush 순서로 `uq_paper_live_symbols` 중복키 위반 → 첫 배정 후 유니버스 고정. `deleteByRuleId`를 벌크 `@Modifying` DELETE로 전환해 해결.

### 변경
- **랭킹 기준 거래량→거래대금:** 라이브/백테스트 랭킹 쿼리를 `SUM(volume×close)` 기준으로 통일(저가 대량거래주 왜곡 방지).
- **staleness 10→25분:** Yahoo 5분봉 ~15분 지연 대응 — 장중 평가가 stale로 전면 스킵되던 문제 완화.

---

## v1.5.0 — 실시간 거래량 상위 유니버스 (Phase 6.7) (2026-06-23)

### Overview
`volume_top_n` 룰이 "그날 거래량 상위 종목"을 동적으로 선정하는 유니버스 타입을 도입. KRX MDC 인증 장벽으로 Yahoo 5분봉 누적 집계를 라이브 fallback으로 채택했다.

### 백엔드
- `VolumeRankingProvider` 포트 + 구현 2종: `DbVolumeRankingAdapter`(백테스트·일봉), `YahooCumulativeVolumeAdapter`(라이브·5분봉 누적).
- `VolumeRankRefresher`: 매 틱 `volume_top_n` 룰 재선정 — 새 top-N ∪ 보유 포지션 → `paper_live_symbols` 갱신(진입은 top-N 멤버만, 청산은 보유 전체).
- V36 Flyway: `companies.instrument_type` 컬럼(ETF/ETN/우선주 제외용 COMMON_STOCK 필터).

---

## v1.4.0 — 룰 설정/운영 역할 분리 & 매매 근거 (2026-06-22)

### Overview
`config_status`/`run_status` 2축 상태 모델 도입으로 전략 설정과 전략 운영 화면의 역할을 완전 분리했다. `EvalResult` 리치 반환 타입으로 백테스트·모의 거래 양쪽에서 매매 조건식·지표값·청산 사유를 캡처해 TradeRationaleRow 아코디언으로 표시한다.

### 신규 기능
- **전략 설정** (`/paper/rules`): config 배지(DRAFT/ACTIVE) + activate/deactivate 토글. run 제어 없음.
- **전략 운영** (`/paper/rules-lifecycle`): ACTIVE 룰만 표시. run 배지(실행 중/중지됨) + 시작/중지.
- **매매 근거 아코디언**: 백테스트/모의 거래 행 클릭 → `RSI(14)=28.3 < 30.00 ✓` 형식 + 청산 사유(익절/손절/지표).
- **MON-05 상하한가 감지**: `PaperExecutor` ±29.5% 가격 변동 → PRICE_LIMIT_PENDING 처리.

### 백엔드
- V35 Flyway: `config_status`/`run_status` 컬럼 추가 + 기존 행 마이그레이션 + run_status 인덱스
- `PaperLifecycleService`: activate/deactivate/start/stop 4-메서드 2축 상태 머신
- `RuleEvaluator.evalEntry/evalExit`: 조건별 지표값·통과여부·청산사유 포함 `EvalResult` 반환
- `IntradayBacktestEngine.buildRationale()`: EvalResult → rationaleJson → BacktestResult.TradeDto 인라인
- `LiveEvaluationService.mergeRationale()`: rationale 블록 → paper_signal_log.indicator_snapshot 병합
- `PaperHistoryService`: rule_id+symbol+ts+signal JOIN으로 per-trade rationale 조회
- `PaperExecutor.isPriceLimitPending()`: market_bars 전일 종가 대비 ±29.5% 감지

### 프론트엔드
- `trading.ts`: `ConfigStatus`/`RunStatus` 타입; `TradingRule += configStatus/runStatus`; `BacktestTrade += rationaleJson`
- `paper.ts`: `PaperTradeHistoryItem += rationaleJson`
- `paperApi.ts`: `activateRule/deactivateRule/startRule/stopRule` 추가
- `TradeRationaleRow.tsx`: 공유 근거 확장 행 컴포넌트 + `parseRationale()` 헬퍼
- `PaperRulesPage`: config축 전용 재작성
- `TradingRulesPage`: run축 전용 재작성 (ACTIVE 필터)
- `PaperBacktestPage` / `PaperHistoryPage`: 행 클릭 accordion 삽입

---

## v1.3.0 — 모의투자 룰 정의/편집 (2026-06-17)

### Overview
모의투자 실행 엔진(`DESIGN.md` v1.3.0)의 첫 슬라이스. 룰 `definition` JSONB 스키마를 확정하고, 룰을 저장·검증·편집하는 계층을 구현했다. 모의/실거래 룰은 `mode` 컬럼으로 구분되는 동일 테이블을 사용해 향후 승격을 지원한다. 실행 엔진(평가/체결/원장)은 후속 릴리즈에서 진행한다.

### 신규 기능
- 룰 `definition` 스키마 확정: `universe / entry / exit / sizing / constraints`, 지표 MVP(PRICE, SMA, EMA, RSI, VOLUME), 연산자(`>,>=,<,<=,==,crossAbove,crossBelow`)
- 모의 룰 CRUD: 목록·생성·수정·삭제 (`/api/v1/trading/paper/rules`)
- 프론트 룰 에디터: 목록 테이블 + 생성/편집 모달(이름·상태·JSON), JSON/서버 검증 오류 처리

### 백엔드
- V27 `trading_rules` 테이블 (user_id, name, mode, status, definition JSONB, promoted_from, timestamps) + `idx_trading_rules_user_mode`
- `TradingRule` 엔티티 / `TradingRuleRepository`
- `RuleDefinition` 레코드 + `RuleDefinitionValidator`(서버 측 스키마 검증)
- `PaperRuleService` / `PaperRuleController`

### 프론트엔드
- `types/trading.ts` 룰 스키마 타입, `lib/ruleApi.ts` API 클라이언트
- `PaperRulesPage` 룰 에디터 (TanStack Query)

### 추가 (엔진 코어 + 백테스트)
- V28 `paper_*` 테이블(accounts/positions/orders/trades/equity_snapshots)
- 룰 엔진 코어 `trading/engine`: `Indicators`(SMA/EMA/RSI), `RuleEvaluator`(AND/OR·crossAbove/Below·TP/SL), `FillSimulator`(수수료 0.015%), `PaperLedger`, `MarketDataPort`+`YahooMarketDataAdapter`
- 백테스트 API `POST /api/v1/trading/paper/backtest` (`BacktestService`/`BacktestController`)
- 프론트 `PaperBacktestPage`: 룰·기간·초기자본 선택 → 수익률/MDD/승률/거래목록

### 추가 (시세 적재 계층)
- Yahoo 클라이언트 OHLCV/분봉 파싱 확장 (`fetchDailyOhlcv`, `fetchIntraday`) — 거래량 포함
- V29 `market_bars`(일봉)/`market_bars_intraday`(분봉) 테이블
- `MarketDataIngestionService`: 룰 유니버스 종목만 수집·upsert
- `DbMarketDataAdapter`(@Primary): 엔진은 DB에서 시세 조회, 비어있으면 즉석 적재(self-healing)
- 내부 적재 엔드포인트 `POST /internal/market/ingest?interval=EOD|MINUTE` (공유 시크릿 헤더 `X-Internal-Token`)
- **운영 설정**: 환경변수 `GRAPHIFY_INTERNAL_TOKEN` + Cloud Scheduler 2개(EOD 일1회 / MINUTE 장중)

---

## v1.2.0 — 모의투자(Paper Trading) 모드 (2026-06-17)

### Overview
트레이딩 페이지에 모의투자/실거래 모드를 도입. 사이드바 하단 토글로 모드를 전환하며, 모드 상태는 유저별로 백엔드에 영속된다. 모드에 따라 사이드바 메뉴가 전환된다. (설계 문서: `DESIGN.md` v1.2.0)

### 신규 기능
- **모의투자 토글** — 트레이딩 사이드바 하단(데스크탑/모바일 공통). PAPER↔LIVE 전환, 낙관적 업데이트 + 실패 시 롤백
- **실거래 전환 확인 모달** — PAPER→LIVE 전환 시 실자금 경고 확인 절차 (LIVE→PAPER는 즉시 적용)
- **모드별 메뉴 전환**
  - 실거래(LIVE): 대시보드 · 거래 이력 · 현재 룰 · 룰 수정 · 동작 모니터링
  - 모의투자(PAPER): 모의 대시보드 · 모의 거래 이력 · 모의 룰 설정 · 백테스트 · 모의 성과 리포트
- **모드 가드** — 반대 모드 경로 딥링크 접근 시 현재 모드 기본 페이지로 리다이렉트
- **기본값 PAPER** — 기존 유저 포함 전원 안전 모드로 초기화

### 백엔드
- **V26 마이그레이션**: `users.trading_mode VARCHAR(8) DEFAULT 'PAPER'`
- `User.tradingMode` 필드, `/api/v1/users/me` 응답에 `tradingMode` 추가
- 신규 `TradingController`: `GET /api/v1/trading/settings`, `PUT /api/v1/trading/mode`
- `TradingSettingsService`: 권한 검증(`trading_enabled=false`→403), 모드 검증(PAPER/LIVE 외→400)

### 프론트엔드
- `tradingStore`에 `mode`/`setMode` 추가, 진입 시 서버 동기화
- `lib/tradingApi.ts`, `components/trading/PaperTradingToggle.tsx`, `components/trading/ModeGuard.tsx` 신규
- `pages/trading/paper/*` 5종 플레이스홀더, `TradingLayout` 메뉴 분기 + `SidebarFooter` 추출

### 미구현 (다음 단계)
- 모의 체결 엔진 / 가상 잔고 정산, 백테스트 연산 엔진
- 모의 룰 vs 실거래 룰 분리 방식 결정 (DESIGN.md 8절 미결정 사항)

---

## v1.1.0 — 자동매매 트레이딩 봇 Phase 1 (2026-06-15)

### Overview
주식 정보 서비스 Graphify에 자동매매 트레이딩 봇 기능을 위한 기반 인프라 및 UI를 추가한 릴리즈.
실제 매매 로직은 Phase 2 이후 연동 예정이며, 이번 릴리즈는 접근 제어 · 레이아웃 · Agent 채팅 UI · 관리자 관리 기능에 집중함.

---

### 신규 기능

#### 🔐 트레이딩 접근 제어 (Easter Egg `/gg`)
- 메인 검색바에 `/gg` 입력 시 트레이딩 페이지 진입 가능
- 진입 시 `/api/v1/users/me` 실시간 호출로 `tradingEnabled` 권한 검증 (로컬 캐시 미사용)
- 권한 없는 유저는 진입 불가

#### 🌑 트레이딩 레이아웃 (다크 모드)
- `/trading/*` 경로 전용 사이드바 레이아웃 (`TradingLayout.tsx`)
- 진입 시 다크 모드 자동 활성화 (`tradingStore`)
- 사이드바 메뉴: DDS Agent · 대시보드 · 거래 이력 · 현재 룰 · 룰 수정 · 동작 모니터링

#### 💬 DDS Agent 채팅 페이지 (`/trading`)
- 트레이딩 기본 페이지로 AI Agent 채팅 UI 제공
- 말풍선 UI: 사용자(에메랄드) / 어시스턴트(gray-800) 구분
- 타이핑 인디케이터 (3-dot bounce 애니메이션)
- Enter 전송 / Shift+Enter 줄바꿈
- 자동 스크롤, textarea 자동 높이 조절
- 웰컴 메시지 및 Agent 백엔드 연동 TODO 처리
- 추후 실제 Agent API 연동 시 `handleSubmit` 교체 예정

#### 👑 관리자 헤더 링크
- 관리자 계정 로그인 시 우측 상단에 "관리자 페이지" 링크 노출 (`role === "admin"` 조건)

#### 👥 관리자 유저 관리 (`/admin/users`)
- 전체 유저 목록 테이블 (이름 · 이메일 · 역할 · 가입일 · 트레이딩 접근)
- 트레이딩 접근 토글 스위치 (즉시 PUT API 호출)
- "+ 유저 추가" 모달: 이메일 · 이름 · 비밀번호 · 역할(USER/ADMIN) 입력 후 생성

---

### 백엔드 변경사항

#### DB 스키마
- `V25__trading_access.sql`: `users` 테이블에 `trading_enabled BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 추가

#### 엔티티 / DTO
- `User.java`: `tradingEnabled` 필드 추가
- `UserDto`, `UserMeDto`: `tradingEnabled` 응답 포함
- `AdminUserDto`, `TradingAccessRequest`, `AdminCreateUserRequest` 신규 추가

#### API 엔드포인트
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/admin/users` | 전체 유저 목록 조회 |
| POST | `/api/v1/admin/users` | 신규 유저 생성 |
| PUT | `/api/v1/admin/users/{id}/trading-access` | 트레이딩 접근 권한 토글 |

---

### 프론트엔드 변경사항

| 파일 | 변경 내용 |
|------|----------|
| `router/index.tsx` | `/trading/*`, `/admin/users` 라우트 추가 |
| `stores/tradingStore.ts` | 다크 모드 Zustand 스토어 신규 |
| `layouts/TradingLayout.tsx` | 트레이딩 전용 사이드바 레이아웃 신규 |
| `pages/trading/TradingChatPage.tsx` | DDS Agent 채팅 UI 신규 |
| `pages/trading/Trading*.tsx` | 대시보드 · 이력 · 룰 · 모니터 플레이스홀더 신규 |
| `pages/AdminUsersPage.tsx` | 유저 관리 페이지 신규 |
| `lib/adminApi.ts` | `fetchAdminUsers`, `updateTradingAccess`, `createAdminUser` 추가 |
| `types/admin.ts` | `AdminUser` 인터페이스 추가 |
| `types/auth.ts` | `tradingEnabled` 필드 추가 |
| `types/user.ts` | `tradingEnabled` 필드 추가 |
| `components/shared/GlobalSearchBar.tsx` | `/gg` 이스터에그 + 실시간 권한 체크 |
| `components/shared/AppHeaderGuest.tsx` | 관리자 페이지 링크 추가 |
| `layouts/AdminLayout.tsx` | "유저 관리" 탭 추가 |

---

### 인프라 / 설정

- `application-prod.yml`: Flyway 전용 datasource 환경변수 분리 지원
  - `FLYWAY_DATASOURCE_URL` / `USERNAME` / `PASSWORD` 별도 지정 가능
  - 미설정 시 `SPRING_DATASOURCE_URL` fallback

---

### 다음 단계 (Phase 2 예정)
- DDS Agent 백엔드 API 연동 (트레이딩 상태 조회 · 거래 이력 요약 · 룰 설명 · 리포팅)
- 토스증권 API 연동
- 실제 거래 룰 편집 UI 구현
- 동작 모니터링 실시간 대시보드

---

## v1.0.0 — 초기 서비스 출시

- 주식 정보 조회 (DART · 네이버금융 · Yahoo Finance · KRX)
- AI 분석 리포트 생성 (OpenAI)
- 기업 검색 · 분석 이력 · 관심 기업 기능
- JWT 인증 · 회원가입 · 로그인
- Vercel(프론트) + Cloud Run(백엔드) + Supabase(DB) 운영 환경 구성
