# External Integrations

**Analysis Date:** 2026-06-20

## APIs & External Services

**Market Data:**
- Yahoo Finance - Stock chart data & historical quotes
  - SDK/Client: Spring RestClient (native)
  - Config: `graphify.market.yahoo-enabled`, `MARKET_YAHOO_CHART_URL`
  - Base URL: `https://query1.finance.yahoo.com`
  - Implementation: `backend/src/main/java/com/graphify/company/market/YahooFinanceChartClient.java`

- Naver Finance - Korean stock quotes & financial data
  - SDK/Client: Spring RestClient (native)
  - Config: `graphify.market.naver-enabled`, `MARKET_NAVER_URL`
  - Base URL: `https://finance.naver.com`
  - Implementation: `backend/src/main/java/com/graphify/company/market/NaverFinanceQuoteClient.java`

- KRX (Korea Exchange) Open API - Official daily trading & closing prices
  - SDK/Client: Spring RestClient (native)
  - Auth: `KRX_API_KEY` environment variable
  - Config: `graphify.market.krx-api-key`, `KRX_API_BASE_URL`
  - Base URL: `https://data-dbg.krx.co.kr/svc/apis/sto` (development), production varies
  - Implementation: `backend/src/main/java/com/graphify/company/market/KrxOpenApiClient.java`

- CNN Fear & Greed Index - Market sentiment indicator
  - SDK/Client: Spring RestClient (native)
  - Config: `graphify.market.cnn-fear-greed-enabled`, `MARKET_CNN_FEAR_GREED_URL`
  - Base URL: `https://production.dataviz.cnn.io`
  - Implementation: `backend/src/main/java/com/graphify/home/sentiment/CnnFearGreedClient.java`

**News & Market Sentiment:**
- NewsAPI.org - English-language market news aggregation
  - SDK/Client: Spring RestClient (native)
  - Auth: `NEWS_API_KEY` environment variable (optional, RSS fallback if empty)
  - Config: `graphify.news.api-key`, `graphify.news.refresh-minutes` (default 15)
  - Implementation: `backend/src/main/java/com/graphify/home/news/NewsApiClient.java`

- RSS Feeds - Korean financial news (always available, no auth required)
  - Default feeds:
    - Hankyung: `https://www.hankyung.com/feed/economy`
    - MK: `https://www.mk.co.kr/rss/30000001/`
    - YNA: `https://www.yna.co.kr/rss/economy.xml`
  - SDK/Client: Rome 2.1.0 (RSS parser)
  - Implementation: `backend/src/main/java/com/graphify/home/news/RssNewsClient.java`

- Yahoo Finance Index Series - Market indices (KOSPI, etc.)
  - SDK/Client: Spring RestClient (native)
  - Implementation: `backend/src/main/java/com/graphify/home/sentiment/YahooIndexSeriesClient.java`

**Company Information & Filings:**
- DART (Data Analysis, Retrieval and Transfer System) - Korean corporate disclosures
  - SDK/Client: Spring RestClient (native)
  - Auth: `DART_API_KEY` environment variable
  - Config: `graphify.dart.api-key`, `graphify.dart.corp-code-cache-hours` (default 24), `graphify.dart.enrich-threshold`
  - Base URL: https://opendart.fss.or.kr (via RestClient)
  - Implementations:
    - `backend/src/main/java/com/graphify/company/registry/dart/DartCompanyRegistryClient.java` - Company lookup
    - `backend/src/main/java/com/graphify/company/registry/dart/DartDisclosureClient.java` - Filing data
    - `backend/src/main/java/com/graphify/company/registry/dart/DartFinancialClient.java` - Financial statements
  - Caching: Corp code list cached for 24 hours (configurable)

**AI & Insights:**
- Azure OpenAI / OpenAI-compatible API - Agent insights & chart analysis
  - SDK/Client: `ai` package (6.0.204) + custom Azure integration
  - Auth: `OPENAI_API_KEY` environment variable (api-key or bearer)
  - Config file: `backend/src/main/java/com/graphify/config/GraphifyOpenAiProperties.java`
  - Configuration:
    - `OPENAI_BASE_URL` - API endpoint
    - `OPENAI_API_KEY` - Authentication key
    - `OPENAI_API_VERSION` - Default: `2024-12-01-preview`
    - `OPENAI_AUTH_MODE` - `api-key` (default) or `bearer`
    - `OPENAI_DEPLOYMENT` - Azure deployment name (defaults to `OPENAI_MODEL` if empty)
    - `OPENAI_MODEL` - Model name, default: `gpt-4o`
    - `OPENAI_FALLBACK_DEPLOYMENT` - Fallback model, default: `gpt-4o`
    - `OPENAI_MAX_TOKENS` - Context limit, default: 4096
  - Implementation: `backend/src/main/java/com/graphify/agent/AzureChatCompletionClient.java`
  - Frontend streaming: `frontend/src/lib/agentStream.ts` uses EventSource for SSE

