## 테스트 시나리오

### API dummy 데이터

**GET /admin/tools — 성공**
```http
GET /api/v1/admin/tools
Authorization: Bearer {adminToken}
```
기대 응답: 200 / `{"success": true, "data": { "tools": [...] }}`

**POST /admin/tools — 실패 (authType 오류)**
```json
{ "name": "x", "endpointUrl": "http://x", "authType": "INVALID", "allowedRoles": ["USER"] }
```
기대 응답: 400 / `ERR_ADMIN_MCP_003`

**GET /admin/tools — 실패 (ROLE_USER)**
기대 응답: 403

### 화면 렌더링 mock 데이터

```json
{
  "tools": [
    {
      "id": 1,
      "name": "company-search",
      "description": "기업 검색 MCP",
      "endpointUrl": "http://localhost:8090/mcp/company-search",
      "authType": "API_KEY",
      "schemaJson": null,
      "status": "CONNECTED",
      "enabled": true,
      "allowedRoles": ["USER", "PREMIUM"],
      "lastCalledAt": "2026-05-19T10:00:00Z"
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
