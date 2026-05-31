# UI 명세 — S10 분석 이력 상세 / 시계열 비교

> **화면 ID**: S10  
> **레이아웃**: User App Layout  
> **PRD**: §12.2.2~3, §13 시계열

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderUser ───────────────────────────────────────────────────┐
├─ MAIN: max-w-[1400px] mx-auto px-4 md:px-8 py-6 ────────────────────────────┤
│  ┌─ 상단 메타 바: flex flex-wrap gap-4 items-center justify-between ─────┐ │
│  │  기업명 + 분석 일시 (Caption) | CompareToggle [SHARED] "현재와 비교"   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ TimelineSlider [SHARED] w-full py-4 ───────────────────────────────────┐ │
│  │  height track 48px, thumb 24px, 이벤트 마커 점 표시                     │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 그래프 영역: min-h-[420px] md:min-h-[520px] ───────────────────────────┐ │
│  │  Compare OFF: 단일 GraphSnapshotView [SHARED] (read-only)              │ │
│  │  Compare ON: grid grid-cols-1 lg:grid-cols-2 gap-4                      │ │
│  │    ├─ PastGraphPanel (flex-1, border Light Cream, radius 12px)          │ │
│  │    └─ CurrentGraphPanel (동일)                                         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 하단: 탭 또는 아코디언 ────────────────────────────────────────────────┐ │
│  │  InsightsSnapshot | SignalsSnapshot | DiffSummary (AI 텍스트)           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─ [SHARED] UserFooterSlim ───────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 최대 콘텐츠 | `1400px` |
| 타임라인 트랙 | 높이 **48px**, 전폭 |
| 비교 시 각 패널 | `1fr` (lg 2열), 최소 높이 **420px** |

**반응형**

| 구간 | 변화 |
|------|------|
| `<1024px` | 비교 모드: **상하 스택** (Past 상단, Current 하단), 스크롤 연동 옵션 |
| `<768px` | 타임라인 슬라이더 터치 영역 확대; 하단 탭 → **Select** 드롭다운 |

---

## 컴포넌트 트리

```
AnalysisHistoryDetailPage (S10)
├── [SHARED] AppHeaderUser
├── main
│   ├── SessionMetaBar (companyName, analyzedAt, status badge)
│   ├── CompareToggle [SHARED] (boolean — 레이아웃 전환)
│   ├── TimelineSlider [SHARED]
│   │   ├── SliderTrack
│   │   ├── EventMarkers [SHARED] (클릭 → EventDetailInline)
│   │   └── EventDetailInline (조건부: 마커 선택 시)
│   ├── GraphCompareLayout
│   │   ├── GraphSnapshotView [SHARED] (past, readOnly)
│   │   └── GraphLiveView [SHARED] (조건부: compare on — 현재 graph)
│   └── HistoryDetailTabs
│       ├── InsightsSnapshotList (과거 카드 읽기 전용)
│       ├── SignalsSnapshotList
│       └── DiffSummaryPanel (AI 트렌드 텍스트 PRD §13.2)
└── [SHARED] UserFooterSlim
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | sessionId 로드 | 전체 Skeleton (슬라이더+그래프+탭) |
| error | 세션 없음/권한 | ErrorBanner + S09 링크 |
| populated | 스냅샷 OK | 단일 그래프 |
| compare_loading | 토글 ON 후 현재 그래프 fetch | 우측 패널 Skeleton |
| timeline_scrubbing | 드래그 중 | 선택 시점 라벨 플로팅 업데이트 (**즉시**) |

---

## 인터랙션 규칙

- Compare 토글 ON → 현재 그래프 API 호출 + 패널 **slide 300ms** ease-out
- 타임라인 마커 클릭 → EventDetailInline **expand 200ms**
- 슬라이더 변경 → 스냅샷 시점 전환 (**150ms** debounce 후 데이터 바인딩)
- 뒤로 → S09 (**즉시**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| SessionMetaBar / Timeline | `GET /history/{sessionId}` | `company`, `analyzedAt`, `timeline[]`: `t`, `eventType`, `label`, `payload` |
| GraphSnapshotView | 동일 응답 | `graphSnapshot`: nodes/edges frozen |
| GraphLiveView | `GET /companies/{id}/graph` | 최신 그래프 (depth 기본값 정책) |
| DiffSummaryPanel | `session.diffSummary` 또는 `GET /history/{id}/diff` | `text`, `generatedAt` |

---

## 디자인 토큰

- 패널: **Light Cream** border, **Cream** 배경, radius **12px**  
- 마커: **Charcoal** 채움, 선택 시 **Focus Shadow**  
- 비교 레이블: **Caption** **Muted Gray** ("과거" / "현재")
