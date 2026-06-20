# Directory Structure

**Analysis Date:** 2026-06-20

## Repository Layout (top level)

```
New_Graph/
├── backend/          Spring Boot 3.4.5 API (Java 21, Gradle)
├── frontend/         React 18 + Vite + TypeScript SPA
├── docker/           Docker support files
├── templates/        Project/scaffolding templates
├── ui_prototype/     Standalone UI prototypes
├── docker-compose.yml
├── init.sh           Local dev orchestration script
├── tmux.sh           Multi-pane dev session launcher
├── CLAUDE.md         Agent/orchestration instructions
├── DESIGN.md         Single consolidated design doc (newest on top)
├── RELEASE_NOTES.md  Single consolidated release history
├── design_system.md  Frontend design-system reference
└── .env / .env.example  Backend secrets (root-level)
```

## Backend — `backend/src/main/java/com/graphify/`

Organized by **feature package** (vertical slices), each typically containing a
controller, service, repository, entity, and a `dto/` subpackage.

| Package | Responsibility |
|---|---|
| `auth/` | Login, JWT issuance, registration (`dto/`) |
| `user/` | User entity, profile service (`User.java`, `UserProfileService.java`, `dto/`) |
| `admin/` | Admin dashboard; sub-areas `vectordb/`, `mcp/`, `prompt/`, `openai/` |
| `company/` | Core company domain; sub-areas `dart/`, `news/`, `registry/`, `market/` |
| `company/market/` | Market data clients — `YahooFinanceChartClient.java`, `OhlcvBar.java`, `IntradayBar.java` |
| `company/registry/dart/` | DART (Korean filings) external registry |
| `trading/` | Auto-trading domain; sub-areas `rule/`, `backtest/`, `engine/` (new) |
| `market/` | Market-wide data module (new) |
| `home/` | Home feed; sub-areas `naver/`, `news/`, `sentiment/` |
| `search/` | Search API (`dto/`) |
| `watchlist/` | User watchlists (`dto/`) |
| `history/` | Analysis history (`dto/`) |
| `terms/` | Terms acceptance (`dto/`) |
| `incident/` | Incident tracking |
| `ops/` | Operational endpoints |
| `agent/` | AI agent integration |
| `config/` | Cross-cutting config — `SecurityConfig.java` |
| `common/` | Shared code — `security/`, `exception/`, `dto/` |
| `bootstrap/` | Startup/seed logic |

**Resources:** `backend/src/main/resources/`
- `application.yml`, `application-dev.yml`, `application-prod.yml` — Spring profiles
- `db/migration/` — Flyway migrations `V1__…sql` … `V29__market_bars.sql`

## Backend tests — `backend/src/test/`

Mirrors the main package path under `java/com/graphify/`. Test fixtures live in
`backend/src/test/resources/` (e.g. `cnn-fear-greed-sample.json`).

## Frontend — `frontend/src/`

Organized by **technical role**, with feature folders inside each role dir.

| Directory | Responsibility |
|---|---|
| `pages/` | Route-level views; feature folders e.g. `pages/trading/paper/` |
| `components/` | Reusable UI grouped by feature: `home/`, `auth/`, `graph/`, `terms/`, `admin/` (`vectordb/`, `prompts/`, `openai/`), `shared/`, `search/`, `mypage/`, `password-reset/`, `history/` (`detail/`), `trading/`, `login/`, `watchlist/`, `company/` |
| `layouts/` | Shell layouts — e.g. `TradingLayout.tsx` |
| `router/` | Route config — `router/index.tsx` |
| `stores/` | Zustand client stores — e.g. `tradingStore.ts` |
| `hooks/` | Custom React hooks |
| `lib/` | API clients & utilities — `tradingApi.ts`, `ruleApi.ts` |
| `types/` | Shared TypeScript types — `user.ts`, `trading.ts` |

**Frontend config:** `frontend/package.json`, `frontend/vite.config.ts`,
`frontend/tsconfig.json`, `frontend/playwright.config.ts`, `frontend/tailwind.config.*`

**Frontend E2E:** `frontend/e2e/` (Playwright specs, e.g. `company-detail-collected-data.spec.ts`)

## Naming Conventions

**Backend (Java):**
- Packages: lowercase feature names (`company.market`), DTOs in nested `dto/`
- Classes: `PascalCase` with role suffix — `*Controller`, `*Service`, `*Repository`, `*Client`, `*Dto`
- Migrations: `V{n}__{snake_case_description}.sql` (Flyway), strictly sequential

**Frontend (TypeScript/React):**
- Components: `PascalCase.tsx` (e.g. `TradingLayout.tsx`)
- Stores: `camelCaseStore.ts` (e.g. `tradingStore.ts`)
- API clients: `camelCaseApi.ts` (e.g. `tradingApi.ts`, `ruleApi.ts`)
- Types: lowercase domain file (`user.ts`, `trading.ts`)
- E2E specs: `*.spec.ts`

## Key Entry Points

- Backend app bootstrap: Spring Boot `@SpringBootApplication` under `com/graphify/`
- Security wiring: `backend/src/main/java/com/graphify/config/SecurityConfig.java`
- Frontend routing: `frontend/src/router/index.tsx`
- Local dev: `init.sh` (start stack), `tmux.sh` (multi-pane session)

---

*Structure analysis: 2026-06-20*
