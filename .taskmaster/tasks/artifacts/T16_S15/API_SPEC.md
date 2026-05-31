# API 명세 — Task 16: S15 Agent 프롬프트 관리 — 버전·OV04

> **우선순위**: P2  
> **구현 화면**: S15  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/admin/prompts?type=` | 태스크 유형별 프롬프트 + 버전 목록 |
| POST | `/api/v1/admin/prompts` | 저장 (신규 버전 생성) |
| POST | `/api/v1/admin/prompts/{id}/test` | 테스트 실행 (mock) |
| POST | `/api/v1/admin/prompts/{id}/rollback` | 버전 롤백 |

### `type` 값

- `RELATION_ANALYSIS` — 관계 분석
- `RISK_DETECTION` — 리스크 탐지
- `INSIGHT_SUMMARY` — 인사이트 요약

## 응답 예시

**GET 성공**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "RELATION_ANALYSIS",
    "systemPrompt": "...",
    "taskTemplate": "...",
    "versions": [
      {
        "id": 2,
        "versionNumber": 2,
        "createdAt": "2026-05-21T10:00:00Z",
        "author": "관리자",
        "summary": "저장",
        "changeNote": "메모"
      }
    ]
  }
}
```

**POST test 성공**
```json
{
  "success": true,
  "data": {
    "output": "[테스트 실행 — 관계 분석]...",
    "tokenUsage": { "inputTokens": 120, "outputTokens": 80, "totalTokens": 200 },
    "companyName": "삼성전자"
  }
}
```

## 오류 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| ERR_ADMIN_PROMPT_001 | 404 | 프롬프트/리소스 없음 |
| ERR_ADMIN_PROMPT_002 | 400 | type·본문 검증 오류 |
| ERR_ADMIN_PROMPT_003 | 404 | 테스트 대상 기업 없음 |
| ERR_ADMIN_PROMPT_004 | 404 | 롤백 대상 버전 없음 |

## 공통 규약

- `Content-Type: application/json`
- 인증: Bearer JWT, `ROLE_ADMIN`
- 성공: `{ "success": true, "data": ... }`
- 실패: `{ "success": false, "error": { "code", "message" } }`
