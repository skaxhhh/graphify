# 개발 가이드 — T08 / S07 기업 상세

> **화면**: S07 `/companies/{id}`  
> **태스크 ID**: T08_S07  
> **후속 CR**: `.harness/requests/CR_00N_*.md` 작성 시 이 문서·`API_SPEC.md`·`DB_SCHEMA.md`를 함께 갱신

## CR 이력 (S07 범위)

| CR | 요약 | 상태 |
|----|------|------|
| [CR_003](../../../.harness/requests/CR_003_S07_dart_collect_agent_insight.md) | DART 수집·Agent 인사이트·신호·2열 레이아웃 | ✅ |
| [CR_004](../../../.harness/requests/CR_004_S07_market_technical_yahoo.md) | Yahoo 시세·MA·RSI·기술 패널 | ✅ |
| (본 CR 이후) | Agent `{{market_technical}}` 프롬프트 연동 | ✅ |

레지스트리: `.harness/cr_registry.md`

---

## 사용자 플로우

1. 검색(S06) → DART 기업 선택 → 상세(S07)
2. `needsSync` / 인사이트·신호 부족 시 FE 자동:
   - `POST /companies/{id}/sync` — DART + 재무 + 공시(6개월) + 뉴스
   - `POST /companies/{id}/insights/generate` — `INSIGHT_SUMMARY` + `RISK_DETECTION`
3. `ticker` 있으면 `GET /companies/{id}/market-technical` — 시장·기술 카드
4. UI: 좌(기본·재무·공시·출처) / 우(뉴스·기술·신호·AI 인사이트)

## 백엔드 패키지

| 패키지 | 역할 |
|--------|------|
| `com.graphify.company.dart.*` | DART 스냅샷, Agent 인사이트·신호 |
| `com.graphify.company.market.*` | Naver 장중 시세 + Yahoo 일봉 MA/RSI, Agent용 `{{market_technical}}` |
| `com.graphify.company.registry.dart.*` | DART API 클라이언트 |
| `com.graphify.agent.AzureChatCompletionClient` | LLM (미설정 시 mock-dev) |

## Agent 프롬프트 입력 인자

관리자 **Agent 프롬프트** (`/admin/prompts`) — 태스크 템플릿 칩 삽입.

| 토큰 | 출처 |
|------|------|
| `{{company_name}}` | `companies.name` |
| `{{context}}` | DART 스냅샷 → `CompanyDartProfileMapper.buildAgentContext` |
| `{{market_technical}}` | Yahoo → `MarketTechnicalContextFormatter` (티커 없으면 빈 문자열) |
| `{{signal_json_instruction}}` | `RISK_DETECTION` 전용, 없으면 서버가 JSON 지침 자동 부착 |

템플릿에 토큰을 **넣지 않으면** `{{context}}` / `{{market_technical}}` 은 각각 끝에 자동 덧붙임 (`CompanyInsightAgentService.buildUserMessage`).

## 환경 변수 (루트 `.env`)

| 변수 | 용도 |
|------|------|
| `DART_API_KEY` | Open DART |
| `NEWS_API_KEY` | (선택) NewsAPI |
| `OPENAI_*` | Azure OpenAI / 호환 게이트웨이 |
| `MARKET_YAHOO_ENABLED` | (선택, 기본 true) Yahoo 시세 |
| `MARKET_NAVER_ENABLED` | (선택, 기본 true) Naver 시세 |
| `KRX_API_KEY` | (선택) KRX Open API 키 (향후 일봉 공식 소스 확장) |

## 로컬 확인

```bash
./init.sh restart
curl -s http://localhost:8081/api/v1/bootstrap/integrations | jq .
curl -s http://localhost:8081/api/v1/companies/1 | jq '.data.ticker, .data.dartProfile.stockCode'
curl -X POST http://localhost:8081/api/v1/companies/1/sync -H "Content-Type: application/json" -d '{}'
curl -s http://localhost:8081/api/v1/companies/1/market-technical | jq .
curl -X POST http://localhost:8081/api/v1/companies/1/insights/generate -H "Content-Type: application/json" -d '{}'
curl -s http://localhost:8081/api/v1/companies/1/insights | jq '.data.agentInsight, .data.signals | length'
```

프론트: `cd frontend && npm run dev` → `/companies/1`

## 프론트 주요 컴포넌트

- `CompanyDetailPage` — sync/generate 파이프라인, 진행률 %
- `CompanyTechnicalPanel` — 시세·MA·RSI + 현재가 대비 5일/20일선 위·아래 표시
- `AgentInsightPanel` — AI 인사이트
- `CompanySignalSections` — Agent 리스크/기회
- `AdminPromptsPage` + `PromptVariablePanel` — 입력 인자

## 후속 CR 작성 시

1. `.harness/requests/CR_00N_S07_*.md` 생성 → `cr_registry.md` 갱신  
2. **본 폴더** `API_SPEC.md` / `DB_SCHEMA.md` / `DEV_GUIDE.md` 에 엔드포인트·테이블·플로우 반영  
3. 구현 화면 메타는 Taskmaster `tasks.json` T08 유지

## 백로그 (미구현)

- Agent SSE 스트리밍 (T17)
- 시세 DB 캐시·KIS/KRX 정식 API 교체
- TradingView 위젯 임베드
