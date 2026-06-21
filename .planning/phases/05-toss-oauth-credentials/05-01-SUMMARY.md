---
phase: "05"
plan: "01"
subsystem: "toss-credentials"
tags: [oauth, aes-gcm, jpa, scheduler, credential-storage]
dependency_graph:
  requires: [04-04]
  provides: [TossCredential, TossCredentialRepository, TossCredentialService, TossTokenManager, TossCredentialController, V34 migration]
  affects: [05-02]
tech_stack:
  added: []
  patterns: [AES-GCM upsert credential pattern (reuses SecretEncryptionService), OAuth client_credentials flow, @Scheduled pre-emptive token refresh]
key_files:
  created:
    - backend/src/main/resources/db/migration/V34__toss_credentials.sql
    - backend/src/main/java/com/graphify/toss/TossCredential.java
    - backend/src/main/java/com/graphify/toss/TossCredentialRepository.java
    - backend/src/main/java/com/graphify/toss/TossCredentialService.java
    - backend/src/main/java/com/graphify/toss/TossTokenManager.java
    - backend/src/main/java/com/graphify/toss/TossCredentialController.java
    - backend/src/main/java/com/graphify/toss/dto/TossCredentialRequest.java
    - backend/src/main/java/com/graphify/toss/dto/TossCredentialStatusDto.java
  modified: []
decisions:
  - "reuse existing SecretEncryptionService (AES-256-GCM) — no new crypto code needed"
  - "findExpiringSoon JPQL query on TossCredentialRepository — avoids loading all rows for refresh scheduler"
  - "saveCredentials clears token on credential update — forces re-issue to ensure new credentials are validated immediately"
  - "TossTokenResponse as private record inside TossTokenManager — keeps Toss API shape internal"
metrics:
  duration: "6m"
  completed_date: "2026-06-21"
  tasks_completed: 2
  files_created: 8
  files_modified: 0
---

# Phase 05 Plan 01: Toss Credential Storage & Token Manager Summary

**One-liner:** Toss Securities credential storage with AES-256-GCM encryption (V34 migration), OAuth client_credentials token manager with @Scheduled pre-emptive refresh (10-min window), and REST API for credential save/status/manual-refresh.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | V34 migration + TossCredential entity + repository | 3829d4e | V34 SQL + TossCredential + TossCredentialRepository |
| 2 | TossCredentialService + TossTokenManager + Controller + DTOs | 3829d4e | TossCredentialService + TossTokenManager + TossCredentialController + 2 DTOs |

## What Was Built

**`V34__toss_credentials.sql`** — `toss_credentials` table: `user_id BIGINT UNIQUE`, `client_id_encrypted TEXT`, `client_secret_encrypted TEXT`, `access_token_encrypted TEXT (nullable)`, `token_expires_at TIMESTAMPTZ (nullable)`, `created_at`, `updated_at`. Index on `user_id`.

**`TossCredential`** — JPA entity with `@PrePersist`/`@PreUpdate` for timestamps.

**`TossCredentialRepository`** — `findByUserId(Long)` + `findExpiringSoon(Instant from, Instant to)` JPQL.

**`TossCredentialService`**:
- `saveCredentials(userId, request)`: upsert with both fields encrypted; clears existing token to force re-issue
- `getStatus(userId)`: `TossCredentialStatusDto(configured, tokenValid, tokenExpiresAt)`
- `getDecryptedClientId/Secret(userId)`: decrypt for token manager use
- `isTokenValid(cred)`: static helper — token exists AND expires > now+1min

**`TossTokenManager`**:
- `ensureValidToken(userId)`: returns cached decrypted token if valid, else calls `issueToken`
- `issueToken(cred)`: POST form-urlencoded to `https://openapi.tossinvest.com/api/v1/oauth2/token`; encrypts response token, sets `tokenExpiresAt = now + expires_in`
- `@Scheduled(fixedDelay=300_000)` `refreshExpiringSoon()`: finds credentials expiring within 10 minutes, calls `issueToken` per credential, swallows exceptions with warn log

**`TossCredentialController`**:
- `GET /api/v1/toss/credentials/status`
- `POST /api/v1/toss/credentials`
- `POST /api/v1/toss/credentials/token/refresh`

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check

Files exist:
- backend/src/main/resources/db/migration/V34__toss_credentials.sql — FOUND
- backend/src/main/java/com/graphify/toss/TossTokenManager.java — FOUND
- backend/src/main/java/com/graphify/toss/TossCredentialController.java — FOUND

Commits: 3829d4e — feat(05-01): Toss credentials storage + OAuth token manager

Full test suite: BUILD SUCCESSFUL (7s)

## Self-Check: PASSED
