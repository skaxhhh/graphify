# DB 스키마 — Task 16: S15 Agent 프롬프트 관리 — 버전·OV04

> **마이그레이션**: `V16__agent_prompts.sql`

## `agent_prompts`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| task_type | VARCHAR(64) UNIQUE | RELATION_ANALYSIS / RISK_DETECTION / INSIGHT_SUMMARY |
| system_prompt | TEXT | 시스템 프롬프트 |
| task_template | TEXT | 태스크 템플릿 |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

## `agent_prompt_versions`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| prompt_id | BIGINT FK → agent_prompts | |
| version_number | INT | 태스크별 순번 |
| system_prompt | TEXT | 스냅샷 |
| task_template | TEXT | 스냅샷 |
| change_note | VARCHAR(255) | 선택 메모 |
| author_id | BIGINT | 저장자 |
| author_name | VARCHAR(128) | 표시명 |
| summary | VARCHAR(255) | 버전 요약 |
| created_at | TIMESTAMPTZ | |

**인덱스**: `(prompt_id, created_at DESC)`, UNIQUE `(prompt_id, version_number)`

## 시드

3개 태스크 유형 + v1 버전 자동 생성.
