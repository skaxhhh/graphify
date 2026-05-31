# UI 명세 — S06 검색 결과 목록

> **화면 ID**: S06  
> **레이아웃**: Guest Layout 또는 User App Layout (로그인 시 헤더에 글로벌 검색·알림·아바타)  
> **PRD**: §1 검색·정렬·필터, 1.3 최근 검색

---

## 레이아웃 구조

**Guest**

```
┌─ [SHARED] AppHeaderGuest ────────────────────────────────────────────────┐
├─ MAIN: max-w-[1200px] mx-auto px-4 md:px-8 py-8 flex flex-col gap-6 ───────┤
│  ┌─ 검색바 행: w-full ───────────────────────────────────────────────────┐ │
│  │  [SHARED] GlobalSearchBar (variant=inline, 기본값=q from URL)          │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 툴바: flex flex-wrap gap-3 justify-between items-center ─────────────┐ │
│  │  좌: 결과 건수 Caption Muted Gray | 우: SortSelect + FilterPopover      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 결과 리스트 영역 flex-1 min-h-[320px] ───────────────────────────────┐ │
│  │  [SHARED] CompanyResultRow × N (border-b Light Cream)                   │ │
│  │  Pagination [SHARED] (하단 중앙)                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] MarketingFooter ───────────────────────────────────────────────┤
└──────────────────────────────────────────────────────────────────────────┘
```

**User**: 헤더를 `[SHARED] AppHeaderUser`로 교체, 동일 메인 폭 규칙.

| 영역 | 크기 |
|------|------|
| 콘텐츠 최대 폭 | `1200px` |
| 결과 행 | 최소 높이 **72px**, 패딩 `py-4 px-4` |
| 필터 팝오버 | 최소 폭 **280px`**, `max-h-[70vh]` 스크롤 |

**반응형**

| 구간 | 변화 |
|------|------|
| `<768px` | 툴바 세로 스택 (`flex-col items-stretch`); Sort/Filter 전폭 드롭다운 |
| `≥768px` | 툴바 가로 정렬 |
| `≥1024px` | (옵션) 좌 280px 필터 사이드바 고정 — **1280px 미만**에서는 `FilterPopover`만 사용 |

---

## 컴포넌트 트리

```
SearchResultPage (S06)
├── [SHARED] AppHeaderGuest | AppHeaderUser
├── main
│   ├── GlobalSearchBar [SHARED] (controlled `q`, submit 유지 S06)
│   ├── SearchToolbar
│   │   ├── ResultCount (Caption)
│   │   ├── SortSelect [SHARED] (options: name, industry, updatedAt)
│   │   └── FilterPopover [SHARED] (업종/시장/데이터상태)
│   ├── SemanticSuggestionsBanner (조건부: Vector 유사 기업 안내)
│   ├── CompanyResultList
│   │   └── CompanyResultRow [SHARED] (클릭 → S07, 키보드 Enter)
│   └── Pagination [SHARED]
├── EmptyState [SHARED] (조건부: 0건)
└── [SHARED] MarketingFooter | UserFooterSlim
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | `q` 변경 후 fetch | 결과 영역 Skeleton 행 8개 |
| populated | 1건 이상 | 리스트 + 페이지네이션 |
| empty | 0건 | EmptyState + 유사어 제안(시맨틱) + 홈 CTA |
| error | API 실패 | ErrorBanner 상단 + 재시도 |
| filtering | 필터 적용 중 | 툴바에 작은 스피너 |

---

## 인터랙션 규칙

- 필터 변경 → debounce **300ms** → `GET /companies/search` 재호출 (PRD 3.1.2와 정렬)
- 정렬 변경 → **즉시** 재요청 (또는 debounce 150ms — 구현 일관만 유지)
- 행 클릭 → S07 `companyId` (**150ms** hover 배경 **Charcoal 3%**)
- 검색 실행 → URL `q` 동기화 + 로딩 (**즉시**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| GlobalSearchBar | `GET /search/autocomplete` | 동 S01 |
| CompanyResultList | `GET /companies/search?q=&sort=&filter=&page=` | 응답: `items[]`: `id`, `name`, `ticker`, `industry`, `market`, `dataFreshness`, `updatedAt`; `total`, `semanticHints?` |
| SemanticSuggestionsBanner | 동 응답 embedded | `relatedQueries[]`, `similarCompanies[]` |

---

## 디자인 토큰

- 행 구분: **Light Cream**  
- 보조 메타: **Muted Gray**, **Caption**  
- 카드형 행(옵션): radius **8px**, border **Light Cream**
