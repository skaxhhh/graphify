# UI 명세 — S11 관심 기업 목록 / 비교

> **화면 ID**: S11  
> **레이아웃**: User App Layout  
> **PRD**: §6.3 관심 기업·비교

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderUser ───────────────────────────────────────────────────┐
├─ MAIN: max-w-[1200px] mx-auto px-4 md:px-8 py-8 flex flex-col gap-8 ────────┤
│  ┌─ 상단: 제목 + 안내 Caption (최대 3개 비교) ──────────────────────────────┐ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 목록 영역: grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 ───────┐ │
│  │  WatchlistCard [SHARED] — 각 카드: 체크박스(비교 선택), 기업명, 업종,    │ │
│  │  미니 지표, 관심 해제, 상세 링크                                         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ CompareDock sticky bottom-0 z-40 (조건부: 1~3개 선택 시) ──────────────┐ │
│  │  높이 auto, 배경 Cream + 상단 border Light Cream, p-4                    │ │
│  │  선택 칩 나열 | Primary "비교 보기" | Ghost "선택 해제"                  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ ComparePanel (조건부: 비교 보기 활성) ─────────────────────────────────┐ │
│  │  BasisTabs [SHARED] (투자 / 공급망 / 협력)                               │ │
│  │  grid grid-cols-1 md:grid-cols-3 gap-4 — 열당 InsightCompareColumn [SHARED]│ │
│  │  (데스크톱 3열, 태블릿 가로 스크롤 snap)                                  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] UserFooterSlim ──────────────────────────────────────────────────┤
└────────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 카드 그리드 | 1 / 2 / 3 열 (breakpoint 위 표) |
| CompareDock | `sticky bottom-0`, 전폭 부모 내 |
| 비교 열 | 각 **min-w-[280px]** (가로 스크롤 시) |

**반응형**

| 구간 | 변화 |
|------|------|
| `<768px` | 비교 패널 → **BottomSheet slide-up 320ms** ease-out 전체 표시 (SCREEN_FLOW 2-5) |
| `768px–1279px` | 비교 3열 → **가로 스크롤** `snap-x` |
| `≥1280px` | 3열 고정 그리드 |

---

## 컴포넌트 트리

```
WatchlistPage (S11)
├── [SHARED] AppHeaderUser
├── main
│   ├── PageTitleBlock
│   ├── WatchlistGrid
│   │   └── WatchlistCard [SHARED]
│   │       ├── CompareCheckbox (max 3, 초과 시 토스트)
│   │       ├── CompanySummary
│   │       ├── RemoveWatchButton → DELETE API
│   │       └── LinkToDetail → S07
│   ├── CompareDock [SHARED]
│   └── ComparePanel [SHARED]
│       ├── BasisTabs [SHARED]
│       └── InsightCompareColumn [SHARED] × 선택 수
└── [SHARED] UserFooterSlim
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 목록 fetch | 카드 Skeleton ×6 |
| empty | 관심 0건 | EmptyState + 검색 CTA → S06/S01 |
| populated | 1건 이상 | 그리드 |
| compare_ready | 체크 1~3 | CompareDock 표시 |
| compare_loading | 비교 API | 열 Skeleton |
| error | API 실패 | ErrorBanner |

---

## 인터랙션 규칙

- 4번째 기업 체크 시도 → 토스트 "최대 3개" (**즉시**, **200ms** fade)
- Basis 탭 변경 → debounce **200ms** → `GET /companies/compare` 재호출
- "비교 보기" → ComparePanel **slide-up 320ms** (모바일), 데스크톱은 인라인 확장 **250ms**
- 카드에서 상세 → S07 (**즉시**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| WatchlistGrid | `GET /watchlist/me` | `items[]`: `companyId`, `name`, `industry`, `addedAt` |
| InsightCompareColumn | `GET /companies/compare?ids=&basis=` | `companies[]`: 동일 basis의 `insightCards[]`, `metrics` |

---

## 디자인 토큰

- 카드: **Light Cream** border, radius **12px**, 내부 간격 **16–24px** (spacing scale)  
- Dock: 배경 **Cream**, 상단 **Light Cream** 1px  
- 탭: **Ghost** 스타일 기본, 선택 **Charcoal** 하단 보더 **2px** (시스템 일관)
