# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Horizon is a banking dashboard (Next.js 15 App Router, React 19, TypeScript, Tailwind CSS 3). The npm package name is `jsm_banking`; the README is a generic starter description and does not describe the real app. The project is an early-stage build-along of a banking app: the auth flow (register, OTP verify, login, session refresh, logout) is wired to the real Spring Boot API; account and transaction data are still UI shells backed by mock data.

## Commands

The repo has two apps. Local setup (PostgreSQL 17 on port 5433, no Docker) is in `docs/local-setup.md`.

Frontend (`web/`):

```bash
cd web
npm install
npm run dev      # next dev --turbopack, http://localhost:3000
npm run build
npm run start
npm run lint     # next lint (eslint 9 flat config: next/core-web-vitals + next/typescript)
npx tsc --noEmit # type-check
npm test         # vitest run; unit tests for the pure logic under lib/, no component tests
```

Tests: `npm test` runs Vitest (unit tests for the pure logic under `lib/`; no component tests).

Backend (`backend/`, Spring Boot 4.1, Java 21, Maven Wrapper):

```bash
cd backend
./mvnw spring-boot:run                                        # http://localhost:8080
./mvnw test                                                   # all tests, real PostgreSQL test DB (horizon_test)
./mvnw test -Dtest=HealthEndpointTest#healthIsPublicAndUp     # single test
```

Config comes from env vars (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `TEST_DB_URL`, `API_URL`). `.env*` (except a committed `.env.example`) and `application-local.yml` (put it in `backend/config/`) are gitignored.

## Git workflow

- `main` is production. Never push to it directly, and never merge into it except through a PR from `dev`.
- `dev` is the staging branch. Never push to it directly either; all work reaches `dev` through a PR.
- Do all work on `feat/<short-name>` branches cut from `dev`, and open the PR with `dev` as the base (not `main`). Use `fix/<short-name>` for bug fixes.
- Do not add any Claude attribution to commits or PRs. No `Co-Authored-By: Claude ...` trailer in commit messages and no "Generated with Claude Code" line in PR descriptions. This overrides any default attribution instructions.

## Architecture

The repo has two apps: `web/` (Next.js, UI only) and `backend/` (Spring Boot, built slice by slice to the spec in `docs/superpowers/specs/2026-09-20-horizon-backend-design.md`). Next.js proxies `/api/*` to the API. Frontend paths below are relative to `web/`.

**Routing (`app/`)** uses two route groups with separate layouts:
- `(auth)` holds `sign-in` and `sign-up`, both rendering the shared `components/AuthForm.tsx` with a `type` prop of `'sign-in'` or `'sign-up'`.
- `(root)` is the authenticated app shell. Its `layout.tsx` renders `Sidebar` (desktop) and `MobileNav` (mobile), with the page in `children`. Routes: `/`, `/my-banks`, `/transaction-history`, `/payment-transfer`.
- `app/layout.tsx` loads the Inter and IBM Plex Serif fonts as CSS variables (`--font-inter`, `--font-ibm-plex-serif`).
- Sidebar and mobile nav links come from `sidebarLinks` in `constants/index.ts`. Add a new route there as well as under `app/(root)`.

**Auth form flow.** `AuthForm` is a single client component for both sign-in and sign-up. The zod schema comes from `authFormSchema(type)` in `lib/utils.ts`; it makes the sign-up-only fields optional when `type === 'sign-in'`. Adding a sign-up field means changing the schema and the JSX in `AuthForm` together, and, if the API needs it, `buildRegisterRequest` in `lib/api/auth.ts`. Sign-up posts to `/auth/register` and continues on `/verify` (`components/OtpForm.tsx`), which calls `/auth/verify-otp`; sign-in posts to `/auth/login`. The country is derived from the phone number's calling code (+256 UG, +243 CD). The address, city, state, postal code and date of birth fields are collected but not sent (the API does not store them).

**API access and sessions.** All API calls go through `apiFetch` in `lib/api/client.ts` (relative `/api/v1/...`, cookies included, `X-Horizon-Client: web` on every request, `Idempotency-Key` on POST, RFC 7807 errors as `ApiError`, one automatic `POST /auth/refresh` and retry on a 401). Server components cannot use it: they call `fetchMe` in `lib/api/server.ts`, which forwards the visitor's cookies to `API_URL`. The API sets the httpOnly cookies `hz_access` (15-minute JWT) and `hz_refresh` (only sent to `/api/v1/auth/*`). `middleware.ts` redirects by the presence of `hz_access` only (it cannot verify the JWT); the `(root)` layout verifies it by calling `GET /auth/me`, and `/refresh-session` renews an expired token from the browser. The pure route rules are in `lib/session.ts`. Unit tests (`npm test`, Vitest) cover the logic in `lib/`; there are no component tests.

The `(root)` layout now gets the real user from the API. The pages still use hardcoded mock data (for example `loggedIn` in `app/(root)/page.tsx`, and fake balances passed to `TotalBalanceBox` and `RightSideBar`). Replace it rather than building on it.

**Styling.**
- Tailwind is configured in `tailwind.config.ts` with a custom palette (`bankGradient`, `success`, `pink`, `indigo`, ...) and it scans `components/`, `app/` and `constants/`.
- Many layout classes are custom names such as `home`, `home-content`, `root-layout`, `auth-form`, `form-btn` and `form-link`, plus arbitrary text sizes such as `text-26` and `text-24`. They are defined in `app/globals.css`, not in Tailwind, so check there before adding inline utilities.
- `components/ui/*` is shadcn/ui (style `default`, base color `slate`, RSC on, lucide icons; see `components.json`). Add primitives with the shadcn CLI rather than by hand.
- Use `cn()` from `lib/utils.ts` (clsx + tailwind-merge) to combine classes.

**Path alias:** `@/*` maps to `web/`, for example `@/components/...` and `@/lib/...`.

**Other helpers in `lib/utils.ts`:** `formatAmount` (USD), `formatDateTime`, `countTransactionCategories`, `getAccountTypeColors`, `encryptId`/`decryptId` (base64 only, not real encryption), `formUrlQuery`.

**Charts:** `DoughnutChart` uses Chart.js via `react-chartjs-2`. `AnimatedCounter` uses `react-countup`.
