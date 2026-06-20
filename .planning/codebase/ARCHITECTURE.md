# Architecture

**Analysis Date:** 2026-06-20

## Pattern Overview

**Overall:** Layered monolithic architecture with clear separation between REST API backend (Spring Boot) and React frontend, communicating via JSON APIs.

**Key Characteristics:**
- Backend: Spring Boot 3.4.5 with MVC pattern (Controllers → Services → Repositories)
- Frontend: React 18 with TypeScript, Zustand state management, React Router navigation
- Database: PostgreSQL with Flyway migrations (29 versions)
- Real-time: Agent streaming via `/api/v1/agent/stream/**` endpoints
- Authentication: JWT tokens with role-based access control (USER/ADMIN)

## Layers

**Backend API Layer:**
- Purpose: HTTP REST endpoints exposing business operations
- Location: `backend/src/main/java/com/graphify/{module}/{Module}Controller.java`
- Contains: 21 controllers handling requests (HomeController, AuthController, CompanyController, TradingController, etc.)
- Depends on: Service layer
- Used by: Frontend via fetch/React Query

**Backend Service Layer:**
- Purpose: Business logic and orchestration
- Location: `backend/src/main/java/com/graphify/{module}/{Module}Service.java`
- Contains: ~37 services implementing domain logic, caching, external API calls
- Depends on: Repository layer, external clients (Naver, DART, Market data providers)
- Used by: Controller layer

**Backend Repository Layer:**
- Purpose: JPA data access abstraction
- Location: `backend/src/main/java/com/graphify/{module}/{Module}Repository.java`
- Contains: Spring Data JPA repositories for entities
- Depends on: Entity/JPA layer, PostgreSQL
- Used by: Service layer

**Backend Entity Layer:**
- Purpose: Persistent domain objects
- Location: `backend/src/main/java/com/graphify/{module}/{Entity}.java`
- Contains: JPA-annotated entity classes with `@Entity` decorator
- Key entities: `User`, `Company`, `TradingRule`, `AnalysisHistory`, `MarketNews`, `WatchlistItem`
- Depends on: JPA, Jakarta Persistence API

**Backend Configuration Layer:**
- Purpose: Spring configuration, security, properties binding
- Location: `backend/src/main/java/com/graphify/config/{Config}.java`
- Contains: `SecurityConfig` (JWT filter, CORS, session management), `CorsConfig`, client configs for external APIs
- Key files: `SecurityConfig.java`, `GraphifyAuthProperties.java`, `GraphifyOpenAiProperties.java`, `MarketClientConfig.java`

**Backend Common Layer:**
- Purpose: Shared utilities, exceptions, DTOs
- Location: `backend/src/main/java/com/graphify/common/`
- Contains: `ApiResponse` record for consistent response format, `GraphifyException` for error handling, security utilities
- Key files: `common/dto/ApiResponse.java`, `common/exception/GlobalExceptionHandler.java`

**Frontend Page Layer:**
- Purpose: Route-specific pages
- Location: `frontend/src/pages/{Page}Page.tsx`
- Contains: ~20 page components (HomePage, LoginPage, CompanyDetailPage, TradingDashboardPage, etc.)
- Depends on: Components, stores, hooks
- Used by: Router

**Frontend Component Layer:**
- Purpose: Reusable UI components organized by feature domain
- Location: `frontend/src/components/{domain}/{Component}.tsx`
- Contains: Feature-specific components (home/, auth/, trading/, company/, admin/, etc.)
- Key domains: `trading/`, `admin/`, `company/`, `search/`, `history/`, `shared/`

**Frontend Layout Layer:**
- Purpose: Route container layouts
- Location: `frontend/src/layouts/{Layout}Layout.tsx`
- Contains: 4 layouts (GuestLayout, UserAppLayout, AdminLayout, TradingLayout)
- Defines: Header, footer, navigation structure for different route groups

**Frontend State Management:**
- Purpose: Client-side state via Zustand stores
- Location: `frontend/src/stores/{module}Store.ts`
- Contains: `authStore.ts` (auth state), `tradingStore.ts` (trading mode)
- Pattern: Create store with actions; subscribe in components

