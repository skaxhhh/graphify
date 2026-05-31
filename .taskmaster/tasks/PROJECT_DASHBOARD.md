# graphify — PROJECT_DASHBOARD

> Taskmaster `master` 태그 기준 자동 생성

## 프로젝트

| 항목 | 값 |
|------|-----|
| 이름 | graphify |
| 스택 | React.js + Spring Boot + PostgreSQL |
| PRD | [prd.txt](../docs/prd.txt) |
| Screen flow | [SCREEN_FLOW.md](../docs/SCREEN_FLOW.md) |
| UI specs | [ui_specs/](../docs/ui_specs/) |

## 우선순위 범례

| 등급 | Taskmaster priority |
|------|---------------------|
| P0 | high |
| P1 | medium |
| P2 | low |

## 태스크 보드

| ID | 구현 화면 | 제목 | Tier | 의존성 | 산출물 |
|----|-----------|------|------|--------|--------|
| 1 | 공통 (부트스트랩/인프라) | graphify 모노레포 부트스트랩 (React + Spring Boot + PostgreSQL) | P0 | [] | `artifacts/T01_BOOT/` |
| 2 | S01 | S01 홈/랜딩 — 검색·히어로·Guest 레이아웃 | P0 | [1] | `artifacts/T02_S01/` |
| 3 | S02 | S02 로그인 — 소셜·이메일 인증 | P0 | [1] | `artifacts/T03_S02/` |
| 4 | S03 | S03 약관 동의 | P0 | [3] | `artifacts/T04_S03/` |
| 5 | S04 | S04 비밀번호 재설정 요청 | P1 | [3] | `artifacts/T05_S04/` |
| 6 | S05 | S05 비밀번호 재설정 확인 | P1 | [5] | `artifacts/T06_S05/` |
| 7 | S06 | S06 검색 결과 목록 — 정렬·필터·하이브리드 검색 | P0 | [1, 2] | `artifacts/T07_S06/` |
| 8 | S07 | S07 기업 상세 — 인사이트 카드·신호·OV02/06 | P0 | [7] | `artifacts/T08_S07/` |
| 9 | S08 | S08 관계 그래프 시각화 — 필터·깊이·SSE·OV01/08 | P0 | [8] | `artifacts/T09_S08/` |
| 10 | S09 | S09 분석 이력 목록 | P1 | [3] | `artifacts/T10_S09/` |
| 11 | S10 | S10 분석 이력 상세·시계열 비교 | P1 | [10, 8] | `artifacts/T11_S10/` |
| 12 | S11 | S11 관심 기업 목록·비교 (최대 3) | P1 | [3, 8] | `artifacts/T12_S11/` |
| 13 | S12 | S12 마이페이지 — 비밀번호·프롬프트·OV05 | P1 | [3] | `artifacts/T13_S12/` |
| 14 | S13 | S13 관리자 대시보드 — Agent 실행·토큰·알림 | P1 | [1, 3] | `artifacts/T14_S13/` |
| 15 | S14 | S14 MCP 도구 관리 — OV03 | P1 | [14] | `artifacts/T15_S14/` |
| 16 | S15 | S15 Agent 프롬프트 관리 — 버전·OV04 | P2 | [14] | `artifacts/T16_S15/` |
| 17 | S16 | S16 Azure OpenAI 연결 설정 | P2 | [14] | `artifacts/T17_S16/` |
| 18 | S17 | S17 Vector DB 관리 — 재임베딩·정리 | P2 | [14] | `artifacts/T18_S17/` |

## 다음 액션

1. `task-master list` 또는 MCP `get_tasks`로 상태 확인
2. Task **1** 부트스트랩부터 의존성 순으로 진행
3. 각 태스크 `artifacts/.../DEV_GUIDE.md`의 UI_SPEC 링크 준수
