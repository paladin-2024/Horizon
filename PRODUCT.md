# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Individual consumers in Uganda and DR Congo who hold money across more than one bank and/or mobile money wallet (e.g. a bank current account plus MTN Mobile Money or M-Pesa) and want one place to see where their money is and what it's doing. Primary job: understand total balance and recent activity across every account without opening each bank's or wallet's own app.

## Product Purpose

Horizon is a personal finance dashboard that lets a user link their bank and mobile money accounts (today: manual entry, one account at a time) and see unified balances, transaction history, and spending patterns across all of them, in their own currency (UGX, CDF, or USD, summed only within a currency, never mixed). Success is a user trusting Horizon's numbers enough to check it regularly instead of the individual bank/wallet apps.

## Positioning

A neighboring product would have to pick one lane; Horizon spans three, deliberately:
- **Unified aggregator** — every bank account and mobile money wallet a user holds, in one dashboard, regardless of institution or currency.
- **Growing toward open banking** — manual linking is the honest state of this build today (`ManualProvider` is the only live provider; the backend's `BankProvider` interface exists specifically so a live-sync provider can be added later without changing the rest of the system). The product should not overclaim live sync it doesn't yet do.
- **Insight-first, not just a ledger** — budgets, spend categorization and trends are core to the experience, not an afterthought bolted onto a balance list.

## Operating Context

- Users link an institution from a seeded list (44 total: 26 Uganda banks, 2 Uganda mobile money wallets, 12 DR Congo banks, 4 DR Congo mobile money wallets) and enter an opening balance, display name, and a 4-digit account mask manually.
- Multi-currency is real, not cosmetic: a user may hold UGX, CDF and USD accounts simultaneously. Totals are always shown per currency; currencies are never summed together.
- Auth is live: register, OTP verification, login, session refresh and logout all call the real Spring Boot API (15-minute access token, refresh token, httpOnly cookies).
- Accounts (linking, balances, per-currency totals) are now live against the real API as of the `feat/accounts` backend slice. Transactions and most dashboard data are still UI shells backed by mock data pending the transactions backend slice.

## Capabilities and Constraints

- No live bank/mobile-money API integration yet — only manual account linking. Do not design flows (e.g. "connect your bank" OAuth-style screens) that imply live sync; the honest verb is "link" or "add," not "connect."
- Supported currencies are fixed: UGX, CDF, USD. No other currency should appear in any new UI.
- Every account belongs to exactly one user; there is no shared/joint account concept.
- `constants/index.ts` still contains leftover Plaid/Appwrite tutorial artifacts (`TEST_USER_ID`, `ITEMS`, sandbox access tokens) from the project's original starter-course origin. These are stale and not product evidence — they describe a different, unbuilt integration (Plaid) and should not inform design or be treated as real demo data.
- Package name `jsm_banking` and the README are leftover starter-template naming; the product's real name is Horizon.
- Undecided: whether/when a live-sync provider (open banking or screen-scraping) gets built. Design should not block on it or assume it's imminent.

## Evidence on Hand

No real user testimonials, press, case studies, or production usage data exist — this is an early-stage build. No real account numbers, balances or transaction data exist beyond what tests and local seed data create; nothing should be presented as real customer evidence.

## Product Principles

1. **Never mix currencies.** Every balance, total, and chart either stays within one currency or explicitly breaks results out per currency — summing UGX and USD into one number is a correctness bug, not a display choice.
2. **Be honest about "manual."** The product doesn't pretend accounts sync live. Copy, empty states, and flows should say "add" or "update" rather than implying automatic sync that doesn't exist.
3. **One unified view is the whole point.** Every new surface should answer "all my money, in one place" better than the last — fragmenting that view across disconnected pages works against the core positioning.
4. **Insight, not just ledger.** Where it's credible given real data (not fabricated), prefer showing a pattern or trend over a bare list.
5. **Scale to real users' accounts.** A user in this market plausibly holds 3-6 accounts across banks and mobile money wallets in 1-3 currencies — design list/grid density and totals for that range, not for one or two token accounts.

## Accessibility & Inclusion

No formal accessibility standard has been set by the user. Given the audience (mobile-heavy markets, variable network conditions), default to strong color contrast, legible type at small sizes, and resilience to slow/unreliable connections rather than assuming desktop-class bandwidth.
