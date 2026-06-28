# PLAN — Trading 메뉴 UI 개편 기능 요구사항 정의서

> **목적**: 현재 구현된 Trading 관련 메뉴/화면의 기능을 빠짐없이 인벤토리화하고, 향후 구현 예정 기능까지 포함해 **새 wireframe 설계의 단일 입력(source of truth)**으로 사용한다. 본 문서는 화면을 재배치하기 위한 "기능 카탈로그"이며, 특정 레이아웃을 규정하지 않는다.
>
> **작성 근거**: `.planning/ROADMAP.md`, `.planning/REQUIREMENTS.md`, `.planning/STATE.md`(GSD 산출물) + 실제 구현 코드(`frontend/src/pages/trading/**`, `frontend/src/layouts/TradingLayout.tsx`, 백엔드 컨트롤러).
> **현황 기준일**: 2026-06-27 / 마일스톤 v1.0 진행률 98% (Phase 0~6.7 완료, Phase 7·8 미착수).
>
> **🔒 개편 범위 (확정): 전체 포함.** 아래 모두 새 wireframe 설계 대상이다 — ① **PAPER + LIVE 두 모드의 전체 IA 재설계**(공통 메뉴 포함), ② **DDS Agent 챗 실연동**까지 포함(목업 유지가 아니라 실제 화면으로 설계), ③ **Phase 7(TradingView) · Phase 8(실투자/LIVE 승격) 미래 기능의 자리(슬롯) 선반영**, ④ 미완 v1 요구사항(§5.1)과 v2 백로그(§5.4)의 화면 영향까지 고려. 즉 현재 구현(✅) + 스텁(🚧) + 목업(⚠️) + 향후(Phase 7·8·v2)를 **하나의 통합 IA**로 재배치한다.

---

## 1. 핵심 개념 (개편 시 반드시 유지)

### 1.1 두 가지 트레이딩 모드 — PAPER vs LIVE
- 전역 상태 `useTradingStore.mode` (`PAPER` | `LIVE`), 기본값 `PAPER`.
- 사이드바 하단 **토글**(`PaperTradingToggle`)로 전환. PAPER→LIVE 전환 시 **위험 확인 모달**("실제 자금이 사용될 수 있습니다") 필수, LIVE→PAPER는 즉시 적용.
- 진입 시 서버 설정(`GET /api/v1/trading/settings`)과 동기화, 전환 시 `PUT /api/v1/trading/mode` 저장(낙관적 업데이트 + 실패 롤백).
- **메뉴 자체가 모드에 따라 달라진다**: PAPER 모드 → 모의 메뉴 6종, LIVE 모드 → 실거래 메뉴 5종. 공통 메뉴 2종은 항상 표시.
- `ModeGuard`: 현재 모드와 다른 모드의 경로로 딥링크 접근 시 해당 모드 기본 페이지로 리다이렉트(PAPER→`/trading/paper/dashboard`, LIVE→`/trading/dashboard`).
- **개편 시사점**: 모드는 IA의 1차 분기 축이다. 새 wireframe도 "현재 어떤 모드인가"를 항상 명확히 보여줘야 하며, 모드 전환의 위험성(LIVE)을 시각적으로 강조해야 한다.

### 1.2 룰의 2축 상태 모델 (Phase 6.5에서 분리, 절대 합치지 말 것)
룰은 **두 개의 독립 축**으로 관리된다:
- **설정 축 (configStatus)**: `DRAFT` ↔ `ACTIVE` — 룰 정의를 편집/확정하는 단계. "전략 설정" 화면 담당.
- **실행 축 (runStatus)**: `STOPPED` ↔ `RUNNING` — ACTIVE 룰을 실제 PAPER_LIVE 평가 루프에 투입하는지. "전략 운영" 화면 담당.
- 규칙: `ACTIVE` + `RUNNING` 룰은 편집 불가, `RUNNING` 룰은 DRAFT로 하향 불가(먼저 중지해야 함).
- (레거시 `RuleStatus`: DRAFT/ACTIVE/PAUSED/BACKTESTED/PAPER_LIVE/LIVE 도 타입에 존재하나, UI 표시는 2축 모델 기준.)
- **개편 시사점**: "설정"과 "운영"의 역할 경계가 명확히 분리되어 있다. 두 화면이 같은 상태를 중복 노출하지 않는다는 원칙을 유지해야 한다.

