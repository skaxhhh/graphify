# UI 명세 — S04 비밀번호 재설정 요청

> **화면 ID**: S04  
> **레이아웃**: Guest Layout  
> **PRD**: 9.4.1 재설정 이메일 발송

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderGuest ────────────────────────────────────────────────┐
├─ MAIN: flex-1 flex items-center justify-center py-12 md:py-20 ───────────┤
│  ┌─ max-w-[440px] w-[calc(100%-2rem)] ───────────────────────────────────┐ │
│  │  Card: p-6, radius 12px, border Light Cream                            │ │
│  │  제목 (Sub-heading 축소): "비밀번호 재설정"                              │ │
│  │  설명 (Body, Muted Gray): 가입 이메일 입력 안내                        │ │
│  │  [SHARED] TextField email, w-full                                       │ │
│  │  PrimaryButton [SHARED] "재설정 메일 보내기" w-full mt-6                │ │
│  │  GhostButton "로그인으로 돌아가기" → S02 mt-3 w-full                   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] MarketingFooter ───────────────────────────────────────────────┤
└──────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 카드 | `max-w-[440px]` |
| 입력 | `h-11`, `w-full` |

**반응형**: `<600px`에서 카드 `p-5`, 버튼 세로 풀폭 동일. `≥768px` `p-8`.

---

## 컴포넌트 트리

```
PasswordResetRequestPage (S04)
├── [SHARED] AppHeaderGuest
├── main
│   └── ResetRequestCard
│       ├── PageTitle
│       ├── HelperText (Muted Gray, Caption~Body)
│       ├── TextField [SHARED] (props: type=email, autoComplete=email)
│       ├── FieldError (조건부: 형식 오류)
│       ├── PrimaryButton [SHARED] (submit)
│       ├── SuccessNotice (조건부: 발송 완료 — Body, border Light Cream bg Cream)
│       └── LinkButton → S02
└── [SHARED] MarketingFooter
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| idle | 진입 | 입력 + CTA |
| validating | blur/submit 전 클라이언트 검증 | 인라인 에러 |
| submitting | `POST` 진행 | 버튼 로딩, 입력 disabled |
| success | 200 응답 | 성공 박스 + "로그인으로" CTA (이메일 노출은 보안상 마스킹 권장) |
| error | 4xx/5xx | ErrorBanner + 재시도 |
| rate_limited | 429 | Muted Gray 안내 + 재시도 가능 시각 표시 |

---

## 인터랙션 규칙

- Submit 클릭 → 클라이언트 이메일 형식 검증 실패 시 FieldError **즉시**
- 성공 응답 후 입력 필드 **숨김** 또는 `readOnly` + 성공 Notice **fade-in 200ms**
- "로그인으로" 클릭 → S02 (**즉시**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| ResetRequestCard | `POST /auth/password-reset/request` | body: `email` |

---

## 디자인 토큰

- 입력: 배경 **Cream Surface**, border **Light Cream**, placeholder **Muted Gray**  
- CTA: Primary Dark (**Charcoal** / **Off-White**)  
- 성공 박스: border **Light Cream**, 텍스트 **Charcoal** + 보조 **Muted Gray**
