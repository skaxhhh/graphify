# DB 스키마 — Task 2: S01 홈 피드

> **마이그레이션**: `V19__home_feed.sql`

## company_view_stats

| 컬럼 | 타입 | 설명 |
|------|------|------|
| company_id | BIGINT PK, FK companies | |
| view_count | BIGINT | 누적 조회 수 |
| updated_at | TIMESTAMPTZ | 최근 갱신 |

기업 상세 조회 시 `+1` (CompanyViewTracker).

## market_news

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| title | VARCHAR(512) | |
| summary | TEXT | |
| source_name | VARCHAR(128) | |
| source_url | VARCHAR(1024) | |
| ticker | VARCHAR(32) | nullable |
| company_name | VARCHAR(255) | nullable |
| published_at | TIMESTAMPTZ | |

인덱스: `published_at DESC`