### 1.3 디자인 토큰 / 공통 컴포넌트 (프로젝트 규칙)
- 디자인 SoT = `frontend/src/components/shared/`(프리미티브) + `tailwind.config.js`(토큰). 새 화면은 raw `<button>` 대신 `shared/` 우선, 하드코딩 hex 금지.
- 현재 trading 화면은 대부분 raw Tailwind 클래스를 직접 사용 중(`bg-gray-900/50`, `text-emerald-400` 등 다크 테마) → **개편 시 shared 컴포넌트로 정렬할 기회**.

---

## 2. 현재 메뉴/IA 구조 (As-Is)

사이드바: `graphify 트레이딩 봇` 헤더 + 네비 + 하단(모의투자 토글 / "메인으로"). 헤더 BETA 배지. 모바일은 드로어.
정의 위치: `frontend/src/layouts/TradingLayout.tsx:13-33`.

| 그룹 | 메뉴 라벨 | 경로 | 표시 조건 | 구현 상태 |
|------|-----------|------|-----------|-----------|
| **공통** | DDS Agent | `/trading` (index) | 항상 | ⚠️ 목업(백엔드 미연동) |
| **공통** | 토스 설정 | `/trading/settings` | 항상 | ✅ 완료 |
| **LIVE** | 대시보드 | `/trading/dashboard` | LIVE 모드 | 🚧 스텁("준비 중") |
| **LIVE** | 거래 이력 | `/trading/history` | LIVE 모드 | 🚧 스텁("준비 중") |
| **LIVE** | 현재 룰 | `/trading/rules` | LIVE 모드 | ✅ 완료(전략 운영, 모의와 공유) |
| **LIVE** | 룰 수정 | `/trading/rules/edit` | LIVE 모드 | ✅ 완료(룰 빌더) |
| **LIVE** | 동작 모니터링 | `/trading/monitor` | LIVE 모드 | ✅ 완료(모의 데이터) |
| **PAPER** | 모의 대시보드 | `/trading/paper/dashboard` | PAPER 모드 | ✅ 완료 |
| **PAPER** | 모의 거래 이력 | `/trading/paper/history` | PAPER 모드 | ✅ 완료 |
| **PAPER** | 모의 룰 설정 | `/trading/paper/rules` | PAPER 모드 | ✅ 완료(전략 설정) |
| **PAPER** | 룰 라이프사이클 | `/trading/paper/rules-lifecycle` | PAPER 모드 | ✅ 완료(전략 운영) |
| **PAPER** | 백테스트 | `/trading/paper/backtest` | PAPER 모드 | ✅ 완료 |
| **PAPER** | 모의 성과 리포트 | `/trading/paper/report` | PAPER 모드 | ✅ 완료 |
| (숨김 경로) | 새 룰 / 룰 편집 | `/trading/paper/rules/new`, `/trading/paper/rules/edit/:id` | PAPER 모드 | ✅ 완료(룰 빌더) |

**개편 시 주목할 구조적 이슈** (재배치 판단 자료):
1. **LIVE 메뉴 vs 컴포넌트 불일치**: LIVE "현재 룰/룰 수정/동작 모니터링"은 실제로는 PAPER용 컴포넌트(`TradingRulesPage`/`TradingRulesEditPage`/`TradingMonitorPage`)를 그대로 렌더한다. LIVE 전용 대시보드·거래이력은 스텁이다 → LIVE 영역은 사실상 미완성.
2. **명칭 혼선**: `TradingRulesPage`(컴포넌트명) = "전략 운영" 화면이지만, LIVE 메뉴 라벨은 "현재 룰", PAPER 메뉴 라벨은 "룰 라이프사이클"로 같은 화면을 다르게 부른다.
3. **"전략 설정"(`paper/rules`)** ↔ **"전략 운영"(`paper/rules-lifecycle`)** 2화면 분리가 메뉴 라벨("모의 룰 설정"/"룰 라이프사이클")에서 직관적으로 안 드러난다.
4. **DDS Agent(챗)** 가 모드와 무관한 공통 진입점인데 index 라우트를 차지 → 진입 첫 화면.

---

## 3. 화면별 기능 요구사항 (현재 구현 — 재배치 대상 카탈로그)

각 항목: 목적 / 주요 UI 섹션 / 데이터·API / 상태(빈·로딩·에러) / 비고. 요구사항 ID는 `REQUIREMENTS.md` 추적성과 연결.

