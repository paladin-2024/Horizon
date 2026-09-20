# Horizon: Spring Boot backend and project restructure

Date: 2026-09-20
Status: draft, awaiting review

## Goal

Horizon is a platform where a user in Uganda or DR Congo links all of their bank and mobile money accounts and manages them in one place. The existing Next.js app has UI shells and mock data only; its planned server side (Appwrite, Plaid, Dwolla) is dropped. The backend becomes a Spring Boot service, and Next.js becomes a pure frontend.

## Constraints

- **The UI and colors must not change.** The existing layout, Tailwind theme, fonts, shadcn components and `globals.css` classes are kept as they are. New screens reuse them.
- Target users are in Uganda and DR Congo: currencies UGX, CDF and USD; languages English and French; phone-number identity; low bandwidth.
- No Plaid-style bank aggregator that reads balances and transactions was found for Uganda or DR Congo. v1 links accounts through manual entry and CSV statement import behind a provider interface. Mobile money APIs (for example pawaPay lists MTN and Airtel in Uganda, Orange and Vodacom in DRC) are payment APIs, and whether any of them can read balances and history is unverified.
- Git workflow (see `CLAUDE.md`): `main` is production, `dev` is staging, all work on `feat/<name>` branches merged by PR, no direct pushes, no Claude attribution on commits or PRs.

## Architecture

Modular monolith: one Spring Boot 3 app (Java 21, Maven) with one PostgreSQL 17 database. Feature packages keep their internals package-private; other modules use a module's public service only.

```
Horizon/
├── web/                      Next.js, UI only (moved from repo root)
├── api/                      Spring Boot
│   └── src/main/java/com/horizon/
│       ├── common/           Money, errors, security config
│       ├── auth/             register, login, SMS OTP, JWT
│       ├── user/             profile, national ID
│       ├── linking/          BankProvider, ManualProvider, CsvImportProvider
│       ├── account/          linked accounts, balances
│       └── transaction/      transactions, categories, import dedupe
├── docs/superpowers/specs/
├── docker-compose.yml        local Postgres 17
└── CLAUDE.md
```

Next.js proxies `/api/*` to Spring, so the browser talks to one origin, the auth cookie is `httpOnly`, and CORS is not needed. `lib/actions/user.action.ts` and the Appwrite, Plaid and Dwolla assumptions in `types/` are removed when the auth slice wires the frontend to the API.

### Code conventions

- JPA entities are ordinary classes (Hibernate needs a non-final class with a no-arg constructor).
- Everything else is a record: request and response DTOs, `@ConfigurationProperties`, domain events, and `Money` (an embeddable record of `amountMinor` and `currency`).
- Hibernate creates tables from the entities in local dev and tests. Staging and production use Flyway migrations with `ddl-auto=validate`; the first migration is generated from the entities.

## Data model (PostgreSQL 17)

| Table | Key columns | Indexes |
|---|---|---|
| `users` | phone (E.164), email (stored lowercase), password_hash, names, national_id (encrypted), country (UG/CD), language (en/fr), phone_verified_at | unique phone, unique email |
| `otp_codes` | phone, code_hash, purpose, attempts, expires_at, consumed_at | (phone, created_at), expires_at |
| `refresh_tokens` | user_id, family_id, token_hash, expires_at, revoked_at | unique token_hash, user_id, family_id, expires_at |
| `idempotency_keys` | user_id, key, request_hash, status, response_status, response_body (jsonb), expires_at | unique (user_id, key), expires_at |
| `institutions` | name, type (BANK or MOBILE_MONEY), country, code; seeded per country | unique (country, code) |
| `linked_accounts` | user_id, institution_id, provider (MANUAL, CSV, later others), display_name, account_mask (last 4), currency, current_balance_minor, balance_as_of, status, version | user_id |
| `transactions` | account_id, posted_at, amount_minor (negative is money out), currency, description, category, fingerprint, source | (account_id, posted_at DESC, id), unique (account_id, fingerprint) |
| `audit_log` | user_id, action, metadata (jsonb), ip | (user_id, created_at) |

- Primary keys are time-ordered UUIDv7.
- Amounts are `bigint` minor units plus an ISO 4217 currency code. Each account has one currency. Totals are shown per currency; nothing is summed across currencies.
- The transaction `fingerprint` hashes account, date, amount and description, so re-importing a statement inserts nothing new.
- Every query filters by the authenticated `user_id`. Requesting another user's account returns `404`.
- Lists use keyset pagination (cursor), not `OFFSET`.

## Cross-cutting concerns

**JWT.** Access token 15 minutes in an `httpOnly`, `SameSite` cookie. Refresh token 30 days, stored hashed, rotated on every use; reuse of an old refresh token revokes the whole family. Signing secrets come from environment variables; the header carries a `kid` so keys can rotate.

