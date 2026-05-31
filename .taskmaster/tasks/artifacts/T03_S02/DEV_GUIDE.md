# 개발 가이드 — Task 3: S02 로그인 — 소셜·이메일 인증

## UI 명세

- [UI_SPEC — S02](../../../docs/ui_specs/UI_SPEC_S02_Login.md)

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

## 로컬 테스트 계정

| 이메일 | 비밀번호 | 비고 |
|--------|----------|------|
| `demo@graphify.dev` | `password123` | 약관 동의 완료 → 로그인 후 S01 |
| `newuser@graphify.dev` | `password123` | 약관 미동의 → 로그인 후 S03(`/terms`) |

## 확인 URL

- 로그인: `http://localhost:5173/login`
- OAuth 콜백: `http://localhost:5173/auth/callback`
- 약관(S03 placeholder): `http://localhost:5173/terms`