**Frontend API/Network Layer:**
- Purpose: HTTP client abstraction
- Location: `frontend/src/lib/{module}Api.ts`
- Contains: Typed fetch wrappers (authApi, userApi, homeApi, tradingApi, etc.)
- Pattern: Fetch wrappers with React Query integration via `@tanstack/react-query`

**Frontend Hooks Layer:**
- Purpose: Custom React hooks for reusable logic
- Location: `frontend/src/hooks/{hook}.ts`
- Contains: `useAuthHydration`, `useRecentSearches`, `useDebounce`, etc.

**Frontend Types Layer:**
- Purpose: TypeScript type definitions
- Location: `frontend/src/types/{module}.ts`
- Contains: Request/response DTOs matching backend contracts

## Data Flow

**Authentication Flow:**

1. User submits email/password on LoginPage
2. Frontend calls `authApi.login(email, password)` (fetch to `/api/v1/auth/login`)
3. Backend AuthService validates credentials, returns JWT tokens
4. Frontend stores tokens in localStorage via `authStore.setSession()`
5. Subsequent requests include `Authorization: Bearer {token}` header
6. Backend `JwtAuthenticationFilter` validates token on each request
7. On logout, tokens cleared from localStorage

**Company Graph Visualization Flow:**

1. User navigates to `/companies/{companyId}/graph`
2. Page fetches company data via `graphApi` (calls `/api/v1/companies/{id}/graph`)
3. Backend CompanyController calls CompanyService.getCompanyGraph()
4. Service queries Company repository + relationship graph data
5. Returns `CompanyGraphDto` with nodes and edges
6. Frontend XYFlow renders graph visualization
7. User can drill into relationships to see company connections

**Agent Streaming Flow (Real-time Chat/Analysis):**

1. User submits question on TradingChatPage or SearchResultPage
2. Frontend calls `agentStream(prompt)` which opens EventSource to `/api/v1/agent/stream/**`
3. Backend AgentStreamController opens connection, streams responses via SSE
4. Messages accumulated in component state as they arrive
5. Streamed content renders incrementally (AI response tokens as they generate)
6. Connection closes when stream completes

**Trading Mode Flow:**

1. User selects trading mode (PAPER or LIVE) on TradingLayout
2. Frontend TradingController PUT `/api/v1/trading/mode` with mode
3. Backend stores mode in User.tradingMode column
4. ModeGuard component on trading routes checks current mode
5. Routes only render if mode matches (PAPER routes vs LIVE routes)
6. User sees appropriate dashboard (paper trading with simulation vs live monitoring)

**Market Data & News Refresh:**

1. HomeService maintains `AtomicReference<Instant> lastNewsRefresh`
2. On `getMarketNews()` call, checks if data stale (> 5 minutes)
3. If stale, acquires synchronized lock and calls `MarketNewsIngestionService.refreshFromProviders()`
4. Service fetches news from external providers (RSS, APIs)
5. Stores MarketNews entities in database
6. Returns latest news to frontend
7. Frontend caches in React Query with refetch settings

**Search & Resolution:**

1. User types query on search bar (frontend SearchResultPage)
2. Debounced call to `searchApi.search(query)` → `/api/v1/search`
3. Backend SearchController queries Elasticsearch/DB for companies/articles
4. Returns `SearchResultDto` with ranked results
5. User clicks result → navigates to `/companies/{id}` or `/history/{sessionId}`

## Key Abstractions

**ApiResponse<T>:**
- Purpose: Unified response envelope across all APIs
- Examples: `backend/src/main/java/com/graphify/common/dto/ApiResponse.java`
- Pattern: All endpoints return `ApiResponse<T>` where success=true/false, data={payload}, error={code, message}

**DTOs (Data Transfer Objects):**
- Purpose: Contract between frontend and backend
- Examples: `CompanyGraphDto`, `TradingSettingsDto`, `AnalysisHistoryDto`
- Location: `{module}/dto/{Entity}Dto.java`
- Pattern: Records or classes with explicit fields for serialization

**Repository Abstraction:**
- Purpose: Abstract database access
- Examples: `UserRepository`, `CompanyRepository`, `TradingRuleRepository`
- Pattern: Extend `JpaRepository<Entity, ID>` with custom query methods

