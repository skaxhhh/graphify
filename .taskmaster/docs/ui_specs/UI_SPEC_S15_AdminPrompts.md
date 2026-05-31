# UI 명세 — S15 Agent 프롬프트 관리

> **화면 ID**: S15  
> **레이아웃**: Admin Layout  
> **오버레이**: OV04 우측 슬라이드 테스트 패널

---

## 레이아웃 구조

```
┌─ AdminSidebar ─┬─ AdminTopBar ─────────────────────────────────────────────┐
│                ├─ MAIN: h-[calc(100vh-3.5rem)] flex flex-col ───────────────┤
│                │ ┌─ TaskTabs h-12 border-b Light Cream ─────────────────────┐ │
│                │ │ 탭: 관계 분석 | 리스크 탐지 | 인사이트 요약               │ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ Body flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-[1fr_320px] ┐ │
│                │ │ 좌: EditorColumn min-w-0                                  │ │
│                │ │   ┌─ 시스템 프롬프트 Textarea min-h-[240px] ─────────────┐ │ │
│                │ │   ┌─ 태스크 템플릿 Textarea min-h-[200px] ───────────────┐ │ │
│                │ │   ┌─ 액션 행: Primary 저장 | Ghost 테스트 실행 ──────────┐ │ │
│                │ │ 우: VersionSidebar w-full lg:w-[320px] border-l Light Cream│ │ │
│                │ │   버전 리스트 스크롤, 롤백 버튼                           │ │ │
│                │ └──────────────────────────────────────────────────────────┘ │
└────────────────┴────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 버전 사이드바 | `320px` (`lg` 이상) |
| 에디터 | `flex-1`, `min-h-0` + 내부 스크롤 |

**반응형**

| 구간 | 변화 |
|------|------|
| `<1024px` | 버전 사이드바 → **BottomSheet** 또는 탭 "버전"으로 전환 |
| `≥1024px` | 2열 고정 |

---

## 컴포넌트 트리

```
AdminPromptsPage (S15)
├── AdminLayoutShell [SHARED]
│   ├── AdminSidebar
│   ├── AdminTopBar
│   └── main
│       ├── TaskTypeTabs [SHARED] (query: `type=`)
│       ├── PromptEditorLayout
│       │   ├── SystemPromptEditor [SHARED] (large textarea, monospace 옵션)
│       │   ├── TaskTemplateEditor [SHARED]
│       │   ├── EditorActionBar
│       │   │   ├── PrimaryButton → POST save (버전 자동)
│       │   │   └── GhostButton → open OV04 test
│       │   └── VersionHistoryPanel [SHARED]
│       │       ├── VersionListItem (timestamp, author, summary)
│       │       └── RollbackButton → confirm → POST rollback
│       └── OV04 PromptTestPanel [SHARED] (slide from right `w-full max-w-[480px]`)
│           ├── CompanyIdPicker [SHARED]
│           ├── RunTestButton → POST test
│           └── TestOutputViewer (streaming 선택)
└── (confirm) RollbackConfirmDialog [SHARED]
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 탭별 프롬프트 로드 | 에디터 Skeleton |
| dirty | 텍스트 변경 | 저장 버튼 강조 + "변경됨" 배지 |
| saving | 저장 중 | 버튼 로딩, 에디터 readOnly |
| testing | OV04 실행 | 출력 영역 스트리밍 커서/스켈레톤 |
| error | API 실패 | ErrorBanner |

---

## 인터랙션 규칙

- 탭 전환 → 변경 사항 있으면 Confirm Dialog (**fade 150ms**)
- 저장 성공 → 토스트 + VersionHistory **prepend 200ms** slide
- 테스트 패널 열기 → OV04 **translateX 100% → 0, 300ms** ease-out
- 롤백 확인 → 성공 시 에디터 텍스트 **즉시** 치환

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| Prompt editors | `GET /admin/prompts?type=` | `systemPrompt`, `taskTemplate`, `versions[]` |
| Save | `POST /admin/prompts` | body: `type`, `systemPrompt`, `taskTemplate`, `changeNote?` |
| VersionHistoryPanel | 응답 embedded | `versions[]`: `id`, `createdAt`, `author`, `summary` |
| Rollback | `POST /admin/prompts/{id}/rollback` (예시) | `targetVersionId` |
| OV04 | `POST /admin/prompts/{id}/test` | body: `companyId`, `sampleInput?` → `output`, `tokenUsage` |

---

## 디자인 토큰

- 에디터: 배경 **Cream Surface**, border **Light Cream**, 폰트 모노 옵션은 **Muted Gray** 보조  
- 탭 활성: **Charcoal** 텍스트 + 하단 **2px Charcoal**  
- 패널 그림자: 얕게 — **Focus Shadow** 수준만 (무거운 drop-shadow 금지)