### 3.1 DDS Agent 챗 — `/trading` `TradingChatPage` ⚠️ 목업
- **목적**: 봇 상태 조회·거래이력 요약·룰 설명·리포팅을 대화형으로 제공하는 에이전트 인터페이스.
- **UI**: 헤더(아바타 "D" + "대기 중" 상태점), 메시지 버블 영역(user/assistant), 타이핑 인디케이터, 자동 높이 textarea 입력(Enter 전송/Shift+Enter 줄바꿈), 전송 버튼.
- **데이터/API**: **없음 — 백엔드 미연동.** `setTimeout`으로 고정 응답("Agent 백엔드 연동 준비 중") 반환. `// TODO: 실제 Agent API 연동 시 교체`.
- **상태**: 환영 메시지 1개로 시작. 빈/에러 상태 미구현.
- **개편 메모**: 향후 실제 Agent API 연동 필요(미정 범위). 위치(진입 첫 화면 유지 여부)는 재배치 후보.

### 3.2 토스 설정 — `/trading/settings` `TossSettingsPage` ✅ (TOSS-01·02)
- **목적**: 토스증권 Open API 자격증명 등록 + 토큰 상태 관리.
- **UI**: 연결 상태 카드(배지: 미설정/설정됨·토큰 유효/설정됨·토큰 만료, 토큰 만료시각, "토큰 수동 갱신" 버튼), 자격증명 폼(Client ID/Secret — 둘 다 `type=password`), 저장 버튼, 안내 문구(AES-256-GCM 암호화 / 발급 경로).
- **데이터/API**: `GET .../toss/credentials/status`, `POST .../toss/credentials`, `POST .../toss/token/refresh`.
- **상태**: 로딩("확인 중..."), 성공/에러 메시지 인라인.
- **비고**: 공통 메뉴(모드 무관). 모의 대시보드의 실계좌 잔고 미연동 시 이 화면으로 유도.

### 3.3 모의 대시보드 — `/trading/paper/dashboard` `PaperDashboardPage` ✅ (DASH-02·03·04)
- **목적**: 모의 계좌 현황 한눈에.
- **UI**: 통계 카드 4종(총 평가금액+수익률, 가용 현금, 오늘 실현손익, 활성 PAPER_LIVE 룰 개수), 미실현 손익 합계, **보유 포지션 테이블**(종목/수량/평균단가/현재가/평가금액/평가손익/손익률, 손익 색상), **토스 실계좌 잔고**(접이식 — 미연동 시 토스 설정 유도, 연동 시 계좌별 잔고/출금가능).
- **데이터/API**: `GET .../paper/dashboard` (30초 자동 갱신), `GET .../toss/accounts`.
- **상태**: 스켈레톤 로딩(카드 4 + 블록), 에러("불러올 수 없습니다"), 포지션 빈("보유 포지션 없음").
- **갭(DASH-01 Pending)**: 가상 현금 잔고+총평가 명시 요구사항은 통계 카드로 부분 충족.

### 3.4 모의 거래 이력 — `/trading/paper/history` `PaperHistoryPage` ✅ (CHART-07, MON-05 일부)
- **목적**: 체결된 모의 거래 내역 + 매매 근거 + 캔들 차트 검증.
- **UI**: 상단 **캔들 차트 섹션**(`CandleSection` — 선택 거래의 종목/날짜 5분봉, 진입/청산 마커, 선택 행 하이라이트), 거래 테이블(체결시각/종목/구분(매수·매도)/수량/가격/손익), **행 클릭 → 매매 근거 아코디언**(`TradeRationaleRow`: 청산 사유 익절/손절/지표, 조건별 ✓/✗ + 지표값) + 해당일 캔들 전환.
- **데이터/API**: `GET .../paper/history` (30초 갱신), `GET .../paper/backtest/bars?symbol&date`.
- **상태**: 로딩/에러/빈("체결된 모의 거래가 없습니다"). 첫 거래 자동 선택.

