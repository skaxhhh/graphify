# 개발 가이드 — Task 13: S12 마이페이지

## UI 명세

- [UI_SPEC — S12](../../../docs/ui_specs/UI_SPEC_S12_MyPage.md)

## 테스트 시나리오

### API dummy 데이터

**GET /api/v1/users/me — 성공**
```
Authorization: Bearer {token}
```
기대 응답: 200 / `displayName`, `email`, `authProvider`, `isPremium`, `customPrompt`

**GET /api/v1/users/me — 실패**
```
(토큰 없음)
```
기대 응답: 401 / `ERR_AUTH_001`

**PUT /api/v1/users/me/password — 실패**
```json
{ "currentPassword": "wrong", "newPassword": "newpass1234" }
```
기대 응답: 400 / `ERR_USER_003`

**PUT /api/v1/users/me/prompt — 성공 (Premium)**
```json
{ "customPrompt": "HBM 공급망 분석 시 투자 관계를 우선 요약해 주세요." }
```
기대 응답: 200

### 화면 렌더링 mock 데이터

```json
{
  "id": 1,
  "email": "demo@graphify.dev",
  "displayName": "데모 사용자",
  "authProvider": "email",
  "isPremium": true,
  "customPrompt": "삼성전자·SK하이닉스 관계 분석 시 HBM 공급망과 투자 관계를 우선적으로 요약해 주세요."
}
```

---

### 완료 기준 (필수)
- [x] UI_SPEC 레이아웃 구조 준수
- [x] 4가지 상태(loading / empty / error / populated) 구현
- [x] 인터랙션 규칙 구현 (해당 화면 UI_SPEC 기준)
- [x] 이전·다음 태스크 인터페이스 연동 확인 (라우팅·API 계약·세션 상태)
- [x] API 테스트: 모든 엔드포인트 성공·실패 케이스 PASS
- [x] 화면 렌더링: 4가지 상태 정상 렌더링 PASS
