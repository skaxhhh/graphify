## 테스트 시나리오

### API dummy 데이터

**GET /admin/agent/stats?period=day — 성공 케이스**
```json
{}
```
기대 응답: 200 / `{"success": true, "data": { "runCount", "avgDurationMs", "tokenUsage", "errorRate", "period", "series", "alerts" }}`

**GET /admin/agent/stats?period=month — 실패 케이스**
```json
{}
```
기대 응답: 400 / `{"success": false, "error": {"code": "ERR_ADMIN_002", ...}}`

**GET /admin/agent/stats — 실패 (ROLE_USER)**
기대 응답: 403 / JSON 오류 본문

### 화면 렌더링 mock 데이터

```json
{
  "runCount": 620,
  "avgDurationMs": 12800,
  "tokenUsage": 2100000,
  "errorRate": 0.04,
  "period": "day",
  "series": [
    { "date": "2026-05-05", "runCount": 45, "tokenUsage": 162000, "errorCount": 1 }
  ],
  "alerts": [
    { "severity": "WARN", "message": "오류율 상승", "detectedAt": "2026-05-19T10:00:00Z" }
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
