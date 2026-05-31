#!/usr/bin/env python3
"""Generate Taskmaster tasks.json + per-task artifacts for graphify."""
from __future__ import annotations

import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TASKS_DIR = ROOT / ".taskmaster" / "tasks"
ARTIFACTS = TASKS_DIR / "artifacts"
UI_SPECS = ROOT / ".taskmaster" / "docs" / "ui_specs"

COMPLETION_BLOCK = """### 완료 기준 (필수)
- [ ] UI_SPEC 레이아웃 구조 준수
- [ ] 4가지 상태(loading / empty / error / populated) 구현
- [ ] 인터랙션 규칙 구현 (해당 화면 UI_SPEC 기준)
- [ ] 이전·다음 태스크 인터페이스 연동 확인 (라우팅·API 계약·세션 상태)
"""


def priority_tm(tier: str) -> str:
    return {"P0": "high", "P1": "medium", "P2": "low"}[tier]


def parse_component_checklist(spec_path: Path) -> list[str]:
    if not spec_path.exists():
        return ["(UI_SPEC 파일 없음) 컴포넌트 트리 수동 정의"]
    text = spec_path.read_text(encoding="utf-8")
    marker = "## 컴포넌트 트리"
    if marker not in text:
        return ["컴포넌트 트리 섹션 확인"]
    rest = text.split(marker, 1)[1]
    start_fence = rest.find("```")
    if start_fence == -1:
        return ["컴포넌트 트리 코드블록 시작 없음"]
    end_fence = rest.find("```", start_fence + 3)
    if end_fence == -1:
        return ["컴포넌트 트리 코드블록 종료 없음"]
    inner = rest[start_fence + 3 : end_fence]
    lines = [ln.rstrip() for ln in inner.strip().splitlines() if ln.strip()]
    items: list[str] = []
    for ln in lines:
        if ln.strip().startswith("```"):
            break
        stripped = ln.lstrip("│ ├└─").strip()
        if not stripped or stripped.startswith("("):
            continue
        items.append(f"[ ] UI 컴포넌트: {stripped}")
    return items[:20] or ["[ ] UI_SPEC 컴포넌트 트리 기반 구현"]


def screen_spec_file(sid: str) -> Path:
    mapping = {
        "S01": "UI_SPEC_S01_Home.md",
        "S02": "UI_SPEC_S02_Login.md",
        "S03": "UI_SPEC_S03_TermsConsent.md",
        "S04": "UI_SPEC_S04_PasswordResetRequest.md",
        "S05": "UI_SPEC_S05_PasswordResetConfirm.md",
        "S06": "UI_SPEC_S06_SearchResult.md",
        "S07": "UI_SPEC_S07_CompanyDetail.md",
        "S08": "UI_SPEC_S08_GraphVisualization.md",
        "S09": "UI_SPEC_S09_AnalysisHistoryList.md",
        "S10": "UI_SPEC_S10_AnalysisHistoryDetail.md",
        "S11": "UI_SPEC_S11_Watchlist.md",
        "S12": "UI_SPEC_S12_MyPage.md",
        "S13": "UI_SPEC_S13_AdminDashboard.md",
        "S14": "UI_SPEC_S14_AdminMcpTools.md",
        "S15": "UI_SPEC_S15_AdminPrompts.md",
        "S16": "UI_SPEC_S16_AdminOpenAIConfig.md",
        "S17": "UI_SPEC_S17_AdminVectorDB.md",
    }
    return UI_SPECS / mapping[sid]


