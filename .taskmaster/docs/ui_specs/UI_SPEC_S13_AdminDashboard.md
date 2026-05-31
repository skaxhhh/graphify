# UI 명세 — S13 관리자 홈 / Agent 실행 이력 대시보드

> **화면 ID**: S13  
> **레이아웃**: Admin Layout (좌 사이드바 `w-64` 접기 시 `w-16` + 상단 TopBar + 콘텐츠)

---

## 레이아웃 구조

```
┌─ SIDEBAR w-64 (collapsed w-16) border-r Light Cream bg Cream ─┬─ TOP BAR h-14 ─┐
│ • 대시보드 active                                             │ Logo | AdminBadge | SwitchToUser | Avatar │
│ • MCP 도구                                                    ├─ CONTENT: flex-1 overflow-auto p-6 md:p-8 ─┤
│ • 프롬프트                                                    │ max-w-[1600px] mx-auto w-full              │
│ • OpenAI 설정                                                 │ ┌─ KPI Cards grid cols-1 sm:2 lg:4 gap-4 ──┐ │
│ • Vector DB                                                   │ │ Card metric + sparkline 영역             │ │
│                                                               │ └──────────────────────────────────────────┘ │
│                                                               │ ┌─ Charts row: grid 1 xl:2 gap-6 ─────────┐ │
│                                                               │ │ Chart: 일별/주별 실행 수                  │ │
│                                                               │ │ Chart: 토큰 사용량 추이                   │ │
│                                                               │ └──────────────────────────────────────────┘ │
│                                                               │ ┌─ AlertsPanel + UsageTable ───────────────┐ │
│                                                               │ │ 좌: 이상 알림 목록 (scroll)               │ │
│                                                               │ │ 우: 사용자별 사용량 테이블                │ │
│                                                               │ └──────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┴──────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| Sidebar | `256px` / 접힘 `64px` |
| TopBar | `56px` (`h-14`) |
| KPI 카드 | 최소 높이 **120px** |
| 테이블 | 가로 스크롤 `min-w-0` + `overflow-x-auto` |

**반응형**

| 구간 | 변화 |
|------|------|
| `<1024px` | Sidebar → **오버레이 Drawer** (햄버거); 콘텐츠 전폭 |
| `≥1024px` | 고정 사이드바 + 2열 차트 |

---

## 컴포넌트 트리

```
AdminDashboardPage (S13)
├── AdminLayoutShell [SHARED]
│   ├── AdminSidebar [SHARED] (nav items)
│   ├── AdminTopBar [SHARED]
│   └── main
│       ├── KpiCardGrid [SHARED]
│       │   └── KpiCard ×4 (실행 수, 평균 시간, 토큰, 오류율)
│       ├── TrendChartsRow [SHARED]
│       │   ├── AgentRunsChart (period selector)
│       │   └── TokenUsageChart
│       ├── AnomalyAlertList [SHARED] (이상 감지)
│       └── UserUsageTable [SHARED]
│           └── Row drill-down → session detail drawer (옵션)
└── (포털) SessionDetailDrawer [SHARED] (선택)
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 대시보드 데이터 | KPI Skeleton ×4 + 차트 Skeleton |
| populated | 성공 | 전체 위젯 |
| partial_error | 일부 위젯 실패 | 위젯별 InlineError |
| empty_alerts | 이상 없음 | Muted Gray 한 줄 안내 |

---

## 인터랙션 규칙

- 기간 토글 (일/주) → debounce **200ms** → stats 재조회
- 테이블 정렬 헤더 클릭 → **즉시** 재정렬 (클라이언트 또는 서버 `sort=`)
- 행 클릭 → 세션 상세 Drawer **slide-in 300ms** ease-out
- "사용자 서비스로" → 메인 앱 루트 (**즉시**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| KpiCardGrid / Charts | `GET /admin/agent/stats?period=` | `runCount`, `avgDurationMs`, `tokenUsage`, `errorRate`, `series[]` |
| UserUsageTable | `GET /admin/users/usage` | `rows[]`: `userId`, `name`, `requests`, `tokens`, `errors` |
| AnomalyAlertList | 동 stats 또는 `GET /admin/alerts` | `alerts[]`: `severity`, `message`, `detectedAt` |
| SessionDetailDrawer | `GET /admin/agent/sessions/{id}` (예시) | 로그 단계, MCP 호출, 토큰 |

---

## 디자인 토큰

- 관리자 배경: **Cream** 유지, 데이터 밀도 높은 표는 **Light Cream** 행 구분  
- 숫자 강조: **Section Heading** 스케일 축소 + **Charcoal**  
- 차트 축/그리드: **Muted Gray** / **Light Cream**
