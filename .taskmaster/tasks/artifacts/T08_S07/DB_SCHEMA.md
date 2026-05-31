# DB 스키마 — T08 / S07 기업 상세

> S07 관련 Flyway 마이그레이션 요약. 신규 CR 시 ALTER는 다음 번호(`V25__…`)로 추가.

---

## `company_dart_snapshots` (V22, V23)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `company_id` | BIGINT PK | `companies.id` |
| `corp_code` | VARCHAR(8) | DART 고유번호 |
| `profile_json` | JSONB | `company.json` |
| `disclosures_json` | JSONB | 공시 목록 |
| `financials_json` | JSONB | V23 — `fnlttSinglAcnt` 요약 |
| `news_json` | JSONB | V23 — 관련 뉴스 |
| `collected_at` | TIMESTAMPTZ | 수집 시각 |

---

## `company_agent_insights` (V22)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGSERIAL PK | |
| `company_id` | BIGINT UNIQUE | |
| `task_type` | VARCHAR(64) | `INSIGHT_SUMMARY` |
| `content` | TEXT | Agent 본문 |
| `model_label` | VARCHAR(128) | 모델명 / `mock-dev` |
| `status` | VARCHAR(32) | `READY` 등 |
| `generated_at` | TIMESTAMPTZ | |

---

## `company_agent_signals` (V24)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGSERIAL PK | |
| `company_id` | BIGINT | |
| `kind` | VARCHAR(32) | `RISK` / `OPPORTUNITY` |
| `label` | VARCHAR(255) | 신호 제목 |
| `sources_json` | JSONB | 근거 유형 배열 |
| `sort_order` | INT | |

DART 스냅샷 있으면 조회 시 **Agent 신호 우선** (레거시 `company_signals` 시드는 폴백).

---

## `companies` (기존 + V21)

| 컬럼 | 설명 |
|------|------|
| `ticker` | 종목코드 → Yahoo `.KS` / `.KQ` |
| `market` | KOSPI / KOSDAQ |
| `sync_status`, `detail_synced_at` | DART sync |
| `external_source`, `external_id` | DART corp 연동 |

---

## 시세 스냅샷 DB

**CR_004 확장**: 시세는 **DB 미저장**, 요청 시 실시간 조회.
- 장중 현재가/등락률: Naver Finance HTML 파싱(`rate_info_krx` 블록)
- 일봉·MA·RSI: Yahoo chart API
- 추후 별도 CR에서 `company_market_snapshots`(KRX 공식 일봉 캐시) 도입 예정

---

## Agent 프롬프트 (V16)

`agent_prompts` / `agent_prompt_versions` — `task_type`: `INSIGHT_SUMMARY`, `RISK_DETECTION`, `RELATION_ANALYSIS`

템플릿 변수는 DB 컬럼이 아니라 **런타임 치환** (`CompanyInsightAgentService`).