def write_task_artifacts(
    task_id: int,
    screen: str | None,
    title: str,
    tier: str,
    layers: dict[str, bool],
    api_endpoints: list[str],
    db_tables: list[str],
) -> None:
    folder = ARTIFACTS / f"T{task_id:02d}_{screen or 'BOOT'}"
    folder.mkdir(parents=True, exist_ok=True)

    api_md = f"""# API 명세 — Task {task_id}: {title}

> **우선순위**: {tier}  
> **구현 화면**: {screen or "공통 (부트스트랩)"}  
> **레이어**: Frontend={layers['fe']}, Backend={layers['be']}, DB={layers['db']}

## 엔드포인트 (SCREEN_FLOW / PRD 기준 초안)

"""
    for ep in api_endpoints:
        api_md += f"- `{ep}`\n"

    api_md += """
## 공통 규약 (Spring Boot)

- `Content-Type: application/json`, 오류 시 RFC7807 스타일 `problem+json` 권장
- 인증: Bearer JWT (User), Admin 라우트는 `ROLE_ADMIN`
- 페이지네이션: `page`, `size` (0-base)

## 버전

- 문서 자동 생성 초안 — 구현 시 OpenAPI로 동기화
"""
    (folder / "API_SPEC.md").write_text(api_md, encoding="utf-8")

    db_md = f"""# DB 스키마 — Task {task_id}: {title}

> **구현 화면**: {screen or "공통"}

## 테이블 (초안)

"""
    for t in db_tables:
        db_md += f"### `{t}`\n- 컬럼·인덱스는 구현 단계에서 Flyway/Liquibase 마이그레이션으로 확정\n\n"

    if not db_tables:
        db_md += "_이 태스크는 스키마 변경이 없거나 공통 마이그레이션에 포함됩니다._\n"

    db_md += """
## 참고

- RDB: PostgreSQL
- 민감정보(비밀번호 해시, API 키)는 암호화/KMS 정책에 따름
"""
    (folder / "DB_SCHEMA.md").write_text(db_md, encoding="utf-8")

    if screen:
        dev_md = f"""# 개발 가이드 — Task {task_id}: {title}

## UI 명세

- [UI_SPEC — {screen}](../../../docs/ui_specs/{screen_spec_file(screen).name})

## 구현 순서

1. UI_SPEC **레이아웃 구조** 및 breakpoint
2. **loading / empty / error / populated** 상태
3. **인터랙션 규칙** (해당 UI_SPEC 표)
4. `API_SPEC.md`와 Spring Controller DTO 정합

## 스택

- React + Spring Boot + PostgreSQL

{COMPLETION_BLOCK}
"""
    else:
        dev_md = f"""# 개발 가이드 — Task {task_id}: {title}

## UI 명세

- 공통 레이아웃: [SCREEN_FLOW.md](../../../docs/SCREEN_FLOW.md) (§3 공유 레이아웃)
- 전역 정책: [prd.txt](../../../docs/prd.txt) (§3 아키텍처)

## 구현 순서

1. 모노레포/멀티모듈 디렉터리 구조 확정
2. 로컬 PostgreSQL 및 Spring datasource
3. React 라우터·레이아웃 셸(Guest/User/Admin 분기 준비)
4. `API_SPEC.md` 헬스 확인

## 스택

- React + Spring Boot + PostgreSQL

{COMPLETION_BLOCK}
"""
    (folder / "DEV_GUIDE.md").write_text(dev_md, encoding="utf-8")


