# CR_003: S07 DART 기업 데이터 수집 + Agent 인사이트

## 원하는 것

- DART 검색으로 진입한 기업 상세(S07) 클릭 시 **Open DART API로 기업 정보 수집·저장**
- 수집 데이터를 **Agent(INSIGHT_SUMMARY 프롬프트)** 에 전달해 인사이트 생성
- 하단 **관계그래프 기반 인사이트 카드 UI** → **Agent 답변 패널**로 대체 (프롬프트는 관리자 페이지)

## DART에서 수집 가능한 정보 (1차)

| API | 용도 | 제공 필드(요약) |
|-----|------|----------------|
| `company.json` | 기업개황 | 정식명, 영문명, 종목명/코드, 대표자, 법인구분(KOSPI/KOSDAQ), 법인·사업자번호, 주소, 홈/IR, 전화, 업종코드, 설립일, 결산월 |
| `list.json` | 최근 공시 목록 | 접수번호, 공시일, 보고서명, 공시유형 (최근 90일) |

2차(후속): `fnlttSinglAcnt`(재무제표), `elestock`(대량보유), MCP 뉴스 교차.

## API (확정 스코프)

- `POST /companies/{id}/sync` — DART 전체 수집 (기존 확장)
- `POST /companies/{id}/insights/generate` — Agent 인사이트 생성·저장
- `GET /companies/{id}/insights` — `agentInsight` + (레거시 cards/signals 호환)

## 변경 이력

- 2026-05-26 PHASE 1·3 — 설계 및 구현
