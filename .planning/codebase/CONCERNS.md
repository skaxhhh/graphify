# Concerns

**Analysis Date:** 2026-06-20

Technical debt, risks, and fragile areas. Severity is the orchestrator's
assessment from structural signals, not from runtime evidence.

## Security

**🔴 Dev config contains hardcoded weak secrets (`backend/src/main/resources/application-dev.yml`):**
- `secrets.encryption-key: dev-only-change-me-graphify-secrets-key-256bits-min!!`
- `auth.jwt-secret: dev-only-change-me-graphify-jwt-secret-key-256bits-minimum!!`
- DB `password: graphify`

These are clearly marked dev-only and `application-prod.yml` uses env-var
references, but the pattern is fragile: any deploy that accidentally activates the
`dev` profile ships predictable JWT/encryption keys. Verify prod never falls back
to dev defaults and that startup fails loudly when prod secrets are unset.

**🟡 Secrets management split across files:** root `.env` (git-ignored, confirmed
not tracked), `application-dev.yml` defaults, and `application-prod.yml` env
references. The encryption-key for at-rest secrets (`secrets.encryption-key`) is a
single symmetric key — rotation strategy is not evident.

**🟢 `.gitignore` is correct** for `.env`, `.env.*` (allowing `.env.example`),
`node_modules/`, `backend/build/`, `frontend/dist/`. `.env` confirmed untracked.

## Repository Hygiene

**🟡 Build artifact committed:** `frontend/tsconfig.tsbuildinfo` is tracked
(shows as modified in git status). It's an incremental-build cache and should be
git-ignored, not version-controlled.

**🟡 Large uncommitted work-in-progress:** git status shows a substantial unstaged
feature spanning new `backend/.../trading/`, `backend/.../market/`,
`company/market/` (`OhlcvBar.java`, `IntradayBar.java`), migrations
`V26`–`V29`, and frontend `trading/` pages/stores/APIs. This is a big surface
landing at once — high integration risk, and untested (see below).

## Testing / Quality

**🔴 Near-zero automated coverage on critical paths:** only 5 backend test files
(all in `company/market` + `home` integration clients) and 1 frontend Playwright
spec. The entire new `trading/` domain (`engine/`, `rule/`, `backtest/`),
auth, security config, and controllers have **no tests**. See `TESTING.md`.

**🟡 No coverage tooling** (no JaCoCo, no frontend coverage) — regressions in
untested areas are invisible.

**🟢 Low marker debt:** 0 `TODO/FIXME/HACK` in backend Java; 1 in frontend. The
codebase doesn't carry a large backlog of inline debt markers.

## Architecture / Data

**🟡 Long, fast-moving Flyway migration chain:** 29 sequential migrations
(`V1__baseline.sql` … `V29__market_bars.sql`), several added in the current WIP
(`V26`–`V29`). Sequential Flyway versioning means concurrent feature branches will
collide on version numbers; coordinate numbering before merge.

**🟡 Heavy reliance on external market/data providers** (Yahoo Finance, Naver
Finance, CNN Fear & Greed, DART, KRX, NewsAPI, OpenAI — see `INTEGRATIONS.md`).
Several use undocumented/scraped endpoints (Yahoo chart, Naver finance) that can
break without notice. Integration clients are the best-tested area, which is
appropriate, but resilience (timeouts, fallbacks, rate limits) should be verified.

**🟡 Trading engine is safety-critical and new:** `trading/engine/` plus paper
accounts (`V28__paper_accounts.sql`) and rules (`V27__trading_rules.sql`). Auto-
trading logic with money semantics demands strong tests and guardrails it does not
yet appear to have.

## Operational

**🟡 Profile-dependent behavior:** correct operation hinges on running with the
`prod` profile and a fully populated `.env` in production. Misconfiguration risk is
elevated given how many integrations read `${ENV:default}` fallbacks in
`application-dev.yml`.

## Suggested Priorities

1. Add tests + guardrails around the new `trading/` engine before it goes live (🔴).
2. Confirm prod cannot boot with dev default secrets; fail fast on missing prod secrets (🔴).
3. Untrack `frontend/tsconfig.tsbuildinfo` and add to `.gitignore` (🟡).
4. Land the large WIP in reviewable slices; coordinate Flyway version numbers (🟡).
5. Introduce coverage tooling (JaCoCo / frontend coverage) to make gaps visible (🟡).

---

*Concerns analysis: 2026-06-20*
