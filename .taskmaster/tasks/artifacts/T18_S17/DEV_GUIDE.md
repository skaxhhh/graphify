## 테스트 시나리오

### API dummy 데이터

**POST /reindex — 성공 (전체)**
```json
{ "scope": "ALL" }
```
기대 응답: 200 / `{"success": true, "data": {"jobId": <number>}}`

**POST /reindex — 실패 (선택인데 targetIds 없음)**
```json
{ "scope": "SELECTED", "targetIds": [] }
```
기대 응답: 400 / `ERR_ADMIN_VECTOR_002`

**DELETE /cleanup — 성공**
```json
{ "olderThanDays": 90, "types": ["COMPANY", "INSIGHT"] }
```
기대 응답: 200 / `{"success": true, "data": {"deletedCount": <number>}}`

**DELETE /cleanup — 실패 (types 비움)**
```json
{ "olderThanDays": 90, "types": [] }
```
기대 응답: 400 / `ERR_ADMIN_VECTOR_002`

### 화면 렌더링 mock 데이터

```json
{
  "totalVectors": 12840,
  "byType": { "COMPANY": 5200, "INSIGHT": 4100, "RELATION": 3540 },
  "indexSizeBytes": 268435456,
  "avgLatencyMs": 142.5,
  "avgSimilarity": 0.872,
  "requestCount24h": 1842,
  "latencySeries": [120, 135, 128, 142, 150],
  "similaritySeries": [0.85, 0.86, 0.87, 0.88, 0.87],
  "requestSeries": [120, 140, 130, 150, 160]
}
```

---

### 완료 기준 (필수)
- [x] UI_SPEC 레이아웃 구조 준수
- [x] 4가지 상태(loading / empty / error / populated) 구현
- [x] 인터랙션 규칙 구현 (재임베딩 확인·2s 폴링·cleanup DELETE 확인·300ms debounce)
- [x] 이전·다음 태스크 인터페이스 연동 확인 (라우팅·API 계약·세션 상태)
- [x] API 테스트: `test.http` 작성
- [x] 화면 렌더링: `ui_prototype/T18_S17_test.html`