**Rate limiting (Bucket4j).** OTP sends 3 per 10 minutes per phone; login 5 per minute per IP and per account; other endpoints about 100 per minute per user. Over the limit returns `429` with `Retry-After`. OTP verification locks out after repeated wrong codes. Buckets are in memory for v1 (single instance) behind an interface; they move to Redis when we run more than one instance.

**Idempotency.** Every creating `POST` requires an `Idempotency-Key` header. Same key and same body replays the stored response; same key and a different body returns `422`; a request still in flight returns `409`. The unique `(user_id, key)` constraint decides races.

**Garbage collection.**
- JVM: Java 21 with G1, container-aware heap sizing (`MaxRAMPercentage`), GC logging, GC metrics via Micrometer. No tuning until a measured problem. CSV imports are streamed under a size cap.
- Data: a scheduled cleanup job deletes expired OTPs, expired or revoked refresh tokens, and idempotency keys older than 48 hours, in small batches; ShedLock ensures one instance runs it; autovacuum handles dead rows.

**Other.** Optimistic locking (`@Version`) on accounts; API versioned at `/api/v1`; errors in RFC 7807 format; validation on every request body; Actuator health checks, structured JSON logs with a request id, graceful shutdown, 12-factor config; timeouts, retries with backoff and circuit breakers (Resilience4j) for outbound provider calls; audit log for sensitive actions; sensitive columns encrypted at rest.

**Deferred until metrics justify them:** Redis, message queues, read replicas, sharding, microservices.

## Auth flow

| Endpoint | Behavior |
|---|---|
| `POST /auth/register` | Phone, password, names, country, optional email and national ID. Creates an unverified user and sends an SMS OTP. Returns the same `202` whether or not the phone exists. |
| `POST /auth/verify-otp` | Checks the 6-digit code, marks the phone verified, issues the cookies. |
| `POST /auth/login` | Phone or email plus password. Unverified users get a verify-your-phone response with resend. Wrong credentials always return one generic error. |
| `POST /auth/refresh` | Rotates the refresh token. |
| `POST /auth/logout` | Revokes the token family and clears the cookies. |

Passwords use Argon2id. OTPs are stored hashed, expire after 5 minutes, allow 5 wrong attempts and work once. SMS goes through an `SmsSender` interface; local dev logs the code. The real gateway must cover both Uganda and DR Congo and is chosen, with its coverage verified, before the auth slice goes live.

## Account linking flow (v1)

1. `GET /institutions?country=UG` lists seeded banks and mobile money wallets.
2. `POST /accounts` links an account (institution, display name, last 4 digits, currency, opening balance); requires `Idempotency-Key`.
3. `POST /accounts/{id}/imports` uploads a CSV statement (max 5 MB, 10,000 rows). The file is streamed; each row is validated, fingerprinted and inserted with `ON CONFLICT DO NOTHING`. The response reports inserted, duplicate and rejected counts with a reason per rejected row. The balance is updated in the same database transaction under the optimistic lock.
4. Linking goes through `BankProvider` (`capabilities()`, `link()`, `sync()`). The manual provider's `sync()` does nothing; a future mobile money or bank API provider implements a real `sync()` without changing other modules.

Dashboard reads: `GET /accounts` (all accounts, totals per currency), `GET /accounts/{id}/transactions?cursor=&limit=`, and `GET /transactions` across accounts, both keyset paginated.

## UI impact (needs approval)

The UI is otherwise unchanged. Phone-first sign-up needs two visible changes to the sign-up form, both reusing the existing input component and styling:

1. Add a phone number field.
2. Relabel "SSN" to "National ID".

New screens (OTP verification, link account, CSV import) reuse existing tokens, fonts and components. French translation is a later slice and changes text only.

## Testing

JUnit 5 with Testcontainers running Postgres 17 (no mocked database). Unit tests for `Money`, fingerprinting, the rate limiter and the OTP rules. Integration tests for every endpoint, including idempotent replay, token reuse revocation, cross-user access returning `404`, and duplicate CSV import. The transactions list query is checked with `EXPLAIN` to confirm it uses its index. The frontend restructure is checked by rename similarity in git and before and after screenshots.

## Delivery order

Each slice is a `feat/` branch and a PR into `dev`.

1. Restructure: move the app into `web/`, add the `api/` skeleton and `docker-compose.yml`. No UI edits.
2. Auth: register, OTP, login, refresh, logout, rate limiting, cleanup job; wire the auth forms.
3. Institutions, linked accounts and the `BankProvider` interface.
4. Transactions and CSV import.
5. Dashboard wiring: replace the mock data in the pages with API data, markup unchanged.
6. French language.

Out of scope for now: fund transfers, live bank or mobile money sync, Redis.
