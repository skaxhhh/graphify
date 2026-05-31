# UI 명세 — S03 약관 동의

> **화면 ID**: S03  
> **레이아웃**: Guest Layout (소셜 최초 가입 직후만)  
> **PRD**: 9.1.2 최초 로그인 시 약관 동의

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderGuest (최소: 로고만, 로그인 버튼 숨김 권장) ────────────┐
├─ MAIN: flex-1 flex flex-col items-center py-16 md:py-24 ─────────────────┤
│  ┌─ max-w-[560px] w-full px-4 ───────────────────────────────────────────┐ │
│  │  Card: border Light Cream, radius 12px, p-6 md:p-8                     │ │
│  │  ┌─ 안내 Body (Muted Gray): "서비스 이용을 위해 약관에 동의해 주세요"   │ │
│  │  ┌─ [SHARED] CheckboxRow "필수 약관 전체 동의" (master toggle) ───────┐ │ │
│  │  ├── ScrollArea max-h-[40vh] border Light Cream rounded-md p-4 (옵션) │ │ │
│  │  │     개별 약관 목록 (서비스 이용약관, 개인정보 처리방침)              │ │ │
│  │  │     각 행: 체크박스 + "본문 보기" 링크 → OV 모달 아닌 인라인 모달   │ │ │
│  │  └────────────────────────────────────────────────────────────────────┘ │ │
│  │  PrimaryButton [SHARED] "동의하고 시작하기" w-full mt-8                 │ │
│  │  GhostButton "뒤로" (로그인 화면 S02) mt-3 w-full                       │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] MarketingFooter ───────────────────────────────────────────────┤
└──────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 카드 최대 폭 | `560px` |
| 약관 스크롤 영역 | `max-h-[40vh]` (모바일), `max-h-[320px]` (데스크톱 옵션) |
| 버튼 | `w-full`, 최소 높이 44px |

**반응형**

| 구간 | 변화 |
|------|------|
| `<768px` | 카드 전폭 마진 16px; 스크롤 영역 `40vh` 유지로 접기 쉬움 |
| `≥768px` | `max-w-[560px]` 중앙, 패딩 `p-8` |
| 키보드 열림 (모바일) | `pb-safe` + 스크롤 영역 축소로 CTA 항상 노출 |

---

## 컴포넌트 트리

```
TermsConsentPage (S03)
├── [SHARED] AppHeaderGuest
├── main
│   └── TermsConsentCard
│       ├── ConsentIntro (Body, Muted Gray)
│       ├── MasterConsentCheckbox [SHARED] (전체 동의 토글 → 자식 동기화)
│       ├── TermsItemList
│       │   └── TermsItem (반복)
│       │       ├── Checkbox [SHARED] (필수 여부 props: required)
│       │       └── TextButton "본문 보기" → TermsDetailModal [SHARED]
│       ├── ConsentErrorBanner (조건부: 미동의 제출 시)
│       ├── PrimaryButton [SHARED] (동의 완료 → POST /auth/consent → S01)
│       └── GhostButton (뒤로 → S02, 세션 정리 정책과 연동)
├── TermsDetailModal [SHARED] (조건부: 약관 본문)
│   └── ScrollableTermsBody (GET /terms/latest HTML 또는 markdown)
└── [SHARED] MarketingFooter
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 약관 목록 로드 | 카드 내 Skeleton 4줄 + 체크박스 placeholder |
| idle | 로드 완료 | 마스터/개별 체크박스 초기 해제 |
| partial | 일부만 동의 | Primary 버튼 활성 정책: 필수 전부 체크 시만 활성 |
| submitting | 동의 API 호출 중 | Primary 로딩, 체크박스 disabled |
| error | API 실패 | ErrorBanner + 재시도 |
| success | 동의 완료 | 짧은 토스트 후 S01 replace |

---

## 인터랙션 규칙

- 마스터 체크 클릭 → 모든 필수 항목 **즉시** 동기화
- 개별 체크 변경 → 마스터 상태 재계산 (**즉시**)
- "본문 보기" → 모달 fade-in **200ms** ease-out, 배경 dim **Charcoal 3%** 오버레이
- 모달 닫기 → **150ms** ease-in
- 동의 완료 클릭 → 필수 미충족 시 ConsentErrorBanner slide-down **200ms**
- 뒤로가기 → S02 (**즉시** navigation)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| TermsItemList / Modal | `GET /terms/latest` | `terms[]`: `id`, `title`, `type`, `contentUrl` 또는 `html`, `required` |
| PrimaryButton submit | `POST /auth/consent` | body: `acceptedTermIds[]`, `version` |

---

## 디자인 토큰

- 본문: **Body** + **Muted Gray**  
- 제목: **Charcoal**, **Sub-heading** 스케일  
- 경계: **Light Cream**; 포커스: **Ring Blue**
