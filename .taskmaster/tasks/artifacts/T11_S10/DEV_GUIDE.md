# 개발 가이드 — Task 11: S10 분석 이력 상세·시계열 비교

## UI 명세

- [UI_SPEC — S10](../../../docs/ui_specs/UI_SPEC_S10_AnalysisHistoryDetail.md)

## 구현 순서

1. UI_SPEC **레이아웃 구조** 및 breakpoint
2. **loading / empty / error / populated** 상태
3. **인터랙션 규칙** (해당 UI_SPEC 표)
4. `API_SPEC.md`와 Spring Controller DTO 정합

## 스택

- React + Spring Boot + PostgreSQL

## 테스트 시나리오

### API dummy 데이터

> API_SPEC.md 의 엔드포인트 기준으로 작성

**GET /api/v1/history/{sessionId} — 성공 케이스**
```
Authorization: Bearer {accessToken}
```
기대 응답: 200 / `{"success": true, "data": { sessionId, company, timeline, graphSnapshot, insights, signals, diffSummary }}`

**GET /api/v1/history/{sessionId} — 실패 케이스**
```
GET /api/v1/history/00000000-0000-0000-0000-000000000000
```
기대 응답: 404 / `{"success": false, "error": {"code": "ERR_HISTORY_002", ...}}`

### 화면 렌더링 mock 데이터

> UI_SPEC populated 상태 기준, 실제 API 응답 구조와 동일하게 작성

```json
{
  "sessionId": "8acb7622-3dd0-4f6c-ad03-643d512de8e6",
  "company": { "id": 1, "name": "삼성전자" },
  "analyzedAt": "2026-05-18T00:29:09.551241Z",
  "status": "COMPLETED",
  "summaryLine": "HBM 공급망·AI GPU 고객 관계 인사이트 2건",
  "timeline": [{ "t": "...", "eventType": "COMPLETED", "label": "분석 완료", "payload": {} }],
  "graphSnapshot": { "nodes": [], "edges": [] },
  "insights": [],
  "signals": [],
  "diffSummary": { "text": "...", "generatedAt": "..." }
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
