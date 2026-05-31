# API 명세 — Task 9: S08 관계 그래프 시각화 — 필터·깊이·SSE·OV01/08

> **우선순위**: P0  
> **구현 화면**: S08  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

- `GET /companies/{id}/graph?depth=&filter=`
- `SSE /agent/stream/{sessionId}`

## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
