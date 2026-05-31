# CR_002: S06 실시간 기업 검색·외부 연동·DB 동기화

## 원하는 것

- 현재는 **DB에 시드/저장된 기업만** `GET /companies/search`, `GET /search/autocomplete`로 조회 가능함.
- **유저 입력에 따른 실시간 기업 검색**이 가능하도록 API를 설계·구현해야 함.
- 이후 **S07 기업 상세**(인사이트·신호·관계 그래프)까지 이어질 수 있도록 **조회 → DB 반영 → 상세 hydrate** 흐름을 한 번에 설계.
- **자동완성(추천 목록)** 은 빠르게: **DB만** 참조 (기존 UX 유지).
- **전체 검색·기업 선택** 시 DB에 없으면 **외부 레지스트리에서 조회 후 upsert**(stub → 점진적 상세 동기화).

## 이유

- 시드 8개 수준의 `companies` 테이블만으로는 PRD §1·§1.1(자동완성·유사 검색) 및 실사용 검색 경험을 충족할 수 없음.
- S07 상세·Agent/MCP(DART 등) 연동의 **진입점**이 “검색으로 `companyId` 확보”이므로, 검색 단계에서 **안정적인 내부 ID**를 만들어 두는 것이 후속 태스크(T08 S07)와 맞음.

## 관련 화면 / 기능

| 항목 | ID |
|------|-----|
| 화면 | S01(홈 검색창), S06(검색 결과), S07(기업 상세 — 연동 대상) |
| 태스크 | T07_S06 (검색), T08_S07 (상세), T02_S01 (autocomplete) |
| 관련 파일 (초기 추정) | `SearchService`, `CompanySearchService`, `CompanyController`, `searchApi.ts`, `GlobalSearchBar`, `SearchResultPage`, `Company` 엔티티, Flyway `V21__company_external_registry.sql` |

## 의존 CR

- 없음 (T07/T08 기반 확장)

---

## 제안 아키텍처 (3계층)

```
[입력]
   │
   ├─ (A) 자동완성 2자+ ──► GET /search/autocomplete ──► DB only (≤10ms~50ms 목표)
   │
   ├─ (B) 전체 검색 제출 ──► GET /companies/search?enrich=true ──► DB + 외부 병합 + upsert
   │
   └─ (C) 기업 확정(선택/엔터) ──► POST /companies/resolve ──► DB hit | 외부 fetch + upsert → id
                                      │
                                      ▼
                               GET /companies/{id}  (S07)
                               POST /companies/{id}/sync  (상세·관계 hydrate, 비동기 가능)
```

### 역할 분리 (사용자 요구 반영)

| 계층 | 데이터 소스 | DB 쓰기 | 용도 |
|------|-------------|---------|------|
| **Autocomplete** | PostgreSQL | 없음 | 타이핑 중 추천 (S01/S06 검색창) |
| **Search (enriched)** | DB + 외부 API | **있음** (없는 기업 stub insert) | S06 결과 목록·“전체 검색” |
| **Resolve** | DB 우선 → 외부 | **있음** | 드롭다운 미스 후 S07 직행, 배치/Agent 전 |
| **Detail sync** | MCP/DART·내부 시드 | update 상세 필드 | S07 카드·신호·그래프 |

---

## API 설계

### 1. 자동완성 (변경 최소 — DB only)

**`GET /api/v1/search/autocomplete?q=`** (기존 유지)

- `q` 길이 ≥ 2
- `companies`에서 `name` / `ticker` prefix·contains (기존 `searchByKeyword`)
- 응답: `{ id, name, ticker?, matchType }[]` 최대 10건
- **외부 API 호출 금지** (debounce 250ms와 맞춤)

---

### 2. 검색 결과 (확장 — 실시간·DB 반영)

**`GET /api/v1/companies/search`**

기존 쿼리 유지 + 아래 추가:

