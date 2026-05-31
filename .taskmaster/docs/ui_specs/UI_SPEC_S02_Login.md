# UI 명세 — S02 로그인

> **화면 ID**: S02  
> **레이아웃**: Guest Layout  
> **접근**: 비로그인 전용 (로그인 상태 → S01 리다이렉트)

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderGuest h-16 sticky ────────────────────────────────────┐
├─ MAIN: flex-1 flex items-center justify-center min-h-[calc(100vh-헤더-푸터)] ┤
│  ┌─ Card 컨테이너 max-w-[440px] w-[calc(100%-2rem)] mx-auto ─────────────┐ │
│  │  padding: 24px (p-6), radius Card 12px, border Light Cream 1px        │ │
│  │  배경 Cream Surface (카드와 페이지 동일 톤 가능, 경계는 border로)       │ │
│  │  ┌─ 제목: Section Heading 스케일 축소(36→28px 모바일) ────────────────┐ │ │
│  │  │  "로그인" Charcoal                                                   │ │ │
│  │  └────────────────────────────────────────────────────────────────────┘ │ │
│  │  [소셜 버튼 스택] gap-3, 전폭                                          │ │
│  │  [이메일 폼] mt-8 pt-8 border-t Light Cream                           │ │
│  │  링크: 비밀번호 찾기 → S04 (Link Small, Muted Gray)                     │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] MarketingFooter ───────────────────────────────────────────────┤
└──────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 | 비고 |
|------|------|------|
| 로그인 카드 | `max-w-[440px]`, 가로 `100% − 32px` | 중앙 정렬 |
| 소셜 버튼 | 높이 최소 44px (터치), `w-full` | 아이콘 + 라벨 |
| 이메일 입력 | `h-11`, radius Standard 6px | PRD 이메일 로그인 선택 제공 시 |

**반응형**

| 구간 | 변화 |
|------|------|
| `<600px` | 카드 `p-5`, 제목 28px; 소셜 버튼 세로 스택 유지 |
| `≥768px` | 카드 `p-8`, 여백 증가 |
| `≥1280px` | 배경에 미묘한 장식(옵션), 카드 폭 고정 440px |

---

## 컴포넌트 트리

```
LoginPage (S02)
├── [SHARED] AppHeaderGuest (로고만 또는 "뒤로" — 정책에 맞춤)
├── main
│   └── LoginCard
│       ├── LoginTitle (타이포: Sub-heading 축소)
│       ├── SocialLoginStack [SHARED]
│       │   ├── SocialProviderButton (Google → OAuth start)
│       │   ├── SocialProviderButton (Naver)
│       │   └── SocialProviderButton (Kakao)
│       ├── DividerWithLabel (또는 "또는" Muted Gray Caption)
│       ├── EmailLoginForm (조건부: 기능 플래그 on)
│       │   ├── TextField [SHARED] (email)
│       │   ├── TextField [SHARED] (password, type=password)
│       │   ├── InlineError (조건부: 인증 실패 메시지)
│       │   └── PrimaryButton [SHARED] (submit → POST /auth/login)
│       └── FooterLinks
│           └── Link (비밀번호 찾기 → S04)
└── [SHARED] MarketingFooter
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| idle | 초기 진입 | 소셜 + (옵션) 이메일 폼 |
| submitting | 로그인 요청 중 | Primary 버튼 `disabled` + 스피너, 입력 잠금 |
| error_invalid | 401/검증 실패 | InlineError (Charcoal 본문), 필드 border interactive |
| error_network | 네트워크 | [SHARED] `ErrorBanner` 카드 상단 + 재시도 |
| oauth_popup_blocked | 팝업 차단 | 토스트 안내 + "현재 탭에서 계속" CTA |
| redirect_if_authenticated | 이미 로그인 | 전체 화면 Skeleton → S01 replace |

---

## 인터랙션 규칙

- 소셜 버튼 클릭 → OAuth 리다이렉트/팝업 (**즉시**, provider별 URL)
- 이메일 로그인 submit → `POST /auth/login` 완료까지 버튼 로딩 (**opacity 0.8** active 스타일)
- 최초 소셜 가입 완료 콜백 → **S03** 약관 (라우트 **즉시**)
- 비밀번호 찾기 클릭 → **S04** (**150ms** hover underline만)
- 입력 포커스 → Ring Blue ring (**Focus Shadow**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| EmailLoginForm | `POST /auth/login` | body: `email`, `password` → 응답 `accessToken`, `refreshToken`, `user` (클라이언트 저장 정책) |
| SocialProviderButton | IdP OAuth URL (BFF 제공 시 `GET /auth/oauth/{provider}/url`) | `authorizationUrl` |
| (post-login) | 내부 프로필 | `termsAccepted`, `isNewUser` → S03 분기 |

---

## 디자인 토큰

- 카드: 배경 **Cream Surface**, **Light Cream** border, radius **12px**  
- 구분: **Light Cream** 상단 divider  
- 링크: **Charcoal** + underline, 보조 텍스트 **Muted Gray**