### 3.5 모의 룰 설정 (= 전략 설정) — `/trading/paper/rules` `PaperRulesPage` ✅ (RULE-03·06·07, RULE-08)
- **목적**: 룰 CRUD + **설정 축(DRAFT↔ACTIVE)** 관리. **실행 제어는 미노출**(역할 분리).
- **UI**: 제목 "전략 설정" + "+ 새 룰" 버튼, 룰 테이블(이름/설정 상태 배지(DRAFT·ACTIVE)/쿨다운(봉+분 환산)/수정일/관리), 행별 액션: **활성화↔하향**(RUNNING이면 하향 불가), **편집**(ACTIVE+RUNNING이면 불가), **복제**(RULE-06), **삭제**.
- **데이터/API**: `GET/POST/PUT/DELETE .../paper/rules`, `POST .../{id}/activate`, `.../{id}/deactivate`, `.../{id}/copy`.
- **상태**: 로딩/에러/빈("등록된 룰이 없습니다"), 작업 실패 인라인 배너.

### 3.6 룰 라이프사이클 (= 전략 운영) — `/trading/paper/rules-lifecycle` & LIVE `/trading/rules` `TradingRulesPage` ✅ (RULE-02·09)
- **목적**: **ACTIVE 룰만** 표시 + **실행 축(STOPPED↔RUNNING)** 제어. 설정 변경 미노출.
- **UI**: 제목 "전략 운영", 룰 테이블(이름/실행 상태 배지(실행 중·중지됨)/수정일/제어), 행별 액션: **시작↔중지**, **복사**. 실시간 거래대금 랭킹 조회 실패(ERR_LIFECYCLE_005) 시 **종목 직접 선택 모달**(`CompanyPickerModal`) 폴백.
- **데이터/API**: `POST .../{id}/start`(overrideSymbols 옵션), `.../{id}/stop`, `.../{id}/copy`. (레거시: `/promote`, `/pause`, `/resume` 도 백엔드 존재.)
- **상태**: 로딩/에러/빈("ACTIVE 상태인 룰이 없습니다").

### 3.7 룰 빌더 (새 룰 / 편집) — `/trading/paper/rules/new`·`/edit/:id` & LIVE `/trading/rules/edit` `TradingRulesEditPage` ✅ (RULE-06·07)
- **목적**: JSON 직접 입력 없이 폼/드롭다운으로 완전한 `RuleDefinition` 구성. (양방향: 빌더↔JSON 탭 전환, 역직렬화 편집 지원.)
- **UI 섹션** (빌더 탭):
  1. **룰 이름**.
  2. **유니버스**: 라디오 — 종목 코드 직접 입력 / 거래량 상위 N종목(KOSPI). 후자는 topN + 추가 종목(additionalSymbols).
  3. **진입 조건**: 논리(AND/OR) + 조건 행 N개(좌측 지표 PRICE/SMA/EMA/RSI/VOLUME + 기간(SMA·EMA·RSI) / 연산자 `>` `>=` `<` `<=` `==` `crossAbove` `crossBelow` / 우측 값 또는 지표). "+ 조건 추가".
  4. **청산 조건**: 익절% / 손절% + 지표 조건 행 N개(논리 AND/OR).
  5. **사이징**: 타입(현금 cash / 비중 percent / 수량 qty) + 값.
  6. **제약**: 쿨다운(봉) / 최대 포지션(종목당).
  - **JSON 탭**: textarea 직접 편집, 빌더↔JSON 직렬화 양방향(`toDefinition`/`fromDefinition`).
- **데이터/API**: `GET/POST/PUT .../paper/rules(/:id)`.
- **검증**: 이름 필수, 진입 조건 ≥1, 사이징 값 숫자. 저장 후 `paper/rules`로 이동.
- **상태**: 편집 모드 로딩, formError/jsonError 인라인.

### 3.8 백테스트 — `/trading/paper/backtest` `PaperBacktestPage` ✅ (CHART-01·02·03·07)
- **목적**: 저장 룰을 5분봉 인트라데이(09:00–12:00 KST, 최대 60일)로 검증.
- **UI**:
  - **입력 폼**: 룰 선택 / 시작일·종료일 / 시작시각·종료시각 / 초기 자본 + "백테스트 실행".
  - **요약 지표 5종**: 최종 평가액 / 수익률 / 최대 낙폭(MDD) / 승률 / 거래 횟수.
  - **수익 곡선 차트**(`EquityCurveChart`): 드로우다운 음영 오버레이.
  - **캔들 차트 섹션**(`CandleSection`): 룰 지표선 오버레이 + 진입/청산 마커.
  - **고급 통계 3종**: Sharpe / Sortino / Profit Factor(설명 포함).
  - **거래 이력 테이블**: 일시/종목/구분/수량/가격/손익 + 행 클릭 근거 아코디언(`TradeRationaleRow`).
  - **빈 유니버스 폴백**(v1.6.0): 거래대금 데이터 없음(ERR_BACKTEST_UNIVERSE_EMPTY) 시 `CompanyPickerModal`로 종목 직접 선택 재실행.
