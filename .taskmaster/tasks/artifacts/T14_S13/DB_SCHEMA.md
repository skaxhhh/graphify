# DB 스키마 — Task 14: S13 관리자 대시보드 — Agent 실행·토큰·알림

> **구현 화면**: S13

## 테이블 (초안)

### `agent_sessions`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정

### `admin_metrics_daily`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정


## 참고

- RDB: PostgreSQL
- 민감정보(비밀번호 해시, API 키)는 암호화/KMS 정책에 따름