# --- Task definitions: id, screen, title, tier, deps, layers, api, db
TASK_DEFS: list[dict] = [
    {
        "id": 1,
        "screen": None,
        "title": "graphify 모노레포 부트스트랩 (React + Spring Boot + PostgreSQL)",
        "tier": "P0",
        "deps": [],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /actuator/health"],
        "db": ["_flyway_schema_history"],
    },
    {
        "id": 2,
        "screen": "S01",
        "title": "S01 홈/랜딩 — 검색·히어로·Guest 레이아웃",
        "tier": "P0",
        "deps": [1],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /search/autocomplete?q=", "GET /terms/latest (옵션)"],
        "db": ["search_log (옵션)", "terms_version"],
    },
    {
        "id": 3,
        "screen": "S02",
        "title": "S02 로그인 — 소셜·이메일 인증",
        "tier": "P0",
        "deps": [1],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["POST /auth/login", "GET /auth/oauth/{provider}/url"],
        "db": ["users", "user_auth_providers"],
    },
    {
        "id": 4,
        "screen": "S03",
        "title": "S03 약관 동의",
        "tier": "P0",
        "deps": [3],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /terms/latest", "POST /auth/consent"],
        "db": ["terms_acceptances", "terms_documents"],
    },
    {
        "id": 5,
        "screen": "S04",
        "title": "S04 비밀번호 재설정 요청",
        "tier": "P1",
        "deps": [3],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["POST /auth/password-reset/request"],
        "db": ["password_reset_tokens"],
    },
    {
        "id": 6,
        "screen": "S05",
        "title": "S05 비밀번호 재설정 확인",
        "tier": "P1",
        "deps": [5],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /auth/password-reset/validate?token=", "POST /auth/password-reset/confirm"],
        "db": ["password_reset_tokens", "users"],
    },
    {
        "id": 7,
        "screen": "S06",
        "title": "S06 검색 결과 목록 — 정렬·필터·하이브리드 검색",
        "tier": "P0",
        "deps": [1, 2],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /companies/search?q=&sort=&filter=&page="],
        "db": ["companies", "company_embeddings (Vector 연동)"],
    },
    {
        "id": 8,
        "screen": "S07",
        "title": "S07 기업 상세 — 인사이트 카드·신호·OV02/06",
        "tier": "P0",
        "deps": [7],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /companies/{id}", "GET /companies/{id}/insights"],
        "db": ["companies", "company_insights", "insight_cards"],
    },
    {
        "id": 9,
        "screen": "S08",
        "title": "S08 관계 그래프 시각화 — 필터·깊이·SSE·OV01/08",
        "tier": "P0",
        "deps": [8],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /companies/{id}/graph?depth=&filter=", "SSE /agent/stream/{sessionId}"],
        "db": ["graph_snapshots", "relationship_edges", "relationship_nodes", "agent_sessions"],
    },
    {
        "id": 10,
        "screen": "S09",
        "title": "S09 분석 이력 목록",
        "tier": "P1",
        "deps": [3],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /history/me?page=&size=&from=&to=&q="],
        "db": ["agent_sessions", "analysis_history"],
    },
    {
        "id": 11,
        "screen": "S10",
        "title": "S10 분석 이력 상세·시계열 비교",
        "tier": "P1",
        "deps": [10, 8],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /history/{sessionId}", "GET /companies/{id}/graph"],
        "db": ["agent_sessions", "graph_snapshots", "timeline_events"],
    },
    {
        "id": 12,
        "screen": "S11",
        "title": "S11 관심 기업 목록·비교 (최대 3)",
        "tier": "P1",
        "deps": [3, 8],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /watchlist/me", "GET /companies/compare?ids=&basis="],
        "db": ["watchlist_items"],
    },
    {
        "id": 13,
        "screen": "S12",
        "title": "S12 마이페이지 — 비밀번호·프롬프트·OV05",
        "tier": "P1",
        "deps": [3],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /users/me", "PUT /users/me/password", "PUT /users/me/prompt", "POST /auth/logout"],
        "db": ["users", "user_preferences"],
    },
    {
        "id": 14,
        "screen": "S13",
        "title": "S13 관리자 대시보드 — Agent 실행·토큰·알림",
        "tier": "P1",
        "deps": [1, 3],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /admin/agent/stats?period=", "GET /admin/users/usage"],
        "db": ["agent_sessions", "admin_metrics_daily"],
    },
    {
        "id": 15,
        "screen": "S14",
        "title": "S14 MCP 도구 관리 — OV03",
        "tier": "P1",
        "deps": [14],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /admin/tools", "POST /admin/tools", "PUT /admin/tools/{id}", "DELETE /admin/tools/{id}", "POST /admin/tools/{id}/ping"],
        "db": ["mcp_tools"],
    },
    {
        "id": 16,
        "screen": "S15",
        "title": "S15 Agent 프롬프트 관리 — 버전·OV04",
        "tier": "P2",
        "deps": [14],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /admin/prompts?type=", "POST /admin/prompts", "POST /admin/prompts/{id}/test", "POST /admin/prompts/{id}/rollback"],
        "db": ["agent_prompts", "agent_prompt_versions"],
    },
    {
        "id": 17,
        "screen": "S16",
        "title": "S16 Azure OpenAI 연결 설정",
        "tier": "P2",
        "deps": [14],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /admin/openai/config", "PUT /admin/openai/config", "GET /admin/openai/status"],
        "db": ["openai_settings (암호화 컬럼)"],
    },
    {
        "id": 18,
        "screen": "S17",
        "title": "S17 Vector DB 관리 — 재임베딩·정리",
        "tier": "P2",
        "deps": [14],
        "layers": {"fe": True, "be": True, "db": True},
        "api": ["GET /admin/vectordb/stats", "POST /admin/vectordb/reindex", "DELETE /admin/vectordb/cleanup"],
        "db": ["embedding_jobs", "vector_index_stats"],
    },
]


