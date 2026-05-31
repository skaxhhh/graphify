# CR_004: S07 시장 기술 지표 (Yahoo Finance)

## 원하는 것

- 상장 기업 상세(S07)에 **금일 주가·등락률**, **일봉 RSI(14)**, **이동평균(5/20/60/120/240)** 및 추세 판별 표시
  - **정배열**: MA60 > MA120 > MA240 (상승 추세)
  - **역배열**: MA60 < MA120 < MA240 (하락 추세)
  - **초단기 상승**: 종가 > MA5
  - **단기 상승**: 종가 > MA20 이고 MA5 > MA20
- 데이터 소스: **Yahoo Finance Chart API** (비공식, API 키 없음) + 서버 측 지표 계산

## 이유

- PRD의 주가 API는 미구현; 사용자 투자 관점(추세·RSI·당일 시세) 반영 필요
- DART는 재무·공시만 제공, 시세·기술적 지표는 별도 연동 필요
- KIS/KRX 대비 PoC 구현 속도가 가장 빠름

## 관련 화면 / 기능

- 화면: **S07** `/companies/{id}`
- 관련 태스크: T08_S07 (기업 상세)
- 관련 파일: `CompanyController`, `CompanyDetailPage`, `company/market/*`

## 의존 CR

- CR_003 (DART sync, `ticker`/`market`) — 완료 가정

## 명확화된 요구사항

| 항목 | 규칙 |
|------|------|
| 심볼 | KOSPI → `{ticker}.KS`, KOSDAQ → `{ticker}.KQ`, 미상장/무티커 → 404 |
| 일봉 | `interval=1d`, `range=2y` (240거래일 확보) |
| RSI | 14일 Wilder RSI, 최신 일봉 기준 |
| 등락률 | 전일 종가 대비 `(금일가 - 전일종가) / 전일종가 * 100` |
| 정배열/역배열 | 그 외 → `MIXED` |

## 범위 판단

- **In**: GET API, 지표 계산, S07 UI 카드, bootstrap integrations 표시
- **Out**: DB 영구 저장, TradingView 위젯, 실시간 틱
- **추가 완료**: Agent `{{market_technical}}` (T08 `PROMPT_VARIABLES.md`)

## 확정 스코프

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/companies/{id}/market-technical` | 시세·MA·RSI·추세 판별 |

## 롤백 정보

- 구현 완료 (커밋 전)

## 변경 이력

- 2026-05-21 PHASE 1·3 — CR 작성 및 구현 완료
  - Backend: `YahooFinanceChartClient`, `TechnicalIndicatorCalculator`, `GET .../market-technical`
  - Frontend: `CompanyTechnicalPanel` on S07 right column
  - Artifacts: T08_S07 `API_SPEC.md` (market-technical 섹션)
  - Agent: `{{market_technical}}` 치환·자동 부착 (`MarketTechnicalContextFormatter`)
