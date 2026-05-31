# UI 명세 — S17 Vector DB 관리

> **화면 ID**: S17  
> **레이아웃**: Admin Layout

---

## 레이아웃 구조

```
┌─ AdminSidebar ─┬─ AdminTopBar ─────────────────────────────────────────────┐
│                ├─ MAIN max-w-[1400px] mx-auto p-6 md:p-8 space-y-8 ─────────┤
│                │ ┌─ Dashboard grid: 1 sm:2 lg:4 gap-4 ──────────────────────┐ │
│                │ │ StatCard: 총 벡터 수 | 유형별 분포 | 인덱스 크기 | 최근 작업 │ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ Two column lg:grid-cols-2 gap-8 items-start ──────────────┐ │
│                │ │ 좌: ReindexPanel Card                                      │ │
│                │ │   라디오: 전체 / 선택 | 멀티셀렉트 | Primary "재임베딩 실행" │ │
│                │ │ 우: PerformancePanel Card                                  │ │
│                │ │   평균 응답시간, 평균 유사도, 요청 수 차트                  │ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ CleanupPanel Card w-full ─────────────────────────────────┐ │
│                │ │ 만료 기준 설정 + 삭제 미리보기 + Danger Primary 실행        │ │
│                │ └──────────────────────────────────────────────────────────┘ │
└────────────────┴────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| Stat 카드 | 동일 높이 **min-h-[100px]** |
| 재임베딩 패널 | 좌열 `1fr` |
| 성능 패널 | 우열 `1fr` |

**반응형**

| 구간 | 변화 |
|------|------|
| `<1024px` | 2열 패널 → **세로 스택**; 상단 KPI 2×2 그리드 |
| `<640px` | KPI **1열** 스택 |

---

## 컴포넌트 트리

```
AdminVectorDbPage (S17)
├── AdminLayoutShell [SHARED]
│   ├── AdminSidebar
│   ├── AdminTopBar
│   └── main
│       ├── VectorStatsDashboard [SHARED]
│       │   └── StatCard ×4
│       ├── ReindexPanel [SHARED]
│       │   ├── ScopeRadio (all | selected)
│       │   ├── EntityMultiSelect [SHARED] (조건부)
│       │   ├── PrimaryButton → POST reindex (job id 반환)
│       │   └── JobProgressBanner [SHARED] (폴링 상태)
│       ├── PerformancePanel [SHARED]
│       │   └── MiniCharts (latency, similarity, qps)
│       └── CleanupPanel [SHARED]
│           ├── RetentionRuleForm (days, types)
│           ├── PreviewTable (삭제 대상 건수)
│           └── DangerButton → DELETE cleanup (confirm dialog)
└── ConfirmDestructiveDialog [SHARED]
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | stats | 대시보드 Skeleton |
| job_running | 재임베딩 | ProgressBanner + 비활성화 |
| job_success | 완료 | 토스트 + 수치 갱신 |
| job_error | 실패 | ErrorBanner |
| cleanup_preview_loading | 규칙 변경 | Preview Skeleton |
| empty_selection | 선택 모드인데 0건 | Primary 비활성 + 인라인 안내 |

---

## 인터랙션 규칙

- 재임베딩 실행 → 확인 Dialog **fade 150ms** → POST → Progress **폴링 2s** 간격  
- 삭제 실행 → 2단계 확인 (텍스트 입력 검증 옵션)  
- 차트 기간 변경 → **300ms** debounce

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| VectorStatsDashboard | `GET /admin/vectordb/stats` | `totalVectors`, `byType{}`, `indexSizeBytes`, `lastJobs[]` |
| ReindexPanel | `POST /admin/vectordb/reindex` | body: `scope`, `targetIds?` → `jobId` |
| Job status (폴링) | `GET /admin/vectordb/jobs/{jobId}` (예시) | `status`, `progress`, `message` |
| PerformancePanel | stats embedded 또는 `.../metrics` | `avgLatencyMs`, `avgSimilarity`, `requestCount` |
| CleanupPanel | `DELETE /admin/vectordb/cleanup` | query/body: `olderThanDays`, `types[]` — 응답 `deletedCount` |

---

## 디자인 토큰

- 위험 액션: 버튼은 Ghost/Danger 변형 — 본문 **Charcoal**, 테두리 **Charcoal 40%**, 배경은 과도한 적색 지양 (디자인 시스템 톤 유지)  
- 성공/진행: **Muted Gray** 텍스트 + 얇은 progress bar **Light Cream** 트랙 위 **Charcoal** fill
