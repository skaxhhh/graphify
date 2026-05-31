# DB 스키마 — Task 6: S05 비밀번호 재설정 확인

> **구현 화면**: S05

## 테이블 (초안)

### `password_reset_tokens`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정

### `users`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정


## 참고

- RDB: PostgreSQL
- 민감정보(비밀번호 해시, API 키)는 암호화/KMS 정책에 따름