## Data Storage

**Databases:**
- PostgreSQL 12+ (primary)
  - Connection: `SPRING_DATASOURCE_URL` environment variable
  - Connection: `jdbc:postgresql://localhost:5432/graphify` (dev default)
  - Client: Hibernate JPA via Spring Data
  - Schema: Managed by Flyway migrations at `backend/src/main/resources/db/migration/`
  - Connection pooling: HikariCP with configurable pool size (`DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`)
  - Recommended for production: Supabase Transaction Pooler (port 6543) for Cloud Run scaling

**File Storage:**
- Local filesystem only (no cloud storage integration)

**Caching:**
- In-memory caching for DART corp codes (24 hours, configurable)
- React Query client-side caching for API responses

## Authentication & Identity

**Auth Provider:**
- Custom JWT implementation (no OAuth provider required)
  - Implementation: `backend/src/main/java/com/graphify/auth/JwtTokenService.java`
  - Token structure: Access (15 min default) + Refresh (7 days default)
  - Config: `graphify.auth.jwt-secret`, `graphify.auth.access-expiration-minutes`, `graphify.auth.refresh-expiration-days`

**OAuth Integrations (Social Login):**
- Generic OAuth2 state-based flow (provider-agnostic)
  - State store: `backend/src/main/java/com/graphify/auth/OAuthStateStore.java`
  - Endpoints: `/api/v1/auth/oauth/{provider}/url`, `/api/v1/auth/oauth/{provider}/authorize`
  - Implementation: `backend/src/main/java/com/graphify/auth/AuthService.java`
  - Frontend: `frontend/src/lib/authApi.ts` → `fetchOAuthUrl(provider)`
  - Email-based login: Also supported (`/api/v1/auth/login`)

**Frontend Auth:**
- Token storage: localStorage (`graphify.accessToken`, `graphify.refreshToken`)
- Authorization: Bearer token in HTTP Authorization header
- Client: `frontend/src/lib/apiClient.ts` handles token injection & refresh

## Monitoring & Observability

**Error Tracking:**
- None detected (relies on Spring logs)

**Logs:**
- Spring Boot logging with configurable levels per profile
- Application logs: `LOG_LEVEL_APP` (default: INFO)
- Web request logs: `LOG_LEVEL_WEB` (default: WARN)
- Levels can be overridden in production via env vars

**Health Monitoring:**
- Spring Actuator endpoints enabled: `/actuator/health`, `/actuator/info`
- Health detail visibility: `when_authorized` (requires auth)

## CI/CD & Deployment

**Hosting:**
- Google Cloud Run (primary target)
  - Database: Supabase PostgreSQL recommended
  - Instance scaling: HikariCP pool sized for multi-instance deployments (recommend max-pool-size: 5)

**CI Pipeline:**
- None detected in configuration (GitHub Actions/other CI not configured in repo)

## Environment Configuration

**Required env vars (Production):**
- `SPRING_DATASOURCE_URL` - PostgreSQL connection string
- `SPRING_DATASOURCE_USERNAME` - Database user
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `GRAPHIFY_JWT_SECRET` - JWT signing key (256+ bits recommended)
- `GRAPHIFY_SECRETS_ENCRYPTION_KEY` - AES encryption key for stored secrets (256+ bits)
- `FRONTEND_BASE_URL` - Frontend origin for redirect URLs
- `API_PUBLIC_BASE_URL` - Public API URL for OAuth callbacks

**Optional env vars (Feature Flags):**
- `DART_API_KEY` - DART.fss.or.kr API key (corp registry enrichment)
- `NEWS_API_KEY` - NewsAPI.org key (English news, fallback to RSS)
- `KRX_API_KEY` - KRX Open API key (official trading data)
- `OPENAI_BASE_URL`, `OPENAI_API_KEY` - Azure OpenAI for agent insights
- `MARKET_PROVIDER` - `composite` (default), `yahoo`, `krx`, or `naver`
- `MARKET_YAHOO_ENABLED`, `MARKET_NAVER_ENABLED`, `MARKET_CNN_FEAR_GREED_ENABLED` - Feature toggles
- `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE` - Connection pool tuning
- `GRAPHIFY_CORS_ALLOWED_ORIGINS` - Comma-separated CORS origins

**Secrets location:**
- Development: `.env` file (Git-ignored)
- Production: Environment variables injected at runtime (Cloud Run / container platform)

## Webhooks & Callbacks

**Incoming:**
- OAuth callback endpoint: `/api/v1/auth/oauth/{provider}/authorize` - POST to complete social login
- Agent insights stream: `/api/v1/agent/stream/{sessionId}` - Server-Sent Events (SSE)

**Outgoing:**
- None detected

---

*Integration audit: 2026-06-20*
