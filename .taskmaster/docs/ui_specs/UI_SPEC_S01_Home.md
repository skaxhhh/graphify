# UI 명세 — S01 홈 / 랜딩

> **화면 ID**: S01  
> **레이아웃**: Guest Layout (SCREEN_FLOW 3-1)  
> **스택**: React + Tailwind CSS  
> **토큰**: `design_system.md` (Cream, Charcoal, Light Cream, Muted Gray, 타이포 계층, spacing scale)

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderGuest h-16 sticky top-0 z-50 ─────────────────────────┐
│  max-w 전체 폭, px-4 md:px-8, 배경 Cream, 하단 border Light Cream 1px      │
│  좌: Logo 링크 | 우: [SHARED] GhostButton "로그인" (Primary Dark 옵션)    │
├──────────────────────────────────────────────────────────────────────────┤
│  MAIN: min-h-[calc(100vh-4rem-푸터)] flex flex-col items-center            │
│  ┌─ Hero 영역: py-24 md:py-32 (128px→모바일 64px 섹션 간격 규칙) ───────┐ │
│  │  max-w-[1200px] w-full px-4, 중앙 정렬 text-center                    │ │
│  │  Display Hero / Section Heading (Camera Plain, Charcoal)               │ │
│  │  Body Large (Muted Gray) 한 줄 가치 제안                               │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 검색 영역: w-full max-w-[720px] mt-8 ────────────────────────────────┐ │
│  │  [SHARED] GlobalSearchBar (variant: hero, 자동완성 드롭다운 포함)       │ │
│  │  하단: 최근 검색 [SHARED] RecentSearchChips (로그인 시만, max N)       │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 인사이트 2열: grid 1 col <1024 | 2 col ≥1024 ────────────────────────────┐ │
│  │  좌: TrendingCompaniesPanel (인기 조회 순위)                              │ │
│  │  우: MarketNewsPanel (시장 뉴스 스크롤)                                   │ │
│  └──────────────────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────┤
│  FOOTER: [SHARED] MarketingFooter (약관·개인정보·면책) border-t Light Cream │
└──────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기·단위 | Tailwind 대응(구현 힌트) |
|------|-----------|---------------------------|
| Header | 높이 64px 고정 | `h-16` |
| Hero 가로 | 최대 1200px 중앙 | `max-w-[1200px] mx-auto` |
| 검색바 컨테이너 | 최대 720px, 폭 100% | `max-w-[720px] w-full` |
| 메인 세로 | 뷰포트 − 헤더 − 푸터 | `min-h-[calc(100vh-theme)]` (푸터 높이 토큰화 권장) |

**반응형 breakpoint** (`design_system.md` §8 기준)

| 구간 | 레이아웃 변화 |
|------|----------------|
| `<768px` (Tablet 이하) | 내비게이션 → [SHARED] `MobileNav` 햄버거; Hero 타이포 Display 60px → 48px → 36px 스케일; 검색바 전폭 `px-4` |
| `768px–1024px` | 헤더 링크 노출 가능 시 가로 유지; 소개 카드 2열 |
| `≥1024px` | 인사이트 2열·Hero 여백 `py-32` 유지 |
| `≥1280px` | Large Desktop: 좌우 마진 generous, `max-w-[1200px]` 콘텐츠 고정 |

---

## 컴포넌트 트리

```
HomePage (S01)
├── [SHARED] AppHeaderGuest (로고 클릭 → S01, 로그인 → S02)
├── main
│   ├── HeroSection (서비스 한 줄 정의 + 서브카피, 타이포: Display Hero / Body Large)
│   ├── GlobalSearchBar [SHARED] (props: variant="hero", minQueryLength=2, onSubmit→S06)
│   │   ├── SearchInput (placeholder: Muted Gray, border Light Cream, focus Ring Blue)
│   │   ├── SearchSubmitButton (Primary Dark 스타일)
│   │   └── AutocompleteDropdown (조건부: 입력 ≥2자 & 포커스, 키보드 탐색)
│   ├── RecentSearchSection (조건부: 로그인 + 로컬/서버 이력 존재)
│   │   └── RecentSearchChips [SHARED] (항목 클릭 → S07 또는 검색 재실행)
│   └── HomeInsightsSection
│       ├── TrendingCompaniesPanel
│       └── MarketNewsPanel (max-h 스크롤)
└── [SHARED] MarketingFooter
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 초기 세션·약관 버전 조회 등 | Hero·검색 영역 [SHARED] `SkeletonBlock` 2~3개 |
| idle | 기본 | Hero + 검색바 + (로그인 시) 최근 검색 |
| autocomplete_loading | `q` 입력 후 디바운스 중 | 드롭다운 내 스피너 또는 소형 Skeleton 3줄 |
| autocomplete_empty | API 응답 0건 | 드롭다운 하단 Muted Gray 안내 + "전체 검색" CTA |
| autocomplete_error | 자동완성 API 실패 | [SHARED] `InlineError` 드롭다운 하단 + 재시도 링크 |
| guest_no_recent | 비로그인 | RecentSearchSection 미렌더 |
| oauth_redirecting | 소셜 콜백 후 홈 복귀 중 | 전역 얇은 progress bar (옵션) |

---

## 인터랙션 규칙

- 검색창 입력(≥2자) → debounce **250ms** → `GET /search/autocomplete` (timing: linear)
- 방향키 ↓↑ → 하이라이트 이동 (**즉시**, easing 없음)
- Enter (목록 포커스 시) → 선택 기업 **S07** 직행 / (미선택 시) → **S06** 목록
- 검색 버튼 클릭 → **S06** (`q` 쿼리 동기화)
- 자동완성 항목 클릭 → **S07** (`companyId`)
- 로그인 버튼 클릭 → **S02** (라우트 전환 **200ms** ease-out)
- 최근 검색 칩 삭제 → 로컬 스토리지 갱신 (**즉시** UI 반영)
- 로고 클릭 → S01 스크롤 탑 (**즉시**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint / 소스 | 필드 매핑 |
|----------|-----------------|-----------|
| GlobalSearchBar | `GET /search/autocomplete?q=` | 응답: `id`, `name`, `ticker?`, `matchType` → 드롭다운 행 |
| GlobalSearchBar (submit) | 라우팅만 또는 `GET /companies/search`는 S06에서 일괄 | `q` 쿼리스트링 |
| RecentSearchChips | 로컬 `localStorage` + (옵션) 서버 동기화 | `companyId`, `label`, `searchedAt` |
| (최초) TermsVersion | `GET /terms/latest` (S03 진입 조건 판별 시) | `version`, `required` |
| TrendingCompaniesPanel | `GET /home/trending-companies` | `rank`, `companyId`, `name`, `ticker`, `viewCount` |
| MarketNewsPanel | `GET /home/market-news` | `title`, `summary`, `sourceName`, `publishedAt`, `sourceUrl` |

**오버레이**: 없음 (OV는 다른 화면)

---

## 디자인 토큰 매핑

- 페이지 배경: **Cream**  
- 본문/제목: **Charcoal**, 보조: **Muted Gray**  
- 구분선: **Light Cream** (`#eceae4`)  
- CTA: Primary Dark (**Charcoal** 배경, **Off-White** 텍스트, inset shadow)  
- 포커스: **Ring Blue** + **Focus Shadow**
