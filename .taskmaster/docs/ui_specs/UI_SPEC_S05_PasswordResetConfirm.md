# UI 명세 — S05 비밀번호 재설정 (신규 비밀번호 입력)

> **화면 ID**: S05  
> **레이아웃**: Guest Layout (유효 토큰 쿼리 `token` 또는 경로 파라미터)  
> **PRD**: 9.4.2~9.4.3

---

## 레이아웃 구조

```
┌─ [SHARED] AppHeaderGuest ────────────────────────────────────────────────┐
├─ MAIN: flex-1 flex items-center justify-center py-12 md:py-20 ─────────────┤
│  ┌─ max-w-[440px] w-[calc(100%-2rem)] ───────────────────────────────────┐ │
│  │  Card p-6 md:p-8, border Light Cream, radius 12px                       │ │
│  │  제목: "새 비밀번호 설정" (Charcoal, Sub-heading 축소)                  │ │
│  │  PasswordField [SHARED] "새 비밀번호" + PolicyHint (Caption, Muted)     │ │
│  │  PasswordField [SHARED] "비밀번호 확인"                                 │ │
│  │  PasswordStrengthMeter [SHARED] (옵션: 정책 시각화)                    │ │
│  │  PrimaryButton "비밀번호 변경" w-full mt-6                               │ │
│  │  GhostButton "재설정 메일 다시 받기" → S04 (만료 시 노출)               │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─ [SHARED] MarketingFooter ───────────────────────────────────────────────┤
└──────────────────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 카드 | `max-w-[440px]` |
| 정책 힌트 | Caption **14px**, **Muted Gray** |

**반응형**: 모바일에서 키보드 대응 `scroll-mt-16`, CTA 하단 `sticky`는 사용하지 않음(간단 폼 권장).

---

## 컴포넌트 트리

```
PasswordResetConfirmPage (S05)
├── [SHARED] AppHeaderGuest
├── main
│   └── ResetConfirmCard
│       ├── TokenValidator (마운트 시 토큰 검증 — 로딩/만료 UI 분기)
│       ├── PageTitle
│       ├── NewPasswordField [SHARED] (props: showStrength)
│       ├── ConfirmPasswordField [SHARED]
│       ├── InlineErrors (조건부: 불일치, 정책 미달)
│       ├── PrimaryButton [SHARED] → POST confirm
│       ├── LinkToS04 (조건부: 토큰 만료/무효)
│       └── SuccessState (조건부: 완료 후 2초 타이머 → S02)
└── [SHARED] MarketingFooter
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| validating_token | URL 토큰 존재, 서버 검증 중 | 카드 중앙 Skeleton |
| invalid_expired | 토큰 무효/만료 | 안내 문구 + S04 링크 + 고스트 버튼 |
| idle | 토큰 유효 | 두 입력 필드 + CTA |
| submitting | confirm 요청 | 버튼 로딩 |
| success | 변경 완료 | 성공 메시지 + 자동 S02 이동 카운트다운 |
| error | API 실패 | ErrorBanner + 재시도 |

---

## 인터랙션 규칙

- 새 비밀번호 입력 → 정책 검증 **디바운스 300ms** → PolicyHint·미터 갱신
- 확인 필드 blur → 불일치 시 InlineErrors **즉시**
- Submit → 성공 시 토스트(옵션) + **2s** 후 S02 redirect (**ease-in-out**)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| TokenValidator | `GET /auth/password-reset/validate?token=` (옵션) 또는 confirm 시 일괄 | `valid`, `expiresAt` |
| PrimaryButton | `POST /auth/password-reset/confirm` | body: `token`, `newPassword` |

---

## 디자인 토큰

- 에러 텍스트: **Charcoal** (본문), 강조 필요 시 경고색은 디자인 시스템 외 — **Muted Gray** 배지로 완화 가능  
- 필드: **Light Cream** border, 포커스 **Ring Blue**
