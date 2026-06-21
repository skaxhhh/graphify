---
phase: 05-toss-oauth-credentials
verified: 2026-06-21T00:00:00Z
status: passed
score: 8/8 must-haves verified
human_verification:
  - test: "TossSettingsPage credential save round-trip"
    expected: "Saving client_id/secret shows '설정됨' badge; DB stores only encrypted Base64 (no plaintext)"
    why_human: "Requires running app + real Toss Open API credentials; DB inspection needed to confirm no plaintext"
  - test: "Token auto-issue + balance fetch against live Toss API"
    expected: "Manual refresh issues a real OAuth token; balance section lists real accounts"
    why_human: "Depends on external Toss Securities Open API; cannot exercise HTTP call statically"
  - test: "401 retry-once behavior"
    expected: "Expired/revoked token triggers single re-issue then retry; second failure shows error message"
    why_human: "Requires inducing a 401 from the live external API"
---

# Phase 5: 토스증권 OAuth & 자격증명 관리 Verification Report

**Phase Goal:** 사용자가 토스증권 client_id와 client_secret을 안전하게 등록하고, 시스템이 OAuth 액세스 토큰을 자동 발급·갱신하며, 연동된 실계좌 잔고를 대시보드에서 조회할 수 있다.
**Verified:** 2026-06-21
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth | Status | Evidence |
| --- | ----- | ------ | -------- |
| 1   | V34 migration creates toss_credentials table with encrypted columns | ✓ VERIFIED | `V34__toss_credentials.sql`: `user_id BIGINT UNIQUE`, `client_id_encrypted`/`client_secret_encrypted` TEXT NOT NULL, `access_token_encrypted`/`token_expires_at` nullable, index on user_id |
| 2   | TossCredential entity stores all encrypted + token fields | ✓ VERIFIED | `TossCredential.java`: @Entity mapped to toss_credentials, all 5 columns + @PrePersist/@PreUpdate timestamps; setters for token re-issue |
| 3   | saveCredentials encrypts and upserts | ✓ VERIFIED | `TossCredentialService.saveCredentials()`: findByUserId upsert, `encryptionService.encrypt(clientId/secret)`, clears token to force re-issue, validates blank inputs |
| 4   | getDecryptedClientId/Secret decrypts for use | ✓ VERIFIED | `getDecryptedClientId/Secret()` call `encryptionService.decrypt()`; findOrThrow → ERR_TOSS_002 NOT_FOUND |
| 5   | TossTokenManager.ensureValidToken issues/refreshes via Toss OAuth | ✓ VERIFIED | `ensureValidToken()` returns cached valid token else `issueToken()`; `issueToken()` POSTs form-urlencoded grant_type=client_credentials to `openapi.tossinvest.com/api/v1/oauth2/token`, encrypts + persists token with expiry |
| 6   | @Scheduled pre-emptive refresh (10-min window) | ✓ VERIFIED | `@Scheduled(fixedDelay=300_000) refreshExpiringSoon()` queries `findExpiringSoon(now, now+10min)`, re-issues per credential, swallows per-credential failures with warn log; @EnableScheduling present in SchedulerConfig |
| 7   | Real-account balance API with 401 retry-once | ✓ VERIFIED | `TossAccountService.getAccounts()`: empty list if unconfigured; ensureValidToken → GET accounts with Bearer; `onStatus` 401 → UnauthorizedException → issueToken force-refresh → retry once → BAD_GATEWAY on second failure |
| 8   | TossSettingsPage routed + functional; nav + dashboard wired | ✓ VERIFIED | Route `path:"settings" → <TossSettingsPage/>` (no ModeGuard); `commonItems` has `{to:"/trading/settings", label:"토스 설정"}`; PaperDashboard renders `<TossBalanceSection/>` |

