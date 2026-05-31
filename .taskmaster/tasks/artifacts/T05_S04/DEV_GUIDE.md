# 개발 가이드 — Task 5: S04 비밀번호 재설정 요청

## UI 명세

- [UI_SPEC — S04](../../../docs/ui_specs/UI_SPEC_S04_PasswordResetRequest.md)

## 구현 순서

1. UI_SPEC **레이아웃 구조** 및 breakpoint
2. **loading / empty / error / populated** 상태
3. **인터랙션 규칙** (해당 UI_SPEC 표)
4. `API_SPEC.md`와 Spring Controller DTO 정합

## 스택

- React + Spring Boot + PostgreSQL

### 완료 기준 (필수)
- [x] UI_SPEC 레이아웃 구조 준수
- [x] 4가지 상태(loading / empty / error / populated) 구현
- [x] 인터랙션 규칙 구현 (해당 화면 UI_SPEC 기준)
- [x] 이전·다음 태스크 인터페이스 연동 확인 (라우팅·API 계약·세션 상태)

## 구현 메모

- API: `POST /api/v1/auth/password-reset/request` (body: `{ email }`)
- DB: `V5__password_reset_tokens.sql`
- dev 환경: 재설정 링크는 백엔드 로그에 출력 (`[dev] Password reset link for ...`)
- S02 로그인 카드 「비밀번호 찾기」→ `/password-reset`
- S05(T06) confirm 화면 링크: `{frontend}/password-reset/confirm?token=...`

