## 테스트 시나리오

### API dummy 데이터

**GET /admin/prompts?type=RELATION_ANALYSIS — 성공**
```http
GET /api/v1/admin/prompts?type=RELATION_ANALYSIS
Authorization: Bearer {adminToken}
```
기대 응답: 200 / `{"success": true, "data": { "id", "type", "systemPrompt", "taskTemplate", "versions": [...] }}`

**GET /admin/prompts — 실패 (type 오류)**
```http
GET /api/v1/admin/prompts?type=INVALID
```
기대 응답: 400 / `ERR_ADMIN_PROMPT_002`

**POST /admin/prompts — 성공**
```json
{
  "type": "RELATION_ANALYSIS",
  "systemPrompt": "...",
  "taskTemplate": "...",
  "changeNote": "메모"
}
```
기대 응답: 200 / 새 `versions[]` 항목 prepend

**POST /admin/prompts/{id}/test — 성공**
```json
{ "companyId": 1, "sampleInput": "선택 입력" }
```
기대 응답: 200 / `output`, `tokenUsage`, `companyName`

**POST /admin/prompts/{id}/rollback — 성공**
```json
{ "targetVersionId": 1 }
```
기대 응답: 200 / 에디터 내용이 해당 버전으로 복원 + 롤백 버전 추가

### 화면 렌더링 mock 데이터

```json
{
  "id": 1,
  "type": "RELATION_ANALYSIS",
  "systemPrompt": "당신은 기업 관계 분석 전문 AI입니다...",
  "taskTemplate": "대상 기업의 공급망·투자·협력 관계를...",
  "versions": [
    {
      "id": 2,
      "versionNumber": 2,
      "createdAt": "2026-05-21T10:00:00Z",
      "author": "관리자",
      "summary": "저장 — UI 테스트",
      "changeNote": "UI 테스트"
    },
    {
      "id": 1,
      "versionNumber": 1,
      "createdAt": "2026-05-20T09:00:00Z",
      "author": "시스템",
      "summary": "v1 초기 버전",
      "changeNote": null
    }
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
