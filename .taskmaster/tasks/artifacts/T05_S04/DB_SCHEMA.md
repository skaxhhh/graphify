# DB 스키마 — Task 5: S04 비밀번호 재설정 요청

> **구현 화면**: S04

## 테이블 (초안)

### `password_reset_tokens` (`V5__password_reset_tokens.sql`)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| token_hash | VARCHAR(64) UNIQUE | SHA-256(raw token) |
| expires_at | TIMESTAMPTZ | 만료 시각 |
| used_at | TIMESTAMPTZ NULL | 사용 시각 (S05) |
| created_at | TIMESTAMPTZ | 생성 시각 |

인덱스: `(user_id, created_at DESC)`, `(expires_at)`


## 참고

- RDB: PostgreSQL
- 민감정보(비밀번호 해시, API 키)는 암호화/KMS 정책에 따름
