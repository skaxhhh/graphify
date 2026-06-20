# Testing

**Analysis Date:** 2026-06-20

## Frameworks

**Backend (`backend/build.gradle.kts`):**
- JUnit 5 (Jupiter) via `useJUnitPlatform()` — `tasks.withType<Test>`
- `spring-boot-starter-test` — Spring Boot integration testing (includes AssertJ, Mockito, JSONassert)
- `spring-security-test` — auth/security testing helpers
- `junit-platform-launcher` (testRuntimeOnly)

**Frontend (`frontend/package.json`):**
- Playwright 1.51.0 — end-to-end browser testing
- No unit-test runner (no Vitest/Jest configured) — coverage is E2E-only

## Structure

**Backend** — tests mirror the main source package path under
`backend/src/test/java/com/graphify/`:

| Test file | Under test |
|---|---|
| `home/naver/NaverPopularSearchClientTest.java` | Naver popular-search client |
| `home/sentiment/CnnFearGreedClientTest.java` | CNN Fear & Greed client |
| `company/market/TechnicalIndicatorCalculatorTest.java` | Technical indicator math |
| `company/market/NaverFinanceQuoteClientTest.java` | Naver finance quote client |
| `company/market/MarketTechnicalContextFormatterTest.java` | Market context formatting |

Fixtures live in `backend/src/test/resources/` (e.g. `cnn-fear-greed-sample.json`).

**Frontend** — Playwright specs in `frontend/e2e/`:
- `frontend/e2e/company-detail-collected-data.spec.ts`

## Configuration

**Playwright (`frontend/playwright.config.ts`):**
- `testDir: "./e2e"`
- `retries: 0`
- `baseURL: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:5173"`
- Projects: `chromium` (Desktop Chrome) only
- `webServer` auto-start, skippable via `PLAYWRIGHT_SKIP_WEBSERVER`

## How to Run

**Backend:**
```bash
cd backend && ./gradlew test
```

**Frontend E2E:**
```bash
cd frontend
npm run test:e2e        # headless playwright run
npm run test:e2e:ui     # interactive UI mode
```

## Mocking & Patterns

- Backend client tests focus on **external integration clients** (Naver, CNN, Yahoo
  finance, technical indicators) — the highest-risk integration surface.
- Fixture-based testing: real sample payloads under `src/test/resources/` rather
  than fully hand-mocked responses.
- Spring's `spring-boot-starter-test` provides Mockito + MockMvc when needed.

## Coverage Gaps (observed)

- **No coverage measurement** configured (no JaCoCo on backend, no coverage reporter on frontend).
- Backend tests (5 files) concentrate on `company/market` + `home` integration clients;
  controllers, services, security, and the new `trading/` domain have **no unit tests**.
- Frontend has a **single** E2E spec; no component/unit tests at all.
- Playwright `retries: 0` and a single chromium project — flaky-test tolerance and
  cross-browser coverage are minimal.

---

*Testing analysis: 2026-06-20*
