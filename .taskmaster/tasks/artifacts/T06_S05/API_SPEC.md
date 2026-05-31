# API 명세 — Task 6: S05 비밀번호 재설정 확인

> **우선순위**: P1  
> **구현 화면**: S05  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

- `GET /api/v1/auth/password-reset/validate?token=`
- `POST /api/v1/auth/password-reset/confirm`

### validate 성공 (200)

```json
{
  "success": true,
  "data": { "valid": true, "expiresAt": "2026-05-18T12:00:00Z" }
}
```

무효/만료 토큰도 200이며 `valid: false`, `expiresAt: null`.

### confirm 요청

```json
{ "token": "...", "newPassword": "newpassword123" }
```

### confirm 성공 (200)

```json
{
  "success": true,
  "data": { "message": "비밀번호가 변경되었습니다. 로그인해 주세요." }
}
```

### 오류

| code | HTTP | 설명 |
|------|------|------|
| ERR_AUTH_012 | 400 | 토큰 만료/무효 |
| ERR_AUTH_013 | 400 | 이메일 로그인 계정 아님 |
| ERR_VALIDATION_001 | 400 | 비밀번호 8자 미만 |

## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