- **데이터/API**: `POST .../paper/backtest`, `GET .../paper/backtest/bars`.
- **상태**: 실행 중/에러 인라인, 결과 전 빈 화면, 거래 0건("체결된 거래가 없습니다").

### 3.9 모의 성과 리포트 — `/trading/paper/report` `PaperReportPage` ✅ (REPORT-01·02)
- **목적**: 모의(PAPER_LIVE) 누적 운영 성과 종합.
- **UI**: 기간 표시(periodFrom~To / 최근 30일), 수익 곡선 차트(`EquityCurveChart`, 드로우다운 없음), 통계 카드 6종(총 수익률/최대 낙폭/승률/총 거래/Sharpe/Sortino).
- **데이터/API**: `GET .../paper/report`.
- **상태**: 로딩/에러/빈("모의 실행 데이터가 없습니다. PAPER_LIVE 룰 활성화 시 자동 기록").

### 3.10 동작 모니터링 — LIVE `/trading/monitor` `TradingMonitorPage` ✅ (MON-01·02·03·04)
- **목적**: 실시간 봇 상태 + 신호 로그 + 당일 체결 피드.
- **UI**: 상태 행(시장 OPEN/CLOSED 신호등, 스케줄러 최근 실행 시각), **신호 로그 테이블**(최대 50건: 시각/종목/신호 배지(매수·매도·관망)/체결 여부/RSI14/SMA20/현재가), **오늘 체결 내역 테이블**(시각/종목/구분/수량/체결가/손익).
- **데이터/API**: `GET .../paper/monitor` (30초 갱신). 마지막 갱신 시각 표시.
- **상태**: 로딩/에러, 각 테이블 빈("최근 신호 없음"/"오늘 체결 내역 없음").
- **비고**: 메뉴상 LIVE 그룹이나 데이터는 모의(paper/monitor) 기준. 재배치 시 PAPER로 이동 검토 대상.

### 3.11 LIVE 대시보드 / 거래 이력 — `/trading/dashboard`·`/trading/history` 🚧 스텁
- 현재 "준비 중" 플레이스홀더만. Phase 8(실투자)에서 구현 예정. (PAPER 대시보드/이력의 LIVE 대응물.)

---

## 4. 백엔드 능력 맵 (화면 재배치 시 데이터 제약)

| Method | Path | 용도 | 컨트롤러 |
|--------|------|------|----------|
| GET | `/api/v1/trading/settings` | 모드 조회 | TradingController |
| PUT | `/api/v1/trading/mode` | 모드 변경 | TradingController |
| GET | `/api/v1/trading/paper/rules` | 룰 목록 | PaperRuleController |
| GET | `/api/v1/trading/paper/rules/{id}` | 룰 단건 | PaperRuleController |
| POST | `/api/v1/trading/paper/rules` | 룰 생성 | PaperRuleController |
| PUT | `/api/v1/trading/paper/rules/{id}` | 룰 수정 | PaperRuleController |
| DELETE | `/api/v1/trading/paper/rules/{id}` | 룰 삭제 | PaperRuleController |
| POST | `.../paper/rules/{id}/activate` | DRAFT→ACTIVE | PaperLifecycleController |
| POST | `.../paper/rules/{id}/deactivate` | ACTIVE→DRAFT | PaperLifecycleController |
| POST | `.../paper/rules/{id}/start` | 실행 시작(overrideSymbols) | PaperLifecycleController |
| POST | `.../paper/rules/{id}/stop` | 실행 중지 | PaperLifecycleController |
| POST | `.../paper/rules/{id}/copy` | 복제 | PaperLifecycleController |
| POST | `.../paper/rules/{id}/promote` | (레거시) 승격 | PaperLifecycleController |
| POST | `.../paper/rules/{id}/pause`·`/resume` | (레거시) 일시정지/재개 | PaperLifecycleController |
| POST | `/api/v1/trading/paper/backtest` | 백테스트 실행 | BacktestController |
| GET | `/api/v1/trading/paper/backtest/bars` | 5분봉 캔들 | BacktestController |
| GET | `/api/v1/trading/paper/dashboard` | 대시보드 | PaperDashboardController |
| GET | `/api/v1/trading/paper/history` | 거래 이력 | PaperHistoryController |
| GET | `/api/v1/trading/paper/monitor` | 모니터 | PaperMonitorController |
| GET | `/api/v1/trading/paper/report` | 성과 리포트 | PaperReportController |
| GET/POST | `.../toss/credentials(/status)` | 자격증명 | TossCredentialController |
| POST | `.../toss/token/refresh` | 토큰 갱신 | TossCredentialController |
| GET | `.../toss/accounts` | 실계좌 잔고 | TossAccountController |

