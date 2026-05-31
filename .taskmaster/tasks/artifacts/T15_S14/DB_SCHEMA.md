# DB 스키마 — Task 15: S14 MCP 도구 관리 — OV03

> **구현 화면**: S14  
> **마이그레이션**: `V15__mcp_tools.sql`

## 테이블

### `mcp_tools`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| name | VARCHAR(128) UNIQUE | 도구 식별자 |
| description | TEXT | 요약 설명 |
| endpoint_url | VARCHAR(512) | MCP 엔드포인트 |
| auth_type | VARCHAR(32) | NONE, API_KEY, BEARER |
| auth_secret | VARCHAR(512) | 인증 시크릿 (개발: 평문) |
| schema_json | TEXT | JSON 스키마 (선택) |
| connection_status | VARCHAR(32) | CONNECTED, DISCONNECTED, ERROR, UNKNOWN |
| enabled | BOOLEAN | 활성 여부 |
| allowed_roles | VARCHAR(128) | 콤마 구분 USER,ADMIN,PREMIUM |
| last_called_at | TIMESTAMPTZ | 마지막 Ping 시각 |
| created_at / updated_at | TIMESTAMPTZ | |

## 시드

- `company-search`, `news-ingest`, `graph-relations` (3건)

## 참고

- RDB: PostgreSQL
- 민감정보(비밀번호 해시, API 키)는 암호화/KMS 정책에 따름 (운영 전환 시)
