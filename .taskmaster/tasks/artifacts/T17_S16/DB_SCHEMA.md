# DB 스키마 — Task 17: S16 Azure OpenAI 연결 설정

> **마이그레이션**: `V17__openai_settings.sql`

## `openai_settings` (싱글톤 id=1)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 항상 1 |
| endpoint_url | VARCHAR(512) | Azure OpenAI URL |
| api_key_encrypted | TEXT | AES-GCM 암호화 API 키 |
| deployment_name | VARCHAR(128) | |
| api_version | VARCHAR(32) | |
| model | VARCHAR(64) | |
| temperature | NUMERIC(4,2) | |
| max_tokens | INT | |
| top_p | NUMERIC(4,2) | |
| embedding_model | VARCHAR(64) | |
| embedding_deployment | VARCHAR(128) | |
| fallback_endpoint_url | VARCHAR(512) | 선택 |
| fallback_api_key_encrypted | TEXT | 선택, 암호화 |
| fallback_deployment_name | VARCHAR(128) | |
| tokens_used | BIGINT | 상태 표시용 |
| rate_limit_remaining | INT | |
| last_status | VARCHAR(32) | |
| last_checked_at | TIMESTAMPTZ | |
| created_at / updated_at | TIMESTAMPTZ | |

**암호화 키**: `graphify.secrets.encryption-key` (application-dev.yml)

## 시드

id=1 빈 엔드포인트, `NOT_CONFIGURED` — 화면 empty 상태.
