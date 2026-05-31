## 테스트 시나리오

### API dummy 데이터

**GET /admin/openai/config — 성공**
```http
GET /api/v1/admin/openai/config
Authorization: Bearer {adminToken}
```
기대 응답: 200 / `{"success": true, "data": { "configured": false|true, "endpointUrl", "hasApiKey", ... }}`

**PUT /admin/openai/config — 성공**
```json
{
  "endpointUrl": "https://your-resource.openai.azure.com",
  "apiKey": "your-azure-openai-key",
  "deploymentName": "gpt-4o",
  "apiVersion": "2024-02-15",
  "model": "gpt-4o",
  "temperature": 0.3,
  "maxTokens": 4096,
  "topP": 1,
  "embeddingModel": "text-embedding-3-large",
  "embeddingDeployment": "text-embedding-3-large"
}
```
기대 응답: 200 / `configured: true`, `hasApiKey: true`

**PUT — 실패 (https 아님)**
```json
{ "endpointUrl": "http://bad", "deploymentName": "gpt-4o", "apiVersion": "2024-02-15", "model": "gpt-4o", "temperature": 0.3, "maxTokens": 4096, "topP": 1, "embeddingModel": "text-embedding-3-large", "embeddingDeployment": "text-embedding-3-large" }
```
기대 응답: 400 / `ERR_ADMIN_OPENAI_002`

**GET /admin/openai/status?refresh=true — 성공**
```http
GET /api/v1/admin/openai/status?refresh=true
Authorization: Bearer {adminToken}
```
기대 응답: 200 / `connection`: `OK` | `ERROR` | `NOT_CONFIGURED`

### 화면 렌더링 mock 데이터

```json
{
  "configured": true,
  "endpointUrl": "https://demo.openai.azure.com",
  "deploymentName": "gpt-4o",
  "apiVersion": "2024-02-15",
  "model": "gpt-4o",
  "temperature": 0.3,
  "maxTokens": 4096,
  "topP": 1,
  "embeddingModel": "text-embedding-3-large",
  "embeddingDeployment": "text-embedding-3-large",
  "fallbackEndpoint": null,
  "fallbackDeploymentName": null,
  "hasApiKey": true,
  "hasFallbackApiKey": false
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
