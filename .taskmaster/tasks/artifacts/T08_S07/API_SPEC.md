# API 명세 — T08 / S07 기업 상세

> **우선순위**: P0  
> **구현 화면**: S07  
> **레이어**: Frontend, Backend, DB  
> **CR**: CR_003, CR_004 — `.harness/requests/`, `.harness/cr_registry.md`

---

## 엔드포인트

| Method | Path | 설명 | CR |
|--------|------|------|-----|
| GET | `/api/v1/companies/{id}` | 상세 + `dartProfile` | CR_003 |
| GET | `/api/v1/companies/{id}/insights` | `agentInsight` + `signals` | CR_003 |
| POST | `/api/v1/companies/{id}/sync` | DART·재무·공시·뉴스 수집 | CR_003 |
| POST | `/api/v1/companies/{id}/insights/generate` | 인사이트 + Agent 신호 생성 | CR_003 |
| GET | `/api/v1/companies/{id}/market-technical` | 시세·MA·RSI·추세 | CR_004 |

`permitAll`: 위 경로 모두 게스트 조회 가능 (`SecurityConfig`).

---

## GET `/companies/{id}` — `CompanyDetailDto`

| 필드 | 설명 |
|------|------|
| `dartProfile` | DART 기업개황, 재무, 공시, 뉴스 |
| `needsSync` | DART/데이터 갱신 필요 |
| `ticker`, `market` | 시세 API 심볼 매핑용 |

---

## POST `/companies/{id}/sync`

**DART 수집**

| 데이터 | API | 스냅샷 컬럼 |
|--------|-----|-------------|
| 기업개황 | `company.json` | `profile_json` |
| 공시 | `list.json` (6개월) | `disclosures_json` |
| 재무 | `fnlttSinglAcnt` | `financials_json` |
| 뉴스 | NewsAPI / RSS / `market_news` | `news_json` |

---

## GET `/companies/{id}/insights` — `CompanyInsightsDto`

| 필드 | 설명 |
|------|------|
| `agentInsight` | `{ content, modelLabel, status, generatedAt }` |
| `signals` | Agent `RISK_DETECTION` 생성 (`company_agent_signals`) |
| `cards` | 레거시; Agent 있으면 빈 배열 |

---

## POST `/companies/{id}/insights/generate`

1. `INSIGHT_SUMMARY` — 관리자 `task_template` + 치환:
   - `{{company_name}}`, `{{context}}`, `{{market_technical}}`
2. `RISK_DETECTION` — 동일 + `{{signal_json_instruction}}` (없으면 서버 JSON 지침 부착)

`{{market_technical}}`: Yahoo 일봉 기반 마크다운 (상장·조회 성공 시). 티커 없음/Yahoo 실패 시 빈 문자열, 템플릿에 토큰 없으면 블록 미부착.

---

## GET `/companies/{id}/market-technical`

Response `data`:

| 필드 | 타입 | 설명 |
|------|------|------|
| `yahooSymbol` | string | 예 `005930.KS` |
| `price` | number? | 장중 현재가 (우선: Naver KRX, 폴백: Yahoo) |
| `changePercent` | number? | 전 거래일 종가 대비 % |
| `previousClose` | number? | 전 거래일 종가 |
| `quoteTime` | datetime | 시세 기준 시각 (UTC ISO) |
| `tradingDate` | string | 거래일 `yyyy-MM-dd` (KST 등 거래소 TZ) |
| `priceKind` | `LIVE` \| `TODAY_CLOSE` \| `LAST_CLOSE` | 표시 구분 |
| `priceLabel` | string | `현재가` / `금일 종가` / `최근 종가 (M/D)` |
| `priceSource` | string? | `NAVER` \| `YAHOO` |
| `historySource` | string? | `YAHOO` (향후 `KRX`) |
| `asOf` | datetime | API 조회 시각 |
| `ma5` … `ma240` | number? | 단순이동평균 |
| `rsi14` | number? | RSI 14 |
| `maAlignment` | `BULLISH_ALIGN` \| `BEARISH_ALIGN` \| `MIXED` | 60·120·240 정/역배열 |
| `shortTermRise5` | boolean | 현재가 > MA5 |
| `shortTermRise20` | boolean | 레거시 값 유지 (UI는 현재가 vs MA20으로 판별) |

---

## 오류 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| ERR_COMPANY_001 | 404 | 기업 없음 |
| ERR_COMPANY_002 | 400 | DART 연동 아님 |
| ERR_COMPANY_003 | 502 | DART company.json 실패 |
| ERR_COMPANY_004 | 500 | 스냅샷 JSON 해석 실패 |
| ERR_COMPANY_005 | 400 | 스냅샷 없음 (generate 전 sync) |
| ERR_COMPANY_006 | 400 | 종목코드 없음 (market-technical) |
| ERR_COMPANY_007 | 502 | Yahoo 시세 조회 실패 |
| ERR_ADMIN_PROMPT_001 | 404 | Agent 프롬프트 미설정 |

---

## Bootstrap

`GET /api/v1/bootstrap/integrations` — `dart`, `newsApi`, `openAi`, `yahooMarket`
