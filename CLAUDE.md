# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Horizon is a banking dashboard (Next.js 15 App Router, React 19, TypeScript, Tailwind CSS 3). The npm package name is `jsm_banking`; the README is a generic starter description and does not describe the real app. The project is an early-stage build-along of a banking app: UI shells exist, but the backend is not wired up yet.

## Commands

```bash
npm install
npm run dev      # next dev --turbopack
npm run build
npm run start
npm run lint     # next lint (eslint 9 flat config: next/core-web-vitals + next/typescript)
```

There is no test runner or test script configured. Type-check with `npx tsc --noEmit`.

`.env*` is gitignored. No environment variables are read by the code yet.

## Git workflow

- `main` is production. Never push to it directly, and never merge into it except through a PR from `dev`.
- `dev` is the staging branch. Never push to it directly either; all work reaches `dev` through a PR.
- Do all work on `feat/<short-name>` branches cut from `dev`, and open the PR with `dev` as the base (not `main`). Use `fix/<short-name>` for bug fixes.
- Do not add any Claude attribution to commits or PRs. No `Co-Authored-By: Claude ...` trailer in commit messages and no "Generated with Claude Code" line in PR descriptions. This overrides any default attribution instructions.

## Architecture

**Routing (`app/`)** uses two route groups with separate layouts:
- `(auth)` holds `sign-in` and `sign-up`, both rendering the shared `components/AuthForm.tsx` with a `type` prop of `'sign-in'` or `'sign-up'`.
- `(root)` is the authenticated app shell. Its `layout.tsx` renders `Sidebar` (desktop) and `MobileNav` (mobile), with the page in `children`. Routes: `/`, `/my-banks`, `/transaction-history`, `/payment-transfer`.
- `app/layout.tsx` loads the Inter and IBM Plex Serif fonts as CSS variables (`--font-inter`, `--font-ibm-plex-serif`).
- Sidebar and mobile nav links come from `sidebarLinks` in `constants/index.ts`. Add a new route there as well as under `app/(root)`.

**Auth form flow.** `AuthForm` is a single client component for both sign-in and sign-up. The zod schema comes from `authFormSchema(type)` in `lib/utils.ts`. It makes the sign-up-only fields optional when `type === 'sign-in'`. Adding a sign-up field means changing the schema, the `SignUpParams` type, and the JSX in `AuthForm` together. `AuthForm` calls `signIn` and `signUp` from `lib/actions/user.action.ts`.

**Backend is not implemented.**
- `lib/actions/user.action.ts` has stub `signIn`/`signUp` that do nothing. Its directive reads `'user server'`, which is a typo for `'use server'`, so it is not currently a server-action module. Fix it when implementing.
- `types/index.d.ts` declares global ambient types (`User`, `Account`, `Transaction`, `SignUpParams`, ...) with no imports needed. Their fields (`$id`, `appwriteItemId`, `dwollaCustomerId`, Plaid-style account fields) and the comments in `AuthForm` show the intended stack: Appwrite for auth and database, Plaid for bank linking, Dwolla for transfers. None of these are installed.
- Pages currently use hardcoded mock data (for example `loggedIn` in `app/(root)/layout.tsx` and `app/(root)/page.tsx`, and fake balances passed to `TotalBalanceBox` and `RightSideBar`). Replace it rather than building on it.

**Styling.**
- Tailwind is configured in `tailwind.config.ts` with a custom palette (`bankGradient`, `success`, `pink`, `indigo`, ...) and it scans `components/`, `app/` and `constants/`.
- Many layout classes are custom names such as `home`, `home-content`, `root-layout`, `auth-form`, `form-btn` and `form-link`, plus arbitrary text sizes such as `text-26` and `text-24`. They are defined in `app/globals.css`, not in Tailwind, so check there before adding inline utilities.
- `components/ui/*` is shadcn/ui (style `default`, base color `slate`, RSC on, lucide icons; see `components.json`). Add primitives with the shadcn CLI rather than by hand.
- Use `cn()` from `lib/utils.ts` (clsx + tailwind-merge) to combine classes.

**Path alias:** `@/*` maps to the repo root, for example `@/components/...` and `@/lib/...`.

**Other helpers in `lib/utils.ts`:** `formatAmount` (USD), `formatDateTime`, `countTransactionCategories`, `getAccountTypeColors`, `encryptId`/`decryptId` (base64 only, not real encryption), `formUrlQuery`.

**Charts:** `DoughnutChart` uses Chart.js via `react-chartjs-2`. `AnimatedCounter` uses `react-countup`.