| 파라미 | 타입 | 기본 | 설명 |
|--------|------|------|------|
| `q` | string | 필수 | 검색어 |
| `enrich` | boolean | `true` | `true`이면 DB 결과 부족 시 외부 검색 후 upsert·병합 |
| `enrichThreshold` | int | `3` | DB 결과가 이 값 미만일 때만 외부 호출 (비용 절감) |
| `sort`, `page`, `size`, `industry`, `market`, `dataStatus` | — | 기존 동일 | |

**처리 순서**

1. DB `searchCompanies` (현행)
2. `enrich=true` 이고 `items.size() < enrichThreshold` → `CompanyRegistryClient.search(q)`
3. 외부 후보를 `ticker` 또는 `(external_source, external_id)` 로 **매칭·upsert**
4. upsert 후 DB 재조회 또는 메모리 병합 (중복 제거: ticker > name 정규화)
5. 응답

**응답 `items[]` 필드 확장**

```json
{
  "id": 42,
  "name": "삼성전자",
  "ticker": "005930",
  "industry": "반도체",
  "market": "KOSPI",
  "dataFreshness": "STALE",
  "updatedAt": "2026-05-25T00:00:00Z",
  "source": "LOCAL",
  "syncStatus": "STUB"
}
```

| 필드 | 설명 |
|------|------|
| `source` | `LOCAL` \| `EXTERNAL` (이번 검색에서 막 upsert된 항목) |
| `syncStatus` | `STUB` \| `PARTIAL` \| `FULL` — S07 진입 전 상태 표시용 |

`semanticHints`는 기존 휴리스틱 유지 (Vector는 후속).

---

### 3. 기업 확정 (신규 — S07 진입용)

**`POST /api/v1/companies/resolve`**

Guest 허용 (검색과 동일).

**Request** (둘 중 하나 이상)

```json
{
  "query": "삼성전자",
  "ticker": "005930",
  "externalSource": "DART",
  "externalId": "00126380"
}
```

**Response 200**

```json
{
  "success": true,
  "data": {
    "id": 42,
    "name": "삼성전자",
    "ticker": "005930",
    "syncStatus": "STUB",
    "created": false
  }
}
```

| 필드 | 설명 |
|------|------|
| `created` | 이번 요청에서 DB에 새로 insert 했는지 |

**처리**

1. `ticker` → DB `findByTicker`
2. 없으면 `externalId` → DB `findByExternal`
3. 없으면 `query` → DB name 검색
4. 없으면 외부 1건 resolve → **insert stub**
5. `id` 반환 (프론트: `/companies/{id}` 이동)

**에러**

- `ERR_SEARCH_003` 404: 외부에서도 찾을 수 없음
- `ERR_SEARCH_004` 400: query/ticker 모두 비어 있음

---

### 4. 상세 hydrate (신규 — S07·후속 연동)

**`GET /api/v1/companies/{id}`** (기존)

- `syncStatus=STUB|PARTIAL` 이면 응답에 `needsSync: true` 추가 (프론트 배너·백그라운드 sync 트리거)

**`POST /api/v1/companies/{id}/sync`** (신규)

- 외부/DART·MCP로 **요약·업종·시장·공시 메타** pull → `companies` update, `data_status` 갱신
- 관계/인사이트는 기존 `insights`·Agent 파이프라인으로 **비동기** (이 CR 1차에서는 stub 필드만)

| 단계 | 이 CR | 후속 |
|------|-------|------|
| stub upsert | ✅ | |
| summary, industry, market | ✅ sync API 1차 | |
| insight cards, signals, graph | ❌ | T08 Agent/MCP |

---

## 외부 연동 (CompanyRegistryClient)

```java
public interface CompanyRegistryClient {
    List<ExternalCompanyCandidate> searchByKeyword(String query, int limit);
    Optional<ExternalCompanyProfile> findByTicker(String ticker);
    Optional<ExternalCompanyProfile> findByExternalId(String source, String externalId);
}
```

**1차 구현 후보**

