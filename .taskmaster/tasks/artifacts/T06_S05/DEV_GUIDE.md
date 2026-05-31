# 개발 가이드 — Task 6: S05 비밀번호 재설정 확인

## UI 명세

- [UI_SPEC — S05](../../../docs/ui_specs/UI_SPEC_S05_PasswordResetConfirm.md)

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

- API: `GET /api/v1/auth/password-reset/validate?token=`, `POST /api/v1/auth/password-reset/confirm`
- 라우트: `/password-reset/confirm?token=...` (T05 dev 로그 링크와 동일)
- 성공 시 2초 카운트다운 후 S02(`/login`) 이동
- 토큰 무효/만료: S04 링크 + 고스트 버튼

