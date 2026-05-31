# API 명세 — Task 17: S16 Azure OpenAI 연결 설정

> **구현 화면**: S16  
> **Base path**: `/api/v1/admin/openai`

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/config` | 현재 설정 조회 (`hasApiKey`만, 키 값 미노출) |
| PUT | `/config` | 설정 저장 (apiKey 빈 값이면 기존 키 유지) |
| GET | `/status` | 연결 상태·토큰·Rate limit |
| GET | `/status?refresh=true` | 상태 재검증 후 갱신 |

## GET /config 응답

```json
{
  "success": true,
  "data": {
    "configured": true,
    "endpointUrl": "https://xxx.openai.azure.com",
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
}
```

## PUT /config body

| 필드 | 필수 | 설명 |
|------|------|------|
| endpointUrl | Y | https Azure OpenAI URL |
| apiKey | 조건부 | 최초 저장 필수, 이후 빈 값이면 유지 |
| deploymentName | Y | |
| apiVersion | Y | |
| model | Y | gpt-4o, gpt-4o-mini, gpt-4.1, gpt-4.1-mini |
| temperature | Y | 0~2 |
| maxTokens | Y | 1~128000 |
| topP | Y | 0~1 |
| embeddingModel | Y | |
| embeddingDeployment | Y | |
| fallbackEndpoint | N | |
| fallbackApiKey | N | |
| fallbackDeploymentName | N | |

## GET /status 응답

```json
{
  "success": true,
  "data": {
    "connection": "OK",
    "tokensUsed": 0,
    "rateLimitRemaining": 100000,
    "lastCheckedAt": "2026-05-21T12:00:00Z",
    "message": "연결 설정이 유효합니다."
  }
}
```

`connection`: `OK` | `ERROR` | `NOT_CONFIGURED`

## 오류 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| ERR_ADMIN_OPENAI_002 | 400 | 검증 실패 |

## 공통

- Bearer JWT, `ROLE_ADMIN`
- API 키는 DB에 AES-GCM 암호화 저장