| Provider | 용도 | 비고 |
|----------|------|------|
| **Open DART** | 법인명 검색, corp_code, 종목코드 | PRD MCP·공시와 동일 계열 |
| **Dev stub** | API 키 없을 때 | `application-dev.yml` mock 목록 |

환경 변수: `DART_API_KEY` (`.env.example` 문서화)

---

## DB 스키마 (V21 초안)

`companies` 확장:

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `external_source` | VARCHAR(32) | `DART`, `MANUAL`, null=레거시 시드 |
| `external_id` | VARCHAR(64) | DART corp_code 등 |
| `sync_status` | VARCHAR(16) | `STUB` / `PARTIAL` / `FULL` |
| `detail_synced_at` | TIMESTAMPTZ | 마지막 상세 동기화 |
| `listed` | BOOLEAN | 상장 여부 (nullable) |

제약:

- `UNIQUE (external_source, external_id)` WHERE both not null
- `UNIQUE (ticker)` WHERE ticker IS NOT NULL — upsert 시 충돌 방지

인덱스: 기존 `LOWER(name)`, `LOWER(ticker)` 유지.

---

## 프론트 연동 (S01/S06)

| UX | 변경 |
|----|------|
| Autocomplete | **변경 없음** (`fetchAutocomplete` → DB) |
| S06 검색 | `fetchCompanySearch({ enrich: true })` |
| Autocomplete 0건 → “전체 검색” | 기존 `/search?q=` → enrich search로 외부 기업 노출 |
| 결과 행 `source=EXTERNAL` | (옵션) “새로 등록됨” 뱃지 |
| S07 진입 전 | resolve 불필요 if search already upserted; 직접 링크만 있을 때 `POST resolve` |

---

## 비기능 요구

| 항목 | 목표 |
|------|------|
| Autocomplete p95 | &lt; 100ms (DB only) |
| Search + enrich p95 | &lt; 2s (외부 1회, timeout 3s) |
| Rate limit | IP당 외부 검색 30/min (dev 제외) |
| Idempotent upsert | 동일 ticker/corp_code 재검색 시 duplicate row 없음 |

---

## 범위 판단 (PHASE 2 예비)

### 이번 CR 포함 (P0)

- V21 migration + Entity 필드
- `CompanyRegistryClient` + DART 또는 dev stub
- `CompanyUpsertService` (stub insert/update)
- `GET /companies/search?enrich=`
- `POST /companies/resolve`
- `POST /companies/{id}/sync` (기본 메타만)
- SecurityConfig Guest 허용
- `T07_S06` API_SPEC / DB_SCHEMA / DEV_GUIDE 갱신
- S06 `searchApi` + (필요 시) empty 상태 copy

### 제외 (P1~)

- Vector semantic search / embedding
- Autocomplete에 외부 결과 노출
- Agent/MCP 자동 관계 그래프 생성
- Admin 기업 수동 merge UI

---

## 명확화된 요구사항

(PHASE 1.5 — 사용자 메시지 기반 선반영)

- **목표**: DB에 없는 상장/법인도 검색·선택 가능, 이후 S07에서 상세 확장.
- **포함**: autocomplete=DB, search=DB+외부+upsert, resolve, sync 1차, 스키마.
- **제외**: Vector 자동완성, 전체 Agent 파이프라인.
- **제약**: 공통 API envelope, Guest 검색 허용, `any` 금지.
- **완료 기준**: Playwright/curl — DB에 없는 기업명 검색 시 S06 목록 노출 → 클릭 → S07 `id` 존재, `companies` row 생성.

## 확정 스코프

- Open DART (A): corpCode.xml 캐시 + company.json sync
- V21, enrich search, resolve, sync API, 프론트 `enrich=true` 기본

## 롤백 정보

- Flyway V21 down: 컬럼 drop (수동)
- `graphify.company.registry.dart` 패키지 제거 시 enrich 비활성

## 변경 이력

- 2026-05-25 PHASE 1 완료 — CR 템플릿 및 API·DB 설계 초안
- 2026-05-26 PHASE 3 완료 — DART 연동 구현 (CR_002)
