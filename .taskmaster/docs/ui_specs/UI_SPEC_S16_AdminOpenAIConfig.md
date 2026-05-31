# UI 명세 — S16 Azure OpenAI 연결 설정

> **화면 ID**: S16  
> **레이아웃**: Admin Layout

---

## 레이아웃 구조

```
┌─ AdminSidebar ─┬─ AdminTopBar ─────────────────────────────────────────────┐
│                ├─ MAIN max-w-[960px] mx-auto p-6 md:p-8 space-y-10 ────────┤
│                │ ┌─ Section: 연결 기본 ───────────────────────────────────┐ │
│                │ │ Card radius 12px border Light Cream p-6 space-y-4       │ │
│                │ │ 필드: endpointUrl, apiKey(masked), deployment, apiVersion│ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ Section: 모델 파라미터 ─────────────────────────────────┐ │
│                │ │ model select, Temperature slider, MaxTokens, Top-P         │ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ Section: 임베딩 모델 ───────────────────────────────────┐ │
│                │ │ embeddingDeployment, embeddingModel                        │ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ Section: 폴백 엔드포인트 ─────────────────────────────────┐ │
│                │ │ repeat fields (subset)                                     │ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ StatusStrip sticky bottom-4 (옵션) ───────────────────────┐ │
│                │ │ 연결상태/토큰사용/RateLimit — 새로고침 아이콘 버튼           │ │
│                │ └──────────────────────────────────────────────────────────┘ │
└────────────────┴────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 카드 | 전폭, 내부 `space-y-4` |
| 슬라이더 | 터치 친화 트랙 높이 **40px** |

**반응형**: 모바일에서 섹션 `space-y-8`; long URL 입력은 `font-mono` **14px**.

---

## 컴포넌트 트리

```
AdminOpenAIConfigPage (S16)
├── AdminLayoutShell [SHARED]
│   ├── AdminSidebar
│   ├── AdminTopBar
│   └── main
│       ├── ConnectionFormCard [SHARED]
│       │   ├── TextField endpointUrl
│       │   ├── SecretField apiKey (mask, rotate hint)
│       │   ├── TextField deploymentName
│       │   └── TextField apiVersion
│       ├── ModelParamsCard [SHARED]
│       │   ├── Select model
│       │   ├── Slider temperature
│       │   ├── NumberField maxTokens
│       │   └── NumberField topP
│       ├── EmbeddingConfigCard [SHARED]
│       ├── FallbackConfigCard [SHARED]
│       ├── FormFooter
│       │   ├── PrimaryButton → PUT config
│       │   └── GhostButton "취소" → reload from server
│       └── OpenAIStatusStrip [SHARED]
│           ├── StatusPill (정상/오류)
│           ├── MetricsInline (tokens, rate limit)
│           └── IconButton refresh → GET status
└── UnsavedChangesDialog [SHARED] (라우트 이탈 시)
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | GET config | 폼 Skeleton |
| editing | 사용자 입력 | dirty 배지 |
| saving | PUT | 전역 또는 버튼 로딩 |
| success | 저장 완료 | 토스트 |
| status_loading | 새로고침 | StatusStrip 스피너 |
| error | API 실패 | ErrorBanner |

---

## 인터랙션 규칙

- 저장 클릭 → 클라이언트 검증 실패 시 필드 스크롤 **smooth 300ms**  
- API 키 필드 포커스 → 값 마스킹 유지, 붙여넣기 시 한 번에 교체  
- 상태 새로고침 → **500ms** throttle

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| 전체 폼 초기값 | `GET /admin/openai/config` | `endpointUrl`, `deploymentName`, `apiVersion`, `model`, `temperature`, `maxTokens`, `topP`, `embeddingModel`, `embeddingDeployment`, `fallbackEndpoint?`, `hasApiKey` |
| Save | `PUT /admin/openai/config` | 위 필드 (키는 optional — 빈 값이면 유지 정책) |
| StatusStrip | `GET /admin/openai/status` | `connection`, `tokensUsed`, `rateLimitRemaining`, `lastCheckedAt` |

---

## 디자인 토큰

- 카드: **Light Cream** border, **Cream** 배경, radius **12px**  
- 슬라이더 트랙: **Light Cream**, 썸: **Charcoal**  
- 비밀 필드: **Muted Gray** placeholder
