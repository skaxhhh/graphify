# DB 스키마 — Task 11: S10 분석 이력 상세·시계열 비교

> **구현 화면**: S10

## 테이블 (초안)

### `agent_sessions`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정

### `graph_snapshots`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정

### `timeline_events`
- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정


## 참고

- RDB: PostgreSQL
- 민감정보(비밀번호 해시, API 키)는 암호화/KMS 정책에 따름
