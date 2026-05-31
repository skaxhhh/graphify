# API 명세 — Task 5: S04 비밀번호 재설정 요청

> **우선순위**: P1  
> **구현 화면**: S04  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

- `POST /api/v1/auth/password-reset/request`

### 요청

```json
{ "email": "user@example.com" }
```

### 성공 응답 (200)

```json
{
  "success": true,
  "data": {
    "message": "입력하신 이메일로 비밀번호 재설정 안내를 발송했습니다.",
    "maskedEmail": "d***@graphify.dev"
  }
}
```

### 오류

| code | HTTP | 설명 |
|------|------|------|
| ERR_VALIDATION_001 | 400 | 이메일 형식 오류 |
| ERR_AUTH_011 | 429 | 요청 횟수 제한 (15분 내 3회) |

## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
