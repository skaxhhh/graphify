# DB 스키마 — Task 3: S02 로그인 — 소셜·이메일 인증

> **구현 화면**: S02

## 테이블 (초안)

### `users`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정

### `user_auth_providers`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정


## 참고

- RDB: PostgreSQL
- 민감정보(비밀번호 해시, API 키)는 암호화/KMS 정책에 따름
