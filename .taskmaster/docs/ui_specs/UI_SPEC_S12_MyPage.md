# UI 명세 — S12 마이페이지 (계정 설정)

> **화면 ID**: S12  
> **레이아웃**: User App Layout  
> **오버레이**: OV05 로그아웃 확인

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderUser ───────────────────────────────────────────────────┐
├─ MAIN: max-w-[720px] mx-auto px-4 md:px-8 py-10 ────────────────────────────┤
│  ┌─ 프로필 요약 카드 (border Light Cream, radius 12px, p-6) ─────────────┐ │
│  │  Avatar [SHARED] | displayName | email (Muted Caption)                  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ Section: 비밀번호 변경 (조건부: 이메일 로그인) ──────────────────────┐ │
│  │  PasswordChangeForm [SHARED]                                            │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ Section: 커스텀 프롬프트 (조건부: Premium) ────────────────────────────┐ │
│  │  PromptEditor [SHARED] (textarea min-h-[160px], 저장 버튼)              │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌─ Section: 계정 ─────────────────────────────────────────────────────────┐ │
│  │  DangerZone: 로그아웃 GhostButton | TextLink 회원탈퇴                    │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] UserFooterSlim ──────────────────────────────────────────────────┤
└────────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 콘텐츠 최대 폭 | `720px` |
| 프롬프트 에디터 | `min-h-[160px]`, `w-full` |

**반응형**: `<600px`에서 섹션 간 `gap-8` → `gap-6`; 버튼 풀폭.

---

## 컴포넌트 트리

```
MyPage (S12)
├── [SHARED] AppHeaderUser
├── main
│   ├── ProfileSummaryCard
│   │   ├── Avatar [SHARED]
│   │   └── UserTextBlock (GET /users/me)
│   ├── PasswordChangeForm [SHARED] (조건부: `authProvider===email`)
│   │   ├── TextField currentPassword
│   │   ├── TextField newPassword
│   │   ├── TextField confirmPassword
│   │   ├── PolicyHints (Caption)
│   │   └── PrimaryButton → PUT password
│   ├── PremiumPromptSection (조건부: Premium)
│   │   ├── PromptEditor [SHARED]
│   │   └── PrimaryButton → PUT prompt
│   └── AccountActions
│       ├── GhostButton "로그아웃" → OV05
│       └── TextLink "회원 탈퇴" → (별도 플로우)
├── OV05 LogoutConfirmDialog [SHARED]
└── [SHARED] UserFooterSlim
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | `/users/me` | 프로필 Skeleton |
| populated | 성공 | 전체 섹션 |
| password_submitting | 변경 요청 | 폼 disabled + 스피너 |
| password_success | 200 | 성공 토스트 + (정책) 재로그인 안내 |
| prompt_saving | Premium 저장 중 | 에디터 하단 로딩 바 |
| error | API 실패 | ErrorBanner |

---

## 인터랙션 규칙

- 로그아웃 클릭 → OV05 **fade 150ms**  
- OV05 확인 → 세션 삭제 + **S02** replace (**즉시**)
- 비밀번호 변경 성공 → 필드 클리어 + 토스트 **2s** 표시
- 프롬프트 저장 → debounce **500ms** (자동 저장 옵션) 또는 수동 클릭만

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| ProfileSummaryCard | `GET /users/me` | `displayName`, `email`, `authProvider`, `isPremium` |
| PasswordChangeForm | `PUT /users/me/password` | body: `currentPassword`, `newPassword` |
| PromptEditor | `PUT /users/me/prompt` | body: `customPrompt` |
| Logout | `POST /auth/logout` (예시) | void |

---

## 디자인 토큰

- 섹션 제목: **Card Title** **Charcoal**  
- 보조: **Muted Gray** **Caption**  
- Danger 영역: 텍스트만 **Charcoal**, 버튼은 Ghost (**Charcoal 40%** border)
