# API 명세 — Task 11: S10 분석 이력 상세·시계열 비교

> **우선순위**: P1  
> **구현 화면**: S10  
> **레이어**: Frontend=True, Backend=True, DB=True

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

- `GET /api/v1/history/{sessionId}` — JWT 필수, 본인 세션만
- `GET /api/v1/companies/{id}/graph?depth=2` — 비교 모드 시 현재 그래프

### GET /api/v1/history/{sessionId} 응답 `data`

| 필드 | 타입 | 설명 |
|------|------|------|
| sessionId | string (UUID) | 세션 ID |
| company | `{ id, name }` | 기업 |
| analyzedAt | ISO-8601 | 분석 완료 시각 |
| status | `COMPLETED` \| `FAILED` \| `RUNNING` | 상태 |
| summaryLine | string \| null | 한줄 요약 |
| timeline | `{ t, eventType, label, payload }[]` | 시계열 이벤트 |
| graphSnapshot | `{ nodes, edges }` | 분석 시점 그래프 |
| insights | InsightCard[] | 인사이트 스냅샷 |
| signals | Signal[] | 신호 스냅샷 |
| diffSummary | `{ text, generatedAt }` \| null | AI 트렌드 요약 |

오류: `ERR_HISTORY_002` (404) 세션 없음/권한 없음

## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
