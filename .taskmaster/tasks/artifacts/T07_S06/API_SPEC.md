# API 명세 — Task 7: S06 검색 결과 목록 — 정렬·필터·하이브리드 검색

> **우선순위**: P0  
> **구현 화면**: S06  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트

### 자동완성 (DB only)

- `GET /api/v1/search/autocomplete?q=` — 2자 이상, DB `companies`만 조회 (외부 호출 없음)

### 검색 결과 (DB + Open DART enrich)

- `GET /api/v1/companies/search?q=&sort=&page=&size=&industry=&market=&dataStatus=&enrich=&enrichThreshold=`

| 파라미 | 기본 | 설명 |
|--------|------|------|
| `enrich` | `true` | DB 결과가 `enrichThreshold` 미만이면 DART corpCode 인덱스 검색 후 stub upsert |
| `enrichThreshold` | `3` | 외부 enrich 트리거 임계값 |

응답 `items[]` 추가 필드: `source` (`LOCAL` \| `EXTERNAL`), `syncStatus` (`STUB` \| `PARTIAL` \| `FULL`)

### 기업 확정 (신규)

- `POST /api/v1/companies/resolve` — Guest 허용

```json
{ "query": "삼성전자", "ticker": "005930", "externalSource": "DART", "externalId": "00126380" }
```

```json
{
  "success": true,
  "data": { "id": 42, "name": "삼성전자", "ticker": "005930", "syncStatus": "PARTIAL", "created": true }
}
```

### 상세 동기화 (신규)

- `POST /api/v1/companies/{id}/sync` — DART `company.json`으로 메타 갱신 후 상세 DTO 반환

### 성공 응답 (search 200)

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "삼성전자",
        "ticker": "005930",
        "industry": "반도체",
        "market": "KOSPI",
        "dataFreshness": "STALE",
        "updatedAt": "2026-05-18T00:00:00Z",
        "source": "EXTERNAL",
        "syncStatus": "STUB"
      }
    ],
    "semanticHints": { "relatedQueries": [], "similarCompanies": [] }
  },
  "meta": { "page": 0, "size": 20, "total": 1 }
}
```

## 환경 변수

| 변수 | 용도 |
|------|------|
| `DART_API_KEY` | Open DART 인증키 (`graphify.dart.api-key`) |

## 오류 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| ERR_SEARCH_002 | 400 | 검색어 없음 |
| ERR_SEARCH_003 | 404 | resolve 실패 |
| ERR_SEARCH_004 | 400 | resolve 파라미터 없음 |
| ERR_COMPANY_002 | 400 | sync — 외부 ID 없음 |
| ERR_COMPANY_003 | 502 | DART company.json 실패 |

## 공통 규약

- `Content-Type: application/json`
- Guest: search, autocomplete, resolve, GET detail
