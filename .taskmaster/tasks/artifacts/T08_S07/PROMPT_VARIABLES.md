# Agent 프롬프트 입력 인자 — S07 / T08

관리자 UI: `/admin/prompts` → **입력 인자** 패널 (`PromptVariablePanel`)

구현: `frontend/src/lib/promptVariables.ts`, `CompanyInsightAgentService.buildUserMessage`

---

## 공통

| 토큰 | 설명 | 자동 부착 |
|------|------|-----------|
| `{{company_name}}` | 대상 기업명 | — |
| `{{context}}` | DART·재무·공시·뉴스 마크다운 | 템플릿에 없으면 `--- 수집 데이터:` 블록 |
| `{{market_technical}}` | 장중 시세(Naver/Yahoo) + MA·RSI(Yahoo) + 정/역배열 | 템플릿에 없고 조회 성공 시 `--- 시장·기술 지표:` 블록 |

## RISK_DETECTION 추가

| 토큰 | 설명 | 자동 부착 |
|------|------|-----------|
| `{{signal_json_instruction}}` | JSON 출력 형식 지침 | 없으면 서버 고정 지침 |

---

## `{{market_technical}}` 본문 예시

```markdown
## 시장·기술 지표
- 데이터: 장중 네이버 금융 · 일봉/지표 Yahoo Finance
- 심볼: 005930.KS
- 현재가: 299,500원 (+2.15%)
- RSI(14): 58.3
- 60·120·240 추세: 정배열 (MA60 > MA120 > MA240, 상승 추세)
- 초단기(5일선): 상승 (현재가 > MA5)
- 단기(20일선): 상승 (현재가 > MA20)
- MA5: …, MA20: …, MA60: …, MA120: …, MA240: …
```

생성: `MarketTechnicalContextFormatter.format(CompanyMarketTechnicalDto)`

---

## 권장 초안 (INSIGHT_SUMMARY)

```
대상 기업: {{company_name}}

{{context}}

{{market_technical}}

위 데이터만 근거로 3~5개 불릿 인사이트…
```

DB에 저장된 기존 템플릿은 관리자 화면에서 칩으로 `{{market_technical}}` 추가 후 저장.
