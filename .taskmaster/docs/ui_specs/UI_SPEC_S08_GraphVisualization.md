# UI 명세 — S08 관계 그래프 시각화

> **화면 ID**: S08  
> **레이아웃**: User/Guest — **SCREEN_FLOW 3-2**: 헤더 유지 + **좌 필터 패널** + **메인 캔버스** + **우 인사이트 패널**  
> **오버레이**: OV01 노드 팝업, OV02, OV06, OV07, OV08 클러스터

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderUser | AppHeaderGuest (h-16 sticky) ────────────────────┐
├─ SUBHEADER toolbar h-12 border-b Light Cream px-4 flex items-center gap-3 ─┤
│  뒤로(S07) | 깊이 Stepper 1–3 | 관계필터 요약 | 초기화 Ghost | 데이터출처 아이콘 │
├─ flex flex-1 min-h-[calc(100vh-4rem-3rem)] relative ───────────────────────┤
│ ┌─ LEFT PANEL ─┬─ GRAPH CANVAS ───────────────┬─ RIGHT PANEL ─────────────┐ │
│ │ w-72 shrink-0│ flex-1 min-w-0               │ w-96 shrink-0            │ │
│ │ hidden <1280 │ h-[calc(100vh-10rem)] min-h   │ hidden <1280             │ │
│ │ lg:flex      │ [480px] 기준 캔버스          │ lg:flex flex-col         │ │
│ │ flex-col     │                              │                          │ │
│ │ border-r     │                              │ border-l Light Cream     │ │
│ │ Light Cream  │ 중앙: GraphView              │ 인사이트 카드 스크롤      │ │
│ │              │ 우하단: ZoomControls         │ 하단: ProvenanceSummary   │ │
│ │ RelationFilter│ 미니맵(옵션) overlay        │                          │ │
│ │ DepthControl │                              │                          │ │
│ │ DimModeToggle│                              │                          │ │
│ └──────────────┴──────────────────────────────┴──────────────────────────┘ │
└─ (모바일) 좌·우 패널: BottomSheet / Drawer 트리거 FAB ──────────────────────┘
```

| 영역 | 크기·단위 |
|------|-----------|
| 좌 패널 | `288px` (`w-72`), `100%` 높이 부모 기준 |
| 그래프 | `flex-1`, 높이 `calc(100vh - 헤더 - 서브헤더)` |
| 우 패널 | `384px` (`w-96`) |
| 서브헤더 | `48px` (`h-12`) |

**반응형 breakpoint**

| 구간 | 변화 |
|------|------|
| `<1280px` | **LEFT/RIGHT 패널 → 하단 BottomSheet** (필터 FAB, 인사이트 FAB); 캔버스 **전폭** `flex-1` |
| `<768px` | 서브헤더: 일부 컨트롤 **오버플로 메뉴**에 수납; 줌은 핀치 + 버튼 |
| `≥1280px` | 3열 고정, 패널 스크롤 독립 (`overflow-y-auto`) |

---

## 컴포넌트 트리

```
GraphVisualizationPage (S08)
├── [SHARED] AppHeaderGuest | AppHeaderUser
├── GraphSubheader
│   ├── BackButton → S07 (히스토리 복원)
│   ├── DepthStepper [SHARED] (1~3, PRD §7.1)
│   ├── ResetViewButton (초기화 — PRD §7.3)
│   └── DataSourceIconButton → Provenance popover
├── GraphShell
│   ├── GraphLeftPanel [SHARED] (lg+)
│   │   ├── RelationTypeFilter [SHARED] (다중 선택 + 전체, debounce 대상)
│   │   ├── NonSelectedDimModeToggle (숨김/흐림)
│   │   └── SessionFilterPersistenceNote (Caption, Muted)
│   ├── GraphCanvas
│   │   ├── GraphView [SHARED] (React Flow/D3 — 노드/엣지, 휠 줌, 드래그 팬)
│   │   ├── GraphTooltipLayer [SHARED] (호버 툴팁 PRD §2.3)
│   │   ├── ZoomControls [SHARED] (+/−, fit view)
│   │   └── Minimap [SHARED] (조건부: 기능 on)
│   ├── GraphRightPanel [SHARED] (lg+)
│   │   ├── InsightCardStack (S07과 동일 카드 축약版)
│   │   └── ProvenanceFooter [SHARED]
│   ├── MobilePanelHost
│   │   ├── FilterBottomSheet [SHARED] (트리거: FAB "필터")
│   │   └── InsightsBottomSheet [SHARED] (트리거: FAB "인사이트")
│   └── OV01 NodeCompanyPopover [SHARED] (앵커드)
│   └── OV08 ClusterMembersPopover [SHARED]
├── StreamingStatusDock (SSE 메시지 — PRD §2.1.1, OV07과 병행 가능)
├── OV02, OV06 (포털)
└── (전역) OV07 AgentProgressToast [SHARED]
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 그래프 fetch | 캔버스 전면 Skeleton + "단계별" 스트리밍 텍스트 영역 |
| streaming | `SSE /agent/stream` | 상단 배너 또는 좌측 로그 패널에 청크 append |
| populated | 데이터 수신 완료 | 그래프 인터랙션 활성 |
| empty | 노드 0 | 캔버스 중앙 EmptyState + 재분석 CTA |
| error | API/SSE 실패 | ErrorBanner + 재시도; SSE 끊김 시 재연결 버튼 |
| depth_loading | 깊이 변경 | 캔버스 반투명 오버레이 + 스피너 (이전 요청 cancel) |

---

## 인터랙션 규칙

- 관계 필터 변경 → debounce **250ms** → 클라이언트 필터 또는 API `filter=` 재조회 (PRD 200~300ms 범위 내 **280ms**)
- 휠 → 줌 **지수 감쇠** (라이브러리 기본), 버튼 ± → **150ms** ease-out
- 노드 클릭 → OV01 **popover 위치 보정** (flip/shift, **즉시**)
- 클러스터 노드 클릭 → OV08
- "초기화" → 확대/이동/필터/하이라이트 기본값 (**즉시** + 확인 dialog 옵션)
- 카드 "그래프에서 보기" → 관련 노드/엣지 **highlight pulse 600ms** ease-in-out

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| GraphView | `GET /companies/{id}/graph?depth=&filter=` | `nodes[]`: `id`, `label`, `type`, `summary`, `degree`, `clusterId?`; `edges[]`: `source`, `target`, `relationType`, `strength`, `evidence`, `updatedAt` |
| StreamingStatusDock | `SSE /agent/stream/{sessionId}` | 이벤트: `stage`, `message`, `progress?` |
| InsightCardStack | `GET /companies/{id}/insights` (캐시/병렬) | 동 S07 |
| ProvenanceFooter | `GET /companies/{id}` 또는 graph embedded | `sources[]`, `lastUpdated`, `mcpToolsUsed[]` |

---

## 디자인 토큰

- 패널 배경: **Cream** / 구분 **Light Cream**  
- 그래프 위 UI: 반투명 패널 **Charcoal 3%** 배경 + **Light Cream** border  
- 강조 엣지: 비포화 색은 제품 팔레트 외 — **Charcoal** 계열 굵기·opacity로 구분 (디자인 시스템 준수)
