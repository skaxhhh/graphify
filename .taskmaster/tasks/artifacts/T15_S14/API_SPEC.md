# API 명세 — Task 15: S14 MCP 도구 관리 — OV03

> **우선순위**: P1  
> **구현 화면**: S14  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

- `GET /admin/tools`
- `POST /admin/tools`
- `PUT /admin/tools/{id}`
- `DELETE /admin/tools/{id}`
- `POST /admin/tools/{id}/ping`

## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