**Score:** 8/8 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `db/migration/V34__toss_credentials.sql` | toss_credentials table | ✓ VERIFIED | UNIQUE user_id, encrypted TEXT cols, index |
| `toss/TossCredential.java` | JPA entity | ✓ VERIFIED | All fields + lifecycle callbacks + setters |
| `toss/TossCredentialRepository.java` | findByUserId + expiry query | ✓ VERIFIED | findByUserId + findExpiringSoon JPQL |
| `toss/TossCredentialService.java` | save/decrypt operations | ✓ VERIFIED | encrypt-on-save, decrypt-on-use, getStatus, isTokenValid (1-min buffer) |
| `toss/TossTokenManager.java` | OAuth issue + scheduled refresh | ✓ VERIFIED | ensureValidToken + issueToken + @Scheduled refreshExpiringSoon |
| `toss/TossCredentialController.java` | REST credential endpoints | ✓ VERIFIED | GET /status, POST /, POST /token/refresh; userId via HistoryService |
| `toss/dto/TossCredentialRequest.java` | request record | ✓ VERIFIED | (clientId, clientSecret) |
| `toss/dto/TossCredentialStatusDto.java` | status record | ✓ VERIFIED | (configured, tokenValid, tokenExpiresAt) |
| `toss/TossAccountService.java` | balance query + 401 retry | ✓ VERIFIED | RestClient onStatus 401 marker + retry-once |
| `toss/TossAccountController.java` | GET /accounts | ✓ VERIFIED | @GetMapping → ApiResponse<List<TossAccountDto>> |
| `toss/dto/TossAccountDto.java` | account record | ✓ VERIFIED | (accountNumber, accountName, balance, availableBalance) |
| `frontend/lib/tossApi.ts` | API client functions | ✓ VERIFIED | fetchTossStatus/saveTossCredentials/refreshTossToken/fetchTossAccounts; apiPost<T,B> order correct |
| `frontend/pages/trading/TossSettingsPage.tsx` | form + status + refresh | ✓ VERIFIED | useQuery status, 2 password inputs, badge (gray/green/yellow), manual refresh button, KST expiry, message state |
| `frontend/layouts/TradingLayout.tsx` | nav item | ✓ VERIFIED | 토스 설정 in commonItems (both modes) |
| `frontend/router/index.tsx` | settings route | ✓ VERIFIED | settings path, no ModeGuard |
| `frontend/.../PaperDashboardPage.tsx` | Toss balance section | ✓ VERIFIED | collapsible TossBalanceSection, empty state link, account table |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| TossCredentialService | SecretEncryptionService | encrypt/decrypt | ✓ WIRED | AES-256-GCM (SHA-256 → 32-byte key, 12-byte IV prepended, 128-bit tag) |
| TossCredentialController | HistoryService.requireCurrentUserId() | per-user scoping | ✓ WIRED | All 3 endpoints scope to current user |
| TossTokenManager.refreshExpiringSoon | Spring scheduler | @Scheduled + @EnableScheduling | ✓ WIRED | SchedulerConfig @EnableScheduling enables fixedDelay job |
| TossTokenManager.issueToken | Toss OAuth endpoint | RestClient POST form-urlencoded | ✓ WIRED | client_credentials grant; encrypts + persists token |
| TossAccountService | TossTokenManager.ensureValidToken/issueToken | bearer token + 401 retry | ✓ WIRED | onStatus 401 → force re-issue → retry once |
| TossAccountController | TossAccountService.getAccounts | @GetMapping | ✓ WIRED | returns ApiResponse list |
| TossSettingsPage | tossApi (status/save/refresh) | useQuery + useMutation | ✓ WIRED | save/refresh invalidate ["toss","status"] |
| TossSettingsPage | router /trading/settings | route element | ✓ WIRED | reachable, no ModeGuard (both modes) |
| TradingLayout | /trading/settings | commonItems nav | ✓ WIRED | visible in PAPER and LIVE |
| PaperDashboardPage | fetchTossAccounts | TossBalanceSection useQuery | ✓ WIRED | independent query, retry:false, graceful empty state |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| TOSS-01 | 05-01 | client_id/secret 등록 (AES-256-GCM 암호화 저장) | ✓ SATISFIED | saveCredentials + SecretEncryptionService + V34 encrypted cols |
| TOSS-02 | 05-01 | OAuth 토큰 자동 발급 + 만료 10분 전 선제 갱신 | ✓ SATISFIED | issueToken + @Scheduled refreshExpiringSoon(now+10min) |
| TOSS-03 | 05-02 | 실계좌 잔고 대시보드 조회 | ✓ SATISFIED | TossAccountService/Controller + TossBalanceSection |

No orphaned requirements — all three TOSS-01..03 declared in plan frontmatter and marked Complete in REQUIREMENTS.md (lines 62-64, 147-149).

### Anti-Patterns Found

None. The only "placeholder" grep hits in TossSettingsPage are legitimate HTML `placeholder=` input attributes and Tailwind `placeholder-gray-500` classes — not stubs. No TODO/FIXME/HACK/"Not implemented" in any Phase 5 file.

### Human Verification Required

These require a running app and real Toss Securities Open API credentials (external service — not statically verifiable):

1. **Credential save round-trip** — Save client_id/secret on TossSettingsPage; confirm badge flips to 설정됨 and the DB `toss_credentials` row contains only Base64 ciphertext (no plaintext).
2. **Token auto-issue + balance** — Click 토큰 수동 갱신; confirm a real token is issued and the PaperDashboard 토스 실계좌 잔고 section lists accounts.
3. **401 retry-once** — Induce an expired/revoked token; confirm a single re-issue + retry, and an error message on a second failure.

### Gaps Summary

No gaps. All 8 observable truths verified at exists/substantive/wired levels. All 16 artifacts present and substantive. All key links connected. `./gradlew compileJava` BUILD SUCCESSFUL and frontend `tsc --noEmit` exit 0. AES-256-GCM encryption confirmed end-to-end (SHA-256-derived 256-bit key, random IV, GCM tag). The scheduler is enabled via SchedulerConfig @EnableScheduling. All three requirements (TOSS-01..03) satisfied.

**Notable (non-blocking):**
- **No Toss-specific unit tests.** Plans 05-01/05-02 required only `./gradlew test` (no regressions) and did not mandate new tests; both summaries report BUILD SUCCESSFUL. Given the OAuth flow, 401-retry, and encryption complexity, targeted unit tests (token expiry boundary, 401 retry path, encrypt/decrypt round-trip) would be valuable in a hardening pass but are out of Phase 5 scope.
- **Default dev encryption key** (`dev-only-change-me-...`) is hardcoded as the property default; prod correctly overrides via `${GRAPHIFY_SECRETS_ENCRYPTION_KEY}` in application-prod.yml. Ensure the env var is set in production.
- `getDecryptedClientId/Secret` in TossCredentialService have no production callers (TossTokenManager decrypts directly from the entity); harmless dead-ish accessors that match the planned service contract.

---

_Verified: 2026-06-21_
_Verifier: Claude (gsd-verifier)_