부가 엔진(화면 없음, 백그라운드): `LiveEvaluationService`(스케줄러 기반 PAPER_LIVE 평가+가상 체결), `VolumeRankingProvider`/`VolumeRankRefresher`(거래량 상위 동적 재선정), `MarketDataIngestionService`(5분봉 수집, ShedLock), KRX 공휴일 가드.

---

## 5. 향후 구현 예정 기능 (새 wireframe에 자리 확보 필요)

### 5.1 미완 v1 요구사항 (현 마일스톤 잔여)
| ID | 내용 | 화면 영향 |
|----|------|-----------|
| **LIVE-06** | 평가 주기마다 가상 평가금액 스냅샷 저장 | (백엔드) 리포트/대시보드 정확도 ↑ |
| **DASH-01** | 가상 현금 잔고+총 평가금액 명시 | 모의 대시보드 보강 |
| **RULE-04** | PAPER_LIVE→LIVE 승격(토스 인증 + 5거래일 운영 게이트) | 전략 운영/룰 상세에 승격 버튼+게이트 UI |
| **RULE-05** | LIVE 룰 편집 불가, DRAFT 복사본으로만 | 룰 빌더 편집 가드 |
| **TOSS-04** | LIVE 룰 → 토스 실주문 발행 | LIVE 대시보드/거래이력 실데이터화 |
| **TOSS-05** | 토스 실시간 시세 LIVE 평가 사용 | (백엔드) |
| **TOSS-06** | API 5회 실패 시 서킷 브레이커 + 평가 중단 | 모니터/대시보드 경고 표시 |

### 5.2 Phase 7 — TradingView Webhook 연동 (미착수)
- **새 룰 타입 `TRADINGVIEW`**: 룰 목록에 **TV 배지**로 구분. 룰 빌더에 TradingView 연동 메모 / **webhook URL 복사** / 파싱 방식 선택 UI 추가.
- `POST /api/webhook/tv/{token}`: 룰별 고유 토큰, 즉시 큐잉 후 100ms 내 200 응답(비동기 처리).
- 신호 파서: JSON(`{symbol,action}`) 우선 → 실패 시 LLM(Claude) fallback → BUY/SELL/UNKNOWN. UNKNOWN은 미체결+로그.
- **종목 선택**: 사전 등록 KOSPI 시총 상위 풀(`tv_supported_symbols`)에서만 선택. 룰 종목 리스트 외 webhook은 무시.
- TRADINGVIEW 룰은 자체 백테스트 대신 **"TradingView에서 보기" 링크** 제공.
- **wireframe 영향**: 룰 빌더에 "신호 소스(CUSTOM/TradingView)" 분기, 룰 목록 배지, webhook 관리 영역 필요.

### 5.3 Phase 8 — 실투자 주문 실행 & LIVE 승격 (미착수)
- LIVE 대시보드/거래이력 **실데이터 구현**(현 스텁 대체).
- 실주문 발행, 실시간 시세, 서킷 브레이커(연속 5회 실패 → 평가 중단+경고).
- PAPER_LIVE→LIVE **승격 게이트 UI/API**(토스 인증 완료 + 최소 5거래일 운영).
- **wireframe 영향**: LIVE 모드 전 화면이 PAPER 대응물과 짝을 이루도록 설계. 서킷 브레이커/주문 실패 경고 배너 자리 필요.

