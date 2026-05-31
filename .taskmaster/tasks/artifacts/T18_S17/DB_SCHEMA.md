# DB 스키마 — Task 18: S17 Vector DB 관리

> **마이그레이션**: `V18__vector_db.sql`

## vector_index_stats (싱글톤 id=1)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| total_vectors | BIGINT | 총 벡터 수 |
| index_size_bytes | BIGINT | 인덱스 용량 |
| vectors_by_type | JSONB | 유형별 건수 |
| avg_latency_ms | NUMERIC | 평균 응답(ms) |
| avg_similarity | NUMERIC | 평균 유사도 |
| request_count_24h | BIGINT | 24h 요청 수 |
| latency_series | JSONB | 차트용 시계열 |
| similarity_series | JSONB | 차트용 시계열 |
| request_series | JSONB | 차트용 시계열 |
| updated_at | TIMESTAMPTZ | 갱신 시각 |

## embedding_jobs

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| job_type | VARCHAR(32) | REINDEX, CLEANUP |
| scope | VARCHAR(32) | ALL, SELECTED |
| status | VARCHAR(32) | PENDING, RUNNING, SUCCESS, FAILED |
| progress | INT | 0–100 |
| message | TEXT | 상태 메시지 |
| target_ids | JSONB | 선택 재임베딩 대상 ID |
| deleted_count | BIGINT | 정리 작업 삭제 건수 |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| completed_at | TIMESTAMPTZ | |

인덱스: `idx_embedding_jobs_created (created_at DESC)`

## 시드

- `vector_index_stats`: populated 상태 (total_vectors=12840)
- `embedding_jobs`: 최근 REINDEX·CLEANUP 성공 작업 2건
