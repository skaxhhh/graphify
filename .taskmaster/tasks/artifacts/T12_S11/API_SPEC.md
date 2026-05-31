# API 명세 — Task 12: S11 관심 기업 목록·비교 (최대 3)

> **우선순위**: P1  
> **구현 화면**: S11  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

- `GET /api/v1/watchlist/me` — JWT 필수
- `POST /api/v1/watchlist/me` — body `{ "companyId": number }`
- `DELETE /api/v1/watchlist/me/{companyId}`
- `GET /api/v1/companies/compare?ids=1,2,3&basis=INVESTMENT|SUPPLY_CHAIN|PARTNERSHIP` — JWT 필수, 최대 3개

### compare 응답 `data`

| 필드 | 설명 |
|------|------|
| basis | 비교 기준 |
| companies[] | `companyId`, `name`, `industry`, `insightCards[]`, `metrics` |

오류: `ERR_COMPARE_002` (400) 4개 초과, `ERR_COMPARE_003` (400) 잘못된 basis

## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
