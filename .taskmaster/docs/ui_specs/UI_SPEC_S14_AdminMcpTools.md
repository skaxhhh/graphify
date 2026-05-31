# UI 명세 — S14 MCP 도구 관리

> **화면 ID**: S14  
> **레이아웃**: Admin Layout  
> **오버레이**: OV03 도구 등록/수정 모달

---

## 레이아웃 구조

```
┌─ AdminSidebar ─┬─ AdminTopBar ─────────────────────────────────────────────┐
│                ├─ MAIN p-6 md:p-8 max-w-[1400px] mx-auto w-full ─────────────┤
│                │ ┌─ 헤더 행: justify-between items-center flex-wrap gap-4 ─┐ │
│                │ │ 제목 "MCP 도구" (Sub-heading) | Primary "신규 등록"      │ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ Toolbar: SearchField + StatusFilter ─────────────────────┐ │
│                │ └──────────────────────────────────────────────────────────┘ │
│                │ ┌─ Table container overflow-x-auto border Light Cream ──────┐ │
│                │ │ 컬럼: 이름, 설명 요약, 연결상태, 마지막 호출, 활성, 액션   │ │
│                │ └──────────────────────────────────────────────────────────┘ │
└────────────────┴────────────────────────────────────────────────────────────┘
```

| 영역 | 크기 |
|------|------|
| 테이블 최소 행 높이 | `56px` |
| 액션 컬럼 | 고정 `160px` |

**반응형**

| 구간 | 변화 |
|------|------|
| `<768px` | 테이블 → **카드 리스트** (도구명, 상태, 더보기 메뉴) |
| `≥768px` | 전체 테이블 |

---

## 컴포넌트 트리

```
AdminMcpToolsPage (S14)
├── AdminLayoutShell [SHARED]
│   ├── AdminSidebar
│   ├── AdminTopBar
│   └── main
│       ├── PageHeader + PrimaryButton (신규 → OV03 create mode)
│       ├── ToolsToolbar [SHARED]
│       ├── McpToolsTable [SHARED]
│       │   └── RowActions (Ping, Edit→OV03, Delete confirm)
│       └── EmptyState (0건)
└── OV03 McpToolFormModal [SHARED] (create/edit)
    ├── FormFields (name, endpointUrl, authType, description, schema JSON)
    ├── RolePermissionMultiSelect [SHARED] (User/Admin/Premium)
    └── FooterActions (Save, Test connection optional)
```

---

## 상태 정의

| 상태 | 트리거 | UI 표현 |
|------|--------|---------|
| loading | 목록 로드 | 테이블 Skeleton |
| populated | 1건 이상 | 테이블 |
| empty | 0건 | EmptyState + 등록 CTA |
| row_ping_loading | Ping 클릭 | 해당 행 인라인 스피너 |
| row_ping_success | 응답 OK | 초록 토스트/배지 **2s** |
| modal_saving | OV03 저장 | 버튼 로딩 |

---

## 인터랙션 규칙

- "신규 등록" → OV03 **scale+fade 200ms**  
- 활성 토글 → 낙관적 UI + 실패 시 롤백 토스트  
- Ping → 행 내 결과 텍스트 **즉시** 교체 (성공/실패 색은 중립 톤 유지 권장)  
- 삭제 → 확인 Dialog [SHARED] (**150ms** fade)

---

## API 바인딩 포인트

| 컴포넌트 | Endpoint | 필드 매핑 |
|----------|----------|-----------|
| McpToolsTable | `GET /admin/tools` | `tools[]`: `id`, `name`, `description`, `status`, `lastCalledAt`, `enabled`, `allowedRoles[]` |
| OV03 Save | `POST /admin/tools`, `PUT /admin/tools/{id}` | 폼 필드 전체 |
| Ping | `POST /admin/tools/{id}/ping` | `ok`, `latencyMs`, `message` |
| Delete | `DELETE /admin/tools/{id}` | — |

---

## 디자인 토큰

- 테이블: 헤더 **Caption** **Muted Gray**, 바디 **Body** **Charcoal**  
- 상태 배지: 배경 **Charcoal 3%**, 텍스트 **Muted Gray** (ON은 **Charcoal** 테두리)