### 5.4 v2 백로그 (장기, 자리만 고려)
- 백테스트: 월별 수익 히트맵(CHART-04), 벤치마크 비교선(CHART-05), 수익곡선 매수/매도 마커(CHART-06).
- 모의: 슬리피지+거래세 모델링(LIVE-07), evalTiming EOD/INTRADAY 선택(LIVE-08), 대시보드 미니 스파크라인(DASH-05).
- 리포트: 거래내역 페이지네이션(REPORT-03), 월별 수익률(REPORT-04), 룰별 성과 비교(REPORT-05).
- 모니터: 상한가/하한가 PRICE_LIMIT_PENDING 처리(MON-05, 일부 반영).
- **Out of scope**(명시적 제외): 수동 주문 UI, WebSocket 푸시, 이메일/푸시 알림, 모바일 앱, 타 증권사, 암호화폐, Walk-forward/Monte Carlo, PDF 내보내기.

---

## 6. 개편(재배치)을 위한 종합 권고

새 wireframe 작성 시 검토할 **현재 구조의 개선 포인트** (결정은 디자인 단계에서):

1. **모드 축을 IA 최상위로 명시** — PAPER/LIVE 토글의 위치·강조, 현재 모드 영구 표시. LIVE 진입 위험 강조 유지.
2. **"설정 vs 운영" 2화면 분리는 유지하되 명명 통일** — 메뉴 라벨("모의 룰 설정"/"룰 라이프사이클")과 화면 제목("전략 설정"/"전략 운영")이 어긋남. 한 쌍의 일관된 용어로 정리.
3. **LIVE 메뉴 정합화** — LIVE "현재 룰/룰 수정/동작 모니터링"이 PAPER 컴포넌트를 재사용하는 현 구조를 의도대로(공유 vs LIVE 전용) 정리. LIVE 대시보드/거래이력 스텁은 Phase 8까지 "준비 중" 명시 or 숨김.
4. **모니터링의 모드 귀속 재고** — `TradingMonitorPage`가 LIVE 그룹이나 모의 데이터 표시. PAPER 그룹 이동 또는 모드별 데이터 분기 검토.
5. **DDS Agent(챗) 위치** — 모드 무관 공통 + 미연동 목업. 진입 첫 화면 유지 여부 / 향후 Agent 연동 범위 결정 필요.
6. **신규 기능 슬롯 확보** — 룰 목록의 TV 배지·신호 소스 분기(Phase 7), 승격 게이트·서킷 브레이커 경고(Phase 8)가 들어갈 공간을 미리 설계.
7. **shared 컴포넌트/토큰 정렬** — 현 trading 화면의 raw Tailwind를 `shared/` 프리미티브 + 토큰으로 수렴(프로젝트 디자인 규칙).

---

## 7. 개편 범위 확정 (전체 포함)

본 개편은 **전체를 포함한다**. 아래 항목 모두 새 wireframe 설계 대상이다 — 별도 차수로 미루는 항목 없음.

| 질문 | 확정 |
|------|------|
| 개편 범위 | **전체 IA 재설계** — PAPER + LIVE 두 모드 + 공통 메뉴 전부 |
| DDS Agent 챗 | **포함** — 목업 유지가 아니라 실연동 전제 화면으로 설계(필요 시 단계적 연동) |
| Phase 7/8 미래 기능 | **선반영** — TradingView 신호 소스·TV 배지·webhook 관리(§5.2), LIVE 승격 게이트·서킷 브레이커 경고·실주문 화면(§5.3)의 자리를 wireframe에 미리 확보 |
| 미완 v1(§5.1) / v2(§5.4) | 화면 영향 있는 항목 모두 슬롯 고려 |
| 모드 토글 위치/형태 | wireframe에서 결정(현 사이드바 하단 유지 또는 헤더 승격 모두 후보) — 단 §6-①(모드 영구 표시·LIVE 위험 강조)은 충족 |

→ 즉 **현재 구현(✅) + 스텁(🚧) + 목업(⚠️) + 향후(Phase 7·8·v2)** 를 하나의 통합 IA로 재배치하는 것이 이 wireframe의 목표다. §6의 권고 7개는 모두 이번 개편의 적용 대상이다.

---

*근거 파일: `frontend/src/layouts/TradingLayout.tsx`, `frontend/src/router/index.tsx`, `frontend/src/pages/trading/**`, `frontend/src/components/trading/**`, `frontend/src/types/trading.ts`, `backend/src/main/java/com/graphify/trading/**`, `.planning/{ROADMAP,REQUIREMENTS,STATE}.md`. 현황 기준일 2026-06-27.*