**Service Abstraction:**
- Purpose: Encapsulate domain logic and orchestration
- Examples: `HomeService`, `CompanyService`, `TradingSettingsService`
- Pattern: `@Service` class with dependencies injected via constructor

**External Client Integration:**
- Purpose: Abstract third-party API calls
- Examples: `NaverPopularSearchClient`, `YahooFinanceChartClient`, market data client
- Location: `{module}/` subdirectories or `admin/` subdirectories
- Pattern: Bean injected into services, error handling via `GraphifyException`

**Guard Components (Frontend):**
- Purpose: Conditional rendering based on auth/permissions
- Examples: `RequireAuth`, `RequireAdmin`, `ModeGuard`
- Pattern: Wrapper component that checks state, renders children or redirects

## Entry Points

**Backend Main Entry:**
- Location: `backend/src/main/java/com/graphify/GraphifyApplication.java`
- Triggers: `java -jar build/libs/graphify-api-*.jar`
- Responsibilities: Spring Boot application startup, enables config properties, runs on port 8080

**Frontend Main Entry:**
- Location: `frontend/src/main.tsx`
- Triggers: `npm run dev` (Vite dev server on 5173)
- Responsibilities: Initializes React root, hydrates auth state, mounts RouterProvider with router config

**API Entry Points (Controllers):**
- `/api/v1/home/*` → HomeController (trending, news, sentiment)
- `/api/v1/auth/*` → AuthController (login, oauth, password reset)
- `/api/v1/companies/*` → CompanyController (details, graph, insights)
- `/api/v1/trading/*` → TradingController (settings, mode)
- `/api/v1/admin/*` → AdminController (dashboard, users, prompts, vectordb)
- `/api/v1/search/*` → SearchController (semantic search)
- `/api/v1/history/*` → HistoryController (analysis sessions)
- `/api/v1/trading/rules/*` → PaperRuleController (trading rules)
- `/api/v1/trading/backtest/*` → BacktestController (backtesting)

**Frontend Routes (via Router):**
- `GuestLayout` routes: `/`, `/login`, `/search`, `/companies/{id}`, `/companies/{id}/graph`
- `UserAppLayout` routes: `/app/history`, `/app/watchlist`, `/app/mypage`
- `TradingLayout` routes: `/trading/*` (all trading features under this layout)
- `AdminLayout` routes: `/admin/*` (admin dashboard, users, settings)

## Error Handling

**Strategy:** Exception-based with centralized handler

**Patterns:**

- **Backend:** Services throw `GraphifyException(code, message, httpStatus)` which is caught by `GlobalExceptionHandler` and converted to `ApiResponse<T>` with error field
- **Frontend:** API calls wrapped in try-catch, errors passed to React Query error boundary or component error states
- **HTTP Status:** 4xx for client errors (validation, auth), 5xx for server errors
- **Logging:** Errors logged via Spring logging framework (SLF4J)

## Cross-Cutting Concerns

**Logging:** SLF4J via Spring Boot (configured in `application.yml`)
- Services log business operations: "Retrieved X companies"
- Controllers log request/response timing
- Exceptions logged with stack traces

**Validation:** 
- Backend: `@Valid` on controller params, custom validators for domain rules
- Frontend: Zod schema validation in form components (tsconfig imports `zod`)

**Authentication:**
- Backend: `JwtAuthenticationFilter` (token extraction, validation, context setting)
- Frontend: `useAuthStore` hydrated on app start, `RequireAuth` guards on routes
- JWT includes user ID, email, role; refreshed via OAuth callback

**Security:**
- CORS configured: allows localhost:5173 in dev
- CSRF disabled (stateless JWT)
- HSTS headers enabled in `SecurityConfig`
- Content-Type protection, clickjacking prevention via X-Frame-Options

**Database Migrations:**
- Flyway manages schema: `V{N}__{description}.sql` files
- 29 versions from baseline to trading features
- Location: `backend/src/main/resources/db/migration/`
- Automatic on startup via Spring Boot config

---

*Architecture analysis: 2026-06-20*