def build_tasks() -> dict:
    tasks: list[dict] = []
    for d in TASK_DEFS:
        tid = d["id"]
        screen = d["screen"]
        spec_path = screen_spec_file(screen) if screen else None
        checklist = parse_component_checklist(spec_path) if spec_path else [
            "[ ] React 앱 라우팅/레이아웃 셸",
            "[ ] Spring Boot 멀티모듈 또는 단일 API",
            "[ ] PostgreSQL Docker compose + 연결 설정",
            "[ ] 공통 CORS·보안 헤더",
        ]
        subtasks = []
        for i, line in enumerate(checklist, start=1):
            subtasks.append(
                {
                    "id": i,
                    "title": line.replace("[ ] ", "")[:120],
                    "description": "UI_SPEC 컴포넌트 트리 체크리스트 항목",
                    "status": "pending",
                    "dependencies": [],
                    "details": line,
                }
            )

        layer_note = (
            f"**레이어**: Frontend={'Y' if d['layers']['fe'] else 'N'}, "
            f"Backend={'Y' if d['layers']['be'] else 'N'}, "
            f"DB={'Y' if d['layers']['db'] else 'N'}"
        )
        screen_field = screen or "공통 (부트스트랩/인프라)"
        ui_spec_ref = (
            f"`.taskmaster/docs/ui_specs/{screen_spec_file(screen).name}`"
            if screen
            else "`SCREEN_FLOW.md` §3 + PRD §3 (공통 인프라)"
        )
        details = f"""## graphify 태스크 메타
- **구현 화면**: {screen_field}
- **우선순위 등급**: {d['tier']} (Taskmaster priority=`{priority_tm(d['tier'])}`)
- {layer_note}

## 참조 문서
- PRD: `.taskmaster/docs/prd.txt`
- 화면 흐름: `.taskmaster/docs/SCREEN_FLOW.md`
- UI 명세: {ui_spec_ref}

## 산출물 (자동 생성)
- `artifacts/T{tid:02d}_{screen or 'BOOT'}/API_SPEC.md`
- `artifacts/T{tid:02d}_{screen or 'BOOT'}/DB_SCHEMA.md`
- `artifacts/T{tid:02d}_{screen or 'BOOT'}/DEV_GUIDE.md`

{COMPLETION_BLOCK}

## 구현 메모
- 스택: React.js + Spring Boot + PostgreSQL
- 인증·권한은 SCREEN_FLOW의 Guest/User/Admin 정책과 일치시킬 것
"""

        tasks.append(
            {
                "id": tid,
                "title": d["title"],
                "description": f"graphify — {screen_field} ({d['tier']})",
                "status": "pending",
                "dependencies": d["deps"],
                "priority": priority_tm(d["tier"]),
                "details": details,
                "testStrategy": "DEV_GUIDE 완료 기준 체크리스트 + API_SPEC 계약 테스트(수동 또는 통합테스트)",
                "subtasks": subtasks,
                "metadata": {
                    "graphifyScreen": screen_field,
                    "priorityTier": d["tier"],
                    "layers": d["layers"],
                    "artifactDir": f"artifacts/T{tid:02d}_{screen or 'BOOT'}",
                },
            }
        )
    return {"master": {"tasks": tasks, "metadata": {"project": "graphify", "generated": "2026-05-15"}}}


def write_dashboard(tasks: list[dict]) -> None:
    def base_lines(link_prefix: str) -> list[str]:
        return [
            "# graphify — PROJECT_DASHBOARD",
            "",
            "> Taskmaster `master` 태그 기준 자동 생성",
            "",
            "## 프로젝트",
            "",
            "| 항목 | 값 |",
            "|------|-----|",
            "| 이름 | graphify |",
            "| 스택 | React.js + Spring Boot + PostgreSQL |",
            f"| PRD | [prd.txt]({link_prefix}docs/prd.txt) |",
            f"| Screen flow | [SCREEN_FLOW.md]({link_prefix}docs/SCREEN_FLOW.md) |",
            f"| UI specs | [ui_specs/]({link_prefix}docs/ui_specs/) |",
            "",
            "## 우선순위 범례",
            "",
            "| 등급 | Taskmaster priority |",
            "|------|---------------------|",
            "| P0 | high |",
            "| P1 | medium |",
            "| P2 | low |",
            "",
            "## 태스크 보드",
            "",
            "| ID | 구현 화면 | 제목 | Tier | 의존성 | 산출물 |",
            "|----|-----------|------|------|--------|--------|",
        ]

    for path, prefix in (
        (TASKS_DIR / "PROJECT_DASHBOARD.md", "../"),
        (ROOT / "PROJECT_DASHBOARD.md", ".taskmaster/"),
    ):
        lines = base_lines(prefix)
        for t in tasks:
            meta = t.get("metadata") or {}
            sc = meta.get("graphifyScreen", "")
            tier = meta.get("priorityTier", "")
            art = meta.get("artifactDir", "")
            title_short = t["title"][:56] + ("…" if len(t["title"]) > 56 else "")
            lines.append(
                f"| {t['id']} | {sc} | {title_short} | {tier} | {t['dependencies']} | `{art}/` |"
            )
        lines += [
            "",
            "## 다음 액션",
            "",
            "1. `task-master list` 또는 MCP `get_tasks`로 상태 확인",
            "2. Task **1** 부트스트랩부터 의존성 순으로 진행",
            "3. 각 태스크 `artifacts/.../DEV_GUIDE.md`의 UI_SPEC 링크 준수",
            "",
        ]
        path.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    TASKS_DIR.mkdir(parents=True, exist_ok=True)
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    data = build_tasks()
    tasks = data["master"]["tasks"]
    out = TASKS_DIR / "tasks.json"
    out.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    for d in TASK_DEFS:
        write_task_artifacts(
            d["id"],
            d["screen"],
            d["title"],
            d["tier"],
            d["layers"],
            d["api"],
            d["db"],
        )

    write_dashboard(tasks)
    print(f"Wrote {out}, {len(tasks)} tasks, artifacts under {ARTIFACTS}")


if __name__ == "__main__":
    main()
