# Technology Stack

**Analysis Date:** 2026-06-20

## Languages

**Primary:**
- Java 21 - Backend (Spring Boot) - `build.gradle.kts`
- TypeScript 5.6.3 - Frontend (React + Vite) - `frontend/package.json`

**Secondary:**
- SQL - Database migrations (PostgreSQL) - `backend/src/main/resources/db/migration/`

## Runtime

**Environment:**
- Spring Boot 3.4.5 - Backend runtime
- Node.js (implicit via npm) - Frontend tooling
- PostgreSQL - Data persistence

**Package Manager:**
- npm (Frontend) - `frontend/package.json`
- Gradle 8.x (Backend) - `backend/build.gradle.kts`
- Maven Central (Backend dependencies)
- Flyway 10.x - Database migration management

## Frameworks

**Core:**
- Spring Boot 3.4.5 - Web framework, dependency injection
- Spring Data JPA - ORM layer
- Spring Security - Authentication & authorization
- Spring Web - REST controllers
- Spring Actuator - Application monitoring
- React 18.3.1 - Frontend UI framework
- Vite 6.0.3 - Frontend bundler & dev server

**Testing:**
- JUnit 5 (Platform Launcher) - Backend testing - `build.gradle.kts`
- Spring Boot Test - Integrated testing - `build.gradle.kts`
- Spring Security Test - Auth testing - `build.gradle.kts`
- Playwright 1.51.0 - E2E testing - `frontend/package.json`

**Build/Dev:**
- Vite 6.0.3 - Frontend dev server & bundler
- Tailwind CSS 3.4.16 - Frontend styling - `frontend/package.json`
- PostCSS 8.4.49 - CSS processing - `frontend/package.json`
- Autoprefixer 10.4.20 - CSS vendor prefixes - `frontend/package.json`
- @vitejs/plugin-react 4.3.4 - React fast refresh - `frontend/package.json`
- TypeScript 5.6.3 - Type checking - `frontend/package.json`

## Key Dependencies

**Critical:**
- io.jsonwebtoken:jjwt 0.12.6 - JWT token generation & validation
- org.springframework.boot:spring-boot-starter-security - Security framework
- org.springframework.boot:spring-boot-starter-data-jpa - Database ORM
- org.postgresql:postgresql - PostgreSQL JDBC driver
- org.flywaydb:flyway-core - Database migration framework
- com.rometools:rome 2.1.0 - RSS feed parsing - `build.gradle.kts`

**Frontend UI & State:**
- @tanstack/react-query 5.62.8 - Server state management & caching
- zustand 5.0.2 - Client state management
- react-router-dom 6.28.0 - Client-side routing
- @xyflow/react 12.10.2 - Graph visualization (company relationships)
- ai 6.0.204 - Streaming AI responses (Vercel SDK)

**Infrastructure:**
- Spring Boot Starter Web - HTTP server
- Spring Boot Starter Validation - Input validation
- Spring Boot Starter Actuator - Health checks & metrics

## Configuration

**Environment:**
- Application profiles: `dev`, `prod` - Separate configs at `backend/src/main/resources/application-{profile}.yml`
- Frontend env: `VITE_API_BASE_URL`, `VITE_LOGIN_EMAIL_ENABLED`, `VITE_LOGIN_OAUTH_ENABLED`
- Backend secrets loaded from `.env` file via Spring property import

**Build:**
- Backend: `build.gradle.kts` with Spring Boot plugin v3.4.5
- Frontend: `vite.config.ts` with React plugin, TypeScript target ES2022
- TypeScript config: `frontend/tsconfig.json` with strict mode enabled

**Environment Configuration Files:**
- `.env.example` - Root environment template (database, APIs, secrets)
- `.env` - Runtime environment (Git-ignored, created from `.env.example`)
- `frontend/.env.example` - Frontend-only variables
- `frontend/.env.development` - Frontend dev config
- `backend/src/main/resources/application.yml` - Base Spring config
- `backend/src/main/resources/application-dev.yml` - Development overrides (localhost defaults)
- `backend/src/main/resources/application-prod.yml` - Production overrides (env var references)

## Platform Requirements

**Development:**
- Java 21 runtime
- PostgreSQL 12+ (local or Docker)
- Node.js 16+ (for npm)
- Docker & Docker Compose (optional, for pgAdmin during `./init.sh start`)

**Production:**
- Cloud Run compatible (Java 21)
- PostgreSQL (Supabase recommended with Transaction Pooler on port 6543)
- Environment variables for secrets injection
- Database connection pooling: HikariCP with configurable pool size

---

*Stack analysis: 2026-06-20*
