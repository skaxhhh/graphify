# UI 명세 — S07 기업 상세

> **화면 ID**: S07  
> **레이아웃**: Guest 또는 User App Layout  
> **연동 오버레이**: OV02 인사이트 근거 모달, OV06 신뢰도 기준, OV07 Agent 진행 토스트

---

## 레이아웃 구조

**User App Layout 기준**

```
┌─ [SHARED] AppHeaderUser h-16 sticky z-50 ──────────────────────────────────┐
│  Logo | GlobalSearchBar (flex-1 max-w-xl) | 🔔 | AvatarMenu                  │
├─ MAIN: max-w-[1200px] mx-auto px-4 md:px-8 py-8 ───────────────────────────┤
│  ┌─ 상단 히어로 카드: w-full, radius 12px, border Light Cream ────────────┐ │
│  │  flex flex-col lg:flex-row gap-8                                        │ │
│  │  좌: 기업명 Display 축소(36~48px) + 업종 Badge + 데이터품질배지 [SHARED]   │ │
│  │  우: CTA 행 — Primary "관계 그래프 보기" | Ghost "분석 이력" | WatchToggle│ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ 2열 그리드 lg:grid-cols-[1fr_360px] gap-8 items-start ─────────────────┐ │
│  │  좌 컬럼 (min-w-0):                                                       │ │
│  │    ┌─ 기본 정보 카드 ─────────────────────────────────────────────────┐ │ │
│  │    │ Body: 사업 요약, 재무 요약 테이블(옵션), 최신 업데이트 일자        │ │ │
│  │    └────────────────────────────────────────────────────────────────────┘ │ │
│  │    ┌─ 인사이트 요약 그리드 ───────────────────────────────────────────┐ │ │
│  │    │ grid 1 col sm:2 col gap-4 — InsightCard [SHARED] × ≥4             │ │ │
│  │    └────────────────────────────────────────────────────────────────────┘ │ │
│  │    ┌─ 리스크/기회 신호 리스트 [SHARED] SignalList ─────────────────────┐ │ │
│  │    └────────────────────────────────────────────────────────────────────┘ │ │
│  │  우 컬럼 lg:sticky lg:top-24 (데스크톱):                                  │ │
│  │    ┌─ 사이드 패널 w-full lg:w-[360px] ─────────────────────────────────┐ │ │
│  │    │ DataProvenanceCard [SHARED] (출처·MCP·면책 링크)                    │ │ │
│  │    │ DisclaimerCompact [SHARED] (AI 참고용)                             │ │ │
│  │    └────────────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] UserFooterSlim ──────────────────────────────────────────────────┤
└────────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 우측 사이드 | `360px` 고정 (`lg` 이상) |
| 메인 그리드 | `1fr` + `360px` |
| 상단 히어로 | `w-full`, 내부 `gap-8` |

**반응형**

| 구간 | 변화 |
|------|------|
| `<1024px` | 2열 → **단일 열**; 사이드 패널이 본문 **하단**으로 이동 (`order-2`) |
| `<768px` | CTA를 **세로 스택** 풀폭; 인사이트 카드 1열 |
| `≥1280px` | `max-w-[1200px]` 유지, 여백 증가 |

---

## 컴포넌트 트리

```
CompanyDetailPage (S07)
├── [SHARED] AppHeaderGuest | AppHeaderUser
├── main
│   ├── CompanyHeroCard
│   │   ├── CompanyTitleBlock (name, ticker)
│   │   ├── IndustryBadge [SHARED]
│   │   ├── DataFreshnessBadges [SHARED] (PRD §5, §8 — 최신성·커버리지)
│   │   └── HeroActions
│   │       ├── PrimaryButton → S08 (graph)
│   │       ├── GhostButton → S09 (조건부: 로그인)
│   │       └── WatchToggle [SHARED] (로그인: PUT watchlist, 비로그인: 로그인 유도 토스트)
│   ├── CompanyInfoSection (사업·재무)
│   ├── InsightCardGrid
│   │   └── InsightCard [SHARED] (props: type, title, summary, confidence, onDetail→OV02, onReliability→OV06)
│   ├── SignalList [SHARED] (신호 유형 라벨, 근거 노드 id)
│   ├── SidebarColumn
│   │   ├── DataProvenanceCard [SHARED]
│   │   └── DisclaimerCompact [SHARED]
│   └── (분석 시작 시) AgentProgressBridge → OV07 토스트 트리거
├── OV02 InsightEvidenceModal [SHARED] (포털)
├── OV06 ReliabilityCriteriaModal [SHARED]
└── [SHARED] UserFooterSlim | MarketingFooter
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | `companyId` 로드 | Hero Skeleton + 카드 Skeleton ×4 |
| error | `GET /companies/{id}` 실패 | ErrorBanner + 재시도 |
| populated | 성공 | 전체 레이아웃 |
| insights_loading | 인사이트 지연 로드 | 인사이트 영역만 Skeleton |
| insights_empty | 빈 배열 | Muted Gray 안내 + 그래프 CTA |
| guest_watchlist | 비로그인 저장 시도 | 로그인 모달 또는 S02 리다이렉트 정책 |

---

## 인터랙션 규칙

- "관계 그래프 보기" 클릭 → S08 (**즉시** navigate, 세션에 `fromDetail` 플래그)
- 인사이트 카드 "자세히" → OV02 **fade + scale 200ms** ease-out
- 신뢰도 라벨 클릭 → OV06
- 분석 이력 → S09 (비로그인 시 Ghost 버튼 비활성 + 툴팁)
- 카드 "그래프에서 보기"(PRD 6.1.3) → S08 + 하이라이트 payload 세션 저장 (**즉시**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| CompanyHeroCard / Info | `GET /companies/{id}` | `name`, `ticker`, `industry`, `summary`, `financials?`, `lastUpdated`, `coverageByRelationType` |
| InsightCardGrid | `GET /companies/{id}/insights` | `cards[]`: `id`, `type`, `title`, `summary`, `confidence`, `evidence`, `highlightNodeIds[]` |
| SignalList | 동일 응답 embedded 또는 `.../signals` | `signals[]`: `label`, `kind`, `relatedNodeIds`, `sources[]` |
| WatchToggle | `POST/DELETE /watchlist/{companyId}` (예시) | SCREEN_FLOW는 `GET /watchlist/me` — 토글 시 낙관적 UI |
| OV02 | 카드 객체 그대로 | `evidence`, `mcpToolTrace`, `rules`, `updatedAt` |

---

## 디자인 토큰

- 카드: **Cream Surface**, **Light Cream** border, radius **12px**  
- 제목: **Charcoal**, **Sub-heading** / **Card Title**  
- 배지: **Muted Gray** 텍스트, 배경 **Charcoal 3%**
