# API 명세 — Task 18: S17 Vector DB 관리

> **구현 화면**: S17  
> **Base path**: `/api/v1/admin/vectordb`

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/stats` | 인덱스 통계·성능 시계열·최근 작업 |
| POST | `/reindex` | 재임베딩 작업 시작 → `jobId` |
| GET | `/jobs/{jobId}` | 작업 진행 상태 (폴링, 진행 시 자동 갱신) |
| GET | `/cleanup/preview` | 삭제 미리보기 건수 |
| DELETE | `/cleanup` | 만료 벡터 삭제 실행 |

## GET /stats 응답

```json
{
  "success": true,
  "data": {
    "totalVectors": 12840,
    "byType": { "COMPANY": 5200, "INSIGHT": 4100, "RELATION": 3540 },
    "indexSizeBytes": 268435456,
    "avgLatencyMs": 142.5,
    "avgSimilarity": 0.872,
    "requestCount24h": 1842,
    "latencySeries": [120, 135, 128],
    "similaritySeries": [0.85, 0.86, 0.87],
    "requestSeries": [120, 140, 130],
    "lastJobs": [
      {
        "id": 1,
        "jobType": "REINDEX",
        "scope": "ALL",
        "status": "SUCCESS",
        "progress": 100,
        "message": "전체 재임베딩 완료",
        "createdAt": "2026-05-19T10:00:00Z",
        "completedAt": "2026-05-19T10:00:30Z"
      }
    ],
    "updatedAt": "2026-05-21T12:00:00Z"
  }
}
```

## POST /reindex body

| 필드 | 필수 | 설명 |
|------|------|------|
| scope | Y | `ALL` \| `SELECTED` |
| targetIds | 조건부 | `SELECTED`일 때 기업 ID 배열 |

응답:

```json
{ "success": true, "data": { "jobId": 3 } }
```

## GET /jobs/{jobId} 응답

```json
{
  "success": true,
  "data": {
    "jobId": 3,
    "jobType": "REINDEX",
    "scope": "ALL",
    "status": "RUNNING",
    "progress": 45,
    "message": "임베딩을 생성 중입니다.",
    "createdAt": "2026-05-21T12:00:00Z",
    "completedAt": null
  }
}
```

`status`: `PENDING` | `RUNNING` | `SUCCESS` | `FAILED`

## GET /cleanup/preview

Query: `olderThanDays` (1–3650), `types` (쉼표 구분, 예: `COMPANY,INSIGHT`)

```json
{ "success": true, "data": { "previewCount": 420 } }
```

## DELETE /cleanup body

```json
{ "olderThanDays": 90, "types": ["COMPANY", "INSIGHT"] }
```

응답:

```json
{ "success": true, "data": { "deletedCount": 420 } }
```

## 오류 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| ERR_ADMIN_VECTOR_001 | 404 | 작업·통계 없음 |
| ERR_ADMIN_VECTOR_002 | 400 | 검증 실패 (scope, targetIds, types 등) |

## 공통

- Bearer JWT, `ROLE_ADMIN`
