# API 명세 — Task 13: S12 마이페이지 — 비밀번호·프롬프트·OV05

> **우선순위**: P1  
> **구현 화면**: S12  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

- `GET /api/v1/users/me` — JWT 필수
- `PUT /api/v1/users/me/password` — body `{ currentPassword, newPassword }` (이메일 계정만)
- `PUT /api/v1/users/me/prompt` — body `{ customPrompt }` (Premium만)
- `POST /api/v1/auth/logout` — JWT (클라이언트 세션 삭제용)

### GET /users/me `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | number | 사용자 ID |
| email | string | 이메일 |
| displayName | string | 표시 이름 |
| authProvider | `email` \| `google` \| `naver` \| `kakao` | 로그인 수단 |
| isPremium | boolean | Premium 여부 |
| customPrompt | string \| null | 커스텀 프롬프트 |

오류: `ERR_USER_003` 현재 비밀번호 불일치, `ERR_USER_005` Premium 아님

## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
