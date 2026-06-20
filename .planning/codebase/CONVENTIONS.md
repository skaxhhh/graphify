# Coding Conventions

**Analysis Date:** 2026-06-20

## Naming Patterns

**Files:**
- TypeScript/React components: PascalCase for `.tsx` files (e.g., `Avatar.tsx`, `HomeController.tsx`)
- TypeScript utilities: camelCase for `.ts` files (e.g., `apiClient.ts`, `authStore.ts`)
- Java files: PascalCase class names matching filename (e.g., `User.java`, `HomeService.java`)
- Test files: Playwright specs use `.spec.ts` suffix (e.g., `company-detail-collected-data.spec.ts`)

**Functions:**
- camelCase for all functions in TypeScript/JavaScript
- Examples: `subscribeAgentStream()`, `maskEmail()`, `getTrendingCompanies()`
- Java methods: camelCase, public methods prefixed with action verbs (get, set, fetch, create, update)
- Examples: `getTrendingCompanies()`, `updateTradingAccess()`, `normalizeLimit()`

**Variables:**
- camelCase in TypeScript and Java
- Constants in Java: UPPER_SNAKE_CASE (e.g., `DEFAULT_TRENDING_LIMIT`, `DEFAULT_NEWS_LIMIT`)
- TypeScript constants: camelCase (e.g., `authStorageKeys`)
- Private/internal variables: camelCase with underscore prefix optional (e.g., `lastNewsRefresh`, `sentimentCache`)

**Types/Interfaces:**
- PascalCase for all type and interface names
- TypeScript interfaces: `AuthState`, `AvatarProps`, `LoginResponse`
- Java model classes: `User`, `Company`, `HomeService`
- DTO classes: PascalCase with `Dto` suffix (e.g., `AdminUserDto`, `BuzzCompanyDto`)
- Union types: PascalCase (e.g., `UserRole = "guest" | "user" | "admin"`)

**React Components:**
- Functional components: PascalCase (e.g., `function Avatar({...})`)
- Props interfaces: Suffix with `Props` (e.g., `AvatarProps`)
- Custom hooks: camelCase with `use` prefix (e.g., `useAuthStore`, `useQueryClient`)

## Code Style

**Formatting:**
- TypeScript: Vite project uses default formatting
- Java: Spring Boot project (Java 21)
- Indentation: 2 spaces for TypeScript, 4 spaces implied for Java
- Line length: No strict limit enforced, follows project defaults

**Linting:**
- TypeScript: No ESLint config detected, relies on TypeScript strict mode
- Java: Spring Boot conventions apply, no explicit linter config
- TypeScript tsconfig: Strict mode enabled
  - `strict: true`
  - `noUnusedLocals: true`
  - `noUnusedParameters: true`
  - `noFallthroughCasesInSwitch: true`
  - `noUncheckedIndexedAccess: true`

## Import Organization

**Order:**
1. React and framework imports (e.g., `import { create } from "zustand"`)
2. Third-party library imports (e.g., `import { QueryClient, QueryClientProvider } from "@tanstack/react-query"`)
3. Project internal imports using `@/` prefix
4. Type imports separated with `import type {...}`

**Example from `main.tsx`:**
```typescript
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { router } from "@/router";
import { useAuthStore } from "@/stores/authStore";
import "@/index.css";
```

**Example from `authStore.ts`:**
```typescript
import { create } from "zustand";
import { authStorageKeys } from "@/lib/apiClient";
import type { AuthProvider, AuthUser, LoginResponse } from "@/types/auth";
```

**Path Aliases:**
- Frontend uses `@/` alias pointing to `src/` directory (configured in `vite.config.ts` and `tsconfig.json`)
- All imports: `import { ... } from "@/lib/apiClient"`
- No relative imports (`../`) in application code

**Java imports:**
- Standard library imports first
- Then jakarta.* (jakarta.persistence, etc.)
- Then Spring Framework imports
- Then project imports
- No wildcard imports

## Error Handling

**TypeScript/React Patterns:**
- Custom error classes with code property for categorization
- Example: `ApiRequestError` in `apiClient.ts` with structure:
  ```typescript
  export class ApiRequestError extends Error {
    readonly code: string;
    constructor(code: string, message: string) {
      super(message);
      this.name = "ApiRequestError";
      this.code = code;
    }
  }
  ```
