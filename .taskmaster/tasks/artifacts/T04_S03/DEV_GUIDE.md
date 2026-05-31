# 개발 가이드 — Task 4: S03 약관 동의

## UI 명세

- [UI_SPEC — S03](../../../docs/ui_specs/UI_SPEC_S03_TermsConsent.md)

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

## 확인 URL

- 약관 동의: `http://localhost:5173/terms`
- 테스트: `newuser@graphify.dev` / `password123` 로그인 → `/terms` → 동의 후 `/`

