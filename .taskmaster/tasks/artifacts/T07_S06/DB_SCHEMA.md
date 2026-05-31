# DB 스키마 — Task 7: S06 검색 + 외부 레지스트리

> **구현 화면**: S06, S07 연동

## `companies` (`V2` + `V6` + `V21__company_external_registry.sql`)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| name | VARCHAR(255) | 기업명 |
| ticker | VARCHAR(32) | 종목코드 |
| industry | VARCHAR(128) | 업종 (DART induty_code 등) |
| market | VARCHAR(32) | KOSPI / KOSDAQ |
| data_status | VARCHAR(32) | FRESH / STALE |
| summary | TEXT | 요약 |
| external_source | VARCHAR(32) | `DART` |
| external_id | VARCHAR(64) | DART corp_code (8자) |
| sync_status | VARCHAR(16) | STUB / PARTIAL / FULL |
| detail_synced_at | TIMESTAMPTZ | 마지막 DART 상세 동기화 |
| listed | BOOLEAN | 상장 여부 |
| updated_at | TIMESTAMPTZ | |

인덱스:

- `LOWER(name)`, `LOWER(ticker)`, `industry`, `market` (기존)
- `UNIQUE (external_source, external_id)` WHERE NOT NULL
- `UNIQUE (ticker)` WHERE ticker IS NOT NULL

## 외부 데이터 흐름

1. DART `corpCode.xml` → 인메모리 인덱스 (24h 캐시)
2. 검색 enrich / resolve → stub `INSERT`
3. `company.json` sync → `PARTIAL` + industry/market/summary 갱신
