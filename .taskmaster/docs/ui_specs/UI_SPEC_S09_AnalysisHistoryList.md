# UI 명세 — S09 분석 이력 목록

> **화면 ID**: S09  
> **레이아웃**: User App Layout 전용 (로그인)  
> **PRD**: §12.2 사용자 분석 이력

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderUser h-16 sticky ───────────────────────────────────────┐
├─ MAIN: max-w-[960px] mx-auto px-4 md:px-8 py-8 ────────────────────────────┤
│  ┌─ 페이지 헤더 flex justify-between items-end gap-4 flex-wrap ──────────┐ │
│  │  좌: 제목 Section Heading "분석 이력"                                   │ │
│  │  우: SearchField (기업명) + DateRangeFilter [SHARED] + ExportMenu(Premium)│ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 테이블/카드 hybrid ────────────────────────────────────────────────────┐ │
│  │  md+: Table [SHARED] (columns: 기업명, 일시, 상태, 한줄요약, 액션)        │ │
│  │  <md: HistoryCard stack (tap → S10)                                     │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ Pagination [SHARED] ───────────────────────────────────────────────────┐ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] UserFooterSlim ──────────────────────────────────────────────────┤
└────────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 콘텐츠 최대 폭 | `960px` (읽기 편의 우선) |
| 테이블 행 높이 | `56px` 이상 |
| 모바일 카드 | `w-full`, `p-4`, radius **12px** |

**반응형**

| 구간 | 변화 |
|------|------|
| `<768px` | 테이블 → 카드 리스트; 필터는 **BottomSheet** |
| `≥768px` | 테이블 전개; 헤더 툴 한 줄 |

---

## 컴포넌트 트리

```
AnalysisHistoryListPage (S09)
├── [SHARED] AppHeaderUser
├── main
│   ├── PageHeader
│   │   ├── PageTitle
│   │   ├── HistorySearchBar [SHARED] (local filter 또는 query param)
│   │   ├── DateRangeFilter [SHARED]
│   │   └── ExportMenu [SHARED] (조건부: Premium — CSV/PDF)
│   ├── HistoryTableDesktop [SHARED] (md+)
│   │   └── Row → navigate S10(sessionId)
│   ├── HistoryCardListMobile (<md)
│   └── Pagination [SHARED]
└── [SHARED] UserFooterSlim
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 페이지 진입 / 페이지 변경 | 테이블 Skeleton 6행 |
| populated | 데이터 존재 | 테이블 또는 카드 |
| empty | 필터 결과 0 | EmptyState + 필터 초기화 CTA |
| error | API 실패 | ErrorBanner + 재시도 |
| export_loading |보내기 요청 | ExportMenu에 스피너 |

---

## 인터랙션 규칙

- 행/카드 클릭 → S10 (**즉시**)
- 검색 입력 → debounce **300ms** → 클라이언트 필터 또는 `?q=` 재호출
- 기간 변경 → **즉시** `GET` 재요청
- CSV/PDF 클릭 → 비동기 job 폴링 또는 다운로드 (**버튼 로딩**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| HistoryTable / Cards | `GET /history/me?page=&size=&from=&to=&q=` | `items[]`: `sessionId`, `companyId`, `companyName`, `analyzedAt`, `status`, `summaryLine` |
| ExportMenu | `POST /history/export` (예시) | body: `format`, `range` — 응답 `downloadUrl` |

---

## 디자인 토큰

- 표 헤더: **Caption**, **Muted Gray**, 하단 border **Light Cream**  
- 행 hover: 배경 **Charcoal 4%**  
- Premium 뱃지: 시스템 보조색 최소 사용 — **Charcoal** 테두리 Ghost 스타일 권장