- Error codes follow pattern: `ERR_[CATEGORY]_[NUMBER]` (e.g., `ERR_AUTH_001`, `ERR_HTTP_404`)
- Errors include user-friendly messages in Korean
- Try-catch blocks used for async operations, silently catch when appropriate
- Example from `agentStream.ts`: `try { ... } catch { handlers.onError?.(); }`

**Java Patterns:**
- Custom `GraphifyException` used for application errors (imported across services)
- Controllers return wrapped responses: `ApiResponse<T>` with success/error fields
- Services use `@Transactional` annotations for database operations
- No explicit try-catch blocks visible; relies on Spring error handling
- Error messages returned in Korean language

## Logging

**Framework:** Console and browser DevTools for TypeScript

**Patterns:**
- No explicit logging framework detected in frontend
- Reliance on console API if needed
- Backend uses Spring Boot logging (standard slf4j)
- No logging statements visible in sample code

## Comments

**When to Comment:**
- JSDoc/JavaDoc used for public APIs and complex logic
- Type interfaces require no comments (types are self-documenting)
- Business logic comments in Korean (native language of team)
- Example from `HomeService.java`:
  ```java
  /** 룰에 등장하는 전 종목 일봉 적재. 적재된 종목 수 반환. */
  public int ingestAllRuleSymbolsDaily() { ... }
  ```

**JSDoc/TSDoc:**
- Not heavily used in simple functions
- Used for complex business logic and public API functions
- Type annotations serve as primary documentation

## Function Design

**Size:** 
- Small to medium functions preferred
- Helper functions extracted for reusable logic
- Example: `initials()` helper in `Avatar.tsx` is 7 lines for single responsibility
- Example: `roleFromUser()` in `authStore.ts` is 5 lines for clear logic

**Parameters:** 
- Objects preferred over multiple positional parameters
- Example: `updateTradingAccess(userId: number, tradingEnabled: boolean)`
- Destructuring used for complex props: `{ displayName, className = "" }`
- Default parameters supported: `className = ""`

**Return Values:** 
- Promise-based for async API calls returning `ApiResponse<T>`
- Typed returns: all functions have explicit return types
- Void functions for state mutations: `logout(): void`
- Conditional returns for validation: `AuthUser | null`

## Module Design

**Exports:** 
- Named exports preferred for functions and constants
- Default exports used for React components: `export function Avatar({...})`
- Type exports with `export type` for TypeScript types
- Example from `authStore.ts`: Named export `useAuthStore`, type exports

**Barrel Files:** 
- Not detected in codebase
- Each module exports its own API
- Router index collects all route definitions: `src/router/index.tsx`
- API utilities organized by feature (e.g., `adminApi.ts`, `searchApi.ts`)

## Zustand Store Pattern

**State Management:**
- Zustand used for global state (`useAuthStore`, `tradingStore`)
- Store creation: `create<StateType>((set) => ({ ... }))`
- Methods defined inline within store creator
- Hydration pattern: `hydrate()` method for server-side restoration
- LocalStorage persistence handled within store logic
- Type-safe state with explicit interface definitions

**Example from `authStore.ts`:**
```typescript
export const useAuthStore = create<AuthState>((set) => ({
  role: "guest",
  isAuthenticated: false,
  hydrated: false,
  // ... state fields
  hydrate: () => {
    const accessToken = localStorage.getItem(authStorageKeys.accessToken);
    // ... restoration logic
    set({ accessToken, refreshToken, user, role, isAuthenticated, hydrated: true });
  },
  setSession: (response) => {
    persistSession(response);
    set({ /* updated state */ });
  },
}));
```

## API Layer Pattern

**Request/Response Handling:**
- Generic API wrapper functions: `apiGet<T>()`, `apiPost<T, B>()`, `apiPut<T, B>()`
- All responses wrapped in `ApiResponse<T>` type with `success` and `error` fields
- Request headers include Authorization bearer token automatically
- Content-Type set to `application/json` for POST/PUT
- Example: `fetchAdminUsers()` returns `Promise<ApiResponse<AdminUser[]>>`

**Typed API Calls:**
- Response type specified in generic: `apiGet<AdminAgentStats>(...)`
- Request payload type specified: `apiPost<AdminUser, typeof data>(...)`
- All API functions organized by feature domain (e.g., `adminApi.ts`, `searchApi.ts`, `tradingApi.ts`)

---

*Convention analysis: 2026-06-20*
