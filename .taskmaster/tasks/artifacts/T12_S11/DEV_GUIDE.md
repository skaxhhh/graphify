# 개발 가이드 — Task 12: S11 관심 기업 목록·비교

## UI 명세

- [UI_SPEC — S11](../../../docs/ui_specs/UI_SPEC_S11_Watchlist.md)

## 테스트 시나리오

### API dummy 데이터

**GET /api/v1/watchlist/me — 성공**
```
Authorization: Bearer {token}
```
기대 응답: 200 / `{"success": true, "data": { "items": [...] }}`

**GET /api/v1/watchlist/me — 실패**
```
(토큰 없음)
```
기대 응답: 401 / `ERR_AUTH_001`

**GET /api/v1/companies/compare?ids=1,2,3&basis=INVESTMENT — 성공**
기대 응답: 200 / `companies[]` 3건

**GET /api/v1/companies/compare?ids=1,2,3,4 — 실패**
기대 응답: 400 / `ERR_COMPARE_002`

### 화면 렌더링 mock 데이터

```json
{
  "items": [
    { "companyId": 1, "name": "삼성전자", "industry": "반도체", "ticker": "005930", "addedAt": "2026-05-18T00:00:00Z" },
    { "companyId": 2, "name": "SK하이닉스", "industry": "반도체", "ticker": "000660", "addedAt": "2026-05-17T00:00:00Z" },
    { "companyId": 5, "name": "NAVER", "industry": "인터넷", "ticker": "035420", "addedAt": "2026-05-16T00:00:00Z" }
  ]
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
