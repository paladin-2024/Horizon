---
name: Horizon
description: A calm, trustworthy personal finance dashboard for Uganda and DR Congo
colors:
  bank-blue: "#0179FE"
  bank-blue-light: "#4893FF"
  indigo-mid: "#6172F3"
  indigo-deep: "#3538CD"
  success-025: "#F6FEF9"
  success-050: "#ECFDF3"
  success-100: "#D1FADF"
  success-600: "#039855"
  success-700: "#027A48"
  success-900: "#054F31"
  pink-025: "#FEF6FB"
  pink-100: "#FCE7F6"
  pink-500: "#EE46BC"
  pink-600: "#DD2590"
  pink-700: "#C11574"
  pink-900: "#851651"
  blue-025: "#F5FAFF"
  blue-100: "#D1E9FF"
  blue-500: "#2E90FA"
  blue-600: "#1570EF"
  blue-700: "#175CD3"
  blue-900: "#194185"
  sky-mist: "#F3F9FF"
  ink-navy: "#00214F"
  ink-slate: "#344054"
  neutral-025: "#FCFCFD"
  neutral-200: "#EAECF0"
  neutral-300: "#D0D5DD"
  neutral-500: "#667085"
  neutral-600: "#475467"
  neutral-700: "#344054"
  neutral-900: "#101828"
typography:
  display:
    fontFamily: "Switzer, system-ui, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: "32px"
  headline:
    fontFamily: "Switzer, system-ui, sans-serif"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: "28px"
  title:
    fontFamily: "Switzer, system-ui, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: "24px"
  body:
    fontFamily: "Switzer, system-ui, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
  label:
    fontFamily: "Switzer, system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: "16px"
rounded:
  sm: "8px"
  md: "10px"
  lg: "12px"
  xl: "18px"
  full: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "24px"
  xl: "32px"
  "2xl": "48px"
components:
  button-primary:
    backgroundColor: "{colors.bank-blue}"
    textColor: "#FFFFFF"
    rounded: "{rounded.sm}"
    padding: "10px 16px"
  button-primary-hover:
    backgroundColor: "{colors.bank-blue-light}"
  button-secondary:
    backgroundColor: "#FFFFFF"
    textColor: "{colors.neutral-700}"
    rounded: "{rounded.sm}"
    padding: "10px 16px"
  card-default:
    backgroundColor: "#FFFFFF"
    textColor: "{colors.neutral-900}"
    rounded: "{rounded.lg}"
  input-default:
    backgroundColor: "#FFFFFF"
    textColor: "{colors.neutral-900}"
    rounded: "{rounded.sm}"
    padding: "10px 14px"
---

# Design System: Horizon

## Overview

**Creative North Star: "The Calm Instrument Panel"**

Horizon reads as a well-kept bank statement, not a flashy fintech pitch deck: white cards, one confident blue, generous whitespace, numbers that are never crowded. That's the resting state of every summary screen — home, balances, profile. But the same system has to work as a precise instrument when a user leans in to check something specific: transaction lists, category breakdowns, budgets. There density is allowed to rise — tighter row rhythm, more numbers on screen — without ever reaching for a different visual language. One palette, one type family, one radius system; only density and information scale with the task.

The system is deliberately quiet about itself: no gradients-as-decoration, no icon-tile soup, no drop shadows pretending to be depth. The one place it allows itself a signature move is the brand-blue gradient (`bank-gradient`), reserved for the bank card, the active sidebar state, and primary CTAs — everywhere else stays flat white and neutral gray.

**Key Characteristics:**
- Calm, high-trust, numbers-first — never loud or "startup-y"
- One primary accent (bank blue) used sparingly and consistently
- Flat surfaces with soft ambient shadows, never hard drop shadows or heavy borders
- Density flexes by task: airy on summary screens, tighter on data-dense ones
- Semantic color (success green, alert red/pink) is reserved for meaning, never decoration

## Colors

A near-monochrome neutral base (white, gray-25 through gray-900) carries almost everything; blue is the one color that gets to mean "Horizon," and green/pink/red are reserved for semantic states, never used decoratively.

### Primary
- **Bank Blue** (#0179FE): The brand accent. Primary buttons, active nav state, links, focus rings, the bank-card gradient (`linear-gradient(90deg, #0179FE 0%, #4893FF 100%)`). Used on a small fraction of any screen — its rarity is what makes it register as "the brand."

### Secondary
- **Indigo** (#6172F3 mid / #3538CD deep): Reserved for chart series and data visualization accents (doughnut chart segments) where a second hue is needed alongside blue.

### Tertiary
- **Pink** (#EE46BC family, 025→900): Category/semantic accent for one transaction category lane and chip variants — never used for primary actions.

### Neutral
- **Ink Navy** (#00214F): Sidebar logo wordmark only.
- **Ink Slate** (#344054): Secondary heading text (sidebar labels, user name).
- **Gray scale** (#FCFCFD → #101828, steps 25/200/300/500/600/700/900): All body text, borders, and backgrounds. gray-900 is primary text, gray-600/500 is secondary/muted text, gray-200/300 are hairline borders, gray-25 is the dashboard's page background (distinct from white cards sitting on it).
- **Sky Mist** (#F3F9FF): The auth-page background wash, nowhere else.

### Semantic
- **Success Green** (#039855 / #027A48, with #ECFDF3 / #D1FADF light backgrounds): Positive amounts, "Success" transaction status, completed states.
- **Alert Red** (destructive token, hsl(4.2 74.3% 48.8%) ≈ #D92D20): Errors, destructive actions, failed transactions, form validation messages.

### Named Rules
**The One Blue Rule.** Bank Blue is the only color allowed to carry a primary call-to-action or an active/selected state. A second "primary-looking" button on the same screen is a bug, not a design choice.
**The Semantic-Only Rule.** Success green, alert red, and the category palette (pink/blue/success triads from `topCategoryStyles`) only appear when they encode a real state or category — never as generic decoration or to "add color" to an empty-feeling layout.

## Typography

**Body & Display Font:** Switzer (Swiss Typefaces, via Fontshare, self-hosted — see `web/assets/fonts/switzer/`), with system-ui/sans-serif fallback — the only typeface in the system; weights 400/500/600/700/800 in active use.

**Character:** A rounded-geometric grotesque — single-story `a`, soft bowls, confident tabular-leaning numerals — carries both the big balance numbers and the smallest form hint. Horizon doesn't pair a display face with a body face; hierarchy is built through size, weight, and color, not a font change. (Replaced Plus Jakarta Sans on 2026-10-05 for a friendlier, more rounded numeral set that reads better at dashboard scale.)

### Hierarchy
- **Display** (600, 24px/32px, scaling to 24px→30px on `lg:`): Page-level totals and headline numbers — `header-box-title`, `total-balance-amount`, profile name.
- **Headline** (600, 20px/28px, scaling to 24px on `md:`): Section titles within a page — "Recent transactions," a budgets section header.
- **Title** (600, 16px/24px): Card-level headings — `header-2`, empty-state titles, modal titles.
- **Body** (400–500, 14px/20px): The default text size for almost everything — labels, descriptions, table cells, button text at the smaller size.
- **Label** (600, 12px/16px, sometimes 10px/14px for the smallest chips): Form error messages, secondary metadata (email under a name), tiny badge text.

### Named Rules
**The One-Family Rule.** Every new screen uses Switzer exclusively; do not introduce a serif or a second sans for "editorial" moments — hierarchy comes from the existing weight/size scale, not a new face.

## Layout

The app shell is a fixed-width left sidebar (icon-only below `lg`, full width with labels at `2xl:w-[280px]`) plus a scrollable main content column, with an optional 300px-wide right sidebar on large screens (`lg:flex`) for secondary/contextual content (profile card, banks summary, recent activity). Below `md`, the sidebar collapses entirely into a 64px top bar (`root-layout`, `h-16`) plus a slide-out `MobileNav` sheet.

Content padding is generous and responsive: `px-5 sm:px-8` horizontal, `py-7 lg:py-12` vertical on the main column. Section rhythm runs on an 8-point-ish scale: `gap-8` between major page sections, `gap-6` between a card's internal blocks, `gap-1`–`gap-2` between a label and its value. Dashboard pages (`my-banks`, `transactions`, `payment-transfer`) share a `bg-gray-25` page background with white cards floating on top — the page background is never pure white, only cards are.

Scrolling containers hide their scrollbar by default (`no-scrollbar`) except where a visible thin custom scrollbar signals a long horizontal/vertical list (`custom-scrollbar`, 3px, rounded thumb).

## Elevation & Depth

Flat by default, with soft ambient shadow doing all elevation work — no borders-as-elevation, no hard drop shadows. A card either sits flush on the gray-25 page background with a shadow, or is separated by a 1px `gray-200` hairline (sidebar, header dividers); the two are not mixed on the same edge.

### Shadow Vocabulary
- **form** (`0px 1px 2px rgba(16,24,40,0.05)`): The lightest touch — buttons and inputs, just enough to lift off the page.
- **chart** (`0px 1px 3px rgba(16,24,40,0.10), 0px 1px 2px rgba(16,24,40,0.06)`): The standard card elevation — total balance card, empty states, most dashboard cards.
- **profile** (`0px 12px 16px -4px rgba(16,24,40,0.08), 0px 4px 6px -2px rgba(16,24,40,0.03)`): A deeper, softer shadow for an element that visually floats over another (the profile avatar overlapping the banner).
- **creditCard** (`8px 10px 16px rgba(0,0,0,0.05)`): The bank card and the mobile top bar — slightly offset rather than purely ambient, giving those two elements a touch more physical presence.

### Named Rules
**The Flat-By-Default Rule.** Nothing gets a shadow just to look "important." Shadow is reserved for the four named roles above; a new component either reuses one of them or stays flat with a hairline border.

## Shapes

Corners are soft but restrained: the base radius is 12px (`--radius: 0.75rem`), stepping down to 10px (`md`) and 8px (`sm`) for tighter controls like inputs and small buttons. The bank card is the one deliberate exception, at 18–20px, giving it a slightly more "physical card" feel than the rest of the flat UI. Avatars, category dots, chips, stat-row icon badges, and the active sidebar/nav item are fully rounded (`rounded-full`) — a small, deliberately-borrowed pill language for anything that marks "selected" or "a unit of one thing," layered on top of the 8–18px scale everything else uses. Borders, where used, are always 1px and light (`gray-200`/`gray-300`) — never a visible "designed" border weight.

## Components

### Buttons
- **Shape:** 8–10px radius (shadcn `rounded-md`), matching the sm/md shape tokens.
- **Primary (brand CTA):** The `bank-gradient` background (`#0179FE → #4893FF`), white text, `shadow-form`, semibold 16px label — used for the single most important action on a form (`form-btn`, `plaidlink-primary`, `payment-transfer_btn`).
- **Default (shadcn):** Solid `primary` (same blue, flat, no gradient) with white text for ordinary in-flow actions; `outline`/`secondary`/`ghost`/`link` variants exist and follow shadcn's standard state behavior (hover darkens/lightens by ~10–20% opacity).
- **View-all / tertiary:** White background, `gray-300` border, `gray-700` text, semibold 14px — used for a de-emphasized action next to a primary one (`view-all-btn`).

### Cards / Containers
- **Corner Style:** 12px (`rounded-lg`) to 18px for feature cards (EmptyState, bank card).
- **Background:** White on a `gray-25` page ground.
- **Shadow Strategy:** `shadow-chart` is the default for a standalone card; see Elevation.
- **Border:** None by default on floating cards; a `gray-200` hairline only where a card sits flush with other elements (sidebar, header).
- **Internal Padding:** `p-4 sm:p-6` for compact cards (total balance), `px-6 py-12` for empty states, `p-6/p-8` for page-level containers.

### Inputs / Fields
- **Style:** White fill, `gray-300` 1px border, 8px radius, 15px text (`text-16` ≈ 15px/22px), placeholder in `gray-500`.
- **Focus:** Ring in Bank Blue (`--ring`), standard shadcn focus-visible treatment — no custom glow.
- **Password fields:** Carry a view/hide toggle button (Hugeicons `ViewIcon`/`ViewOffIcon`) inside the field, right-aligned.
- **Error:** 12px red (`text-red-500`) message directly under the field (`form-message`), no red border treatment currently applied to the input itself.

### Navigation
- **Sidebar:** White background, `gray-200` right hairline, nav items in `ink-slate` text turning white-on-bank-gradient when active — the only place in the system a nav/list item gets a filled gradient background rather than a flat color.
- **Mobile:** Collapses to a 64px top bar with logo + hamburger, opening a bottom-anchored `Sheet` (`mobilenav-sheet`) with the same link list and a footer user card, identical states to desktop.
- **Typography:** 15px semibold labels (`sidebar-label`), 22px icons.

### Chips / Category Badges
- **Style:** Fully rounded (`rounded-2xl`/`rounded-full`), each semantic category gets a 3-part palette (background tint, circle/icon background, text color) drawn from the `success`/`pink`/`blue` families — never an arbitrary one-off color. A transaction-status chip additionally carries a colored left border/dot plus a tinted background (`Success` = green family, `Processing` = neutral gray, etc.).

### Bank Card (signature component)
The one place the system allows a fully-saturated gradient surface: an 18–20px-radius card in the `bank-gradient` blue, white text, masked account number, and a decorative diagonal-lines texture image bleeding off the top-left corner. This card's visual weight is intentional — it's the one element on the page allowed to look like a "real" physical bank card, everything else around it stays flat and quiet.

## Do's and Don'ts

### Do:
- **Do** keep every new card flat on `gray-25` with a `shadow-chart`-equivalent elevation, not a border.
- **Do** reuse the existing `text-10` through `text-36` scale (and its named hierarchy above) instead of raw Tailwind text sizes — they're already tuned to be denser than Tailwind's defaults.
- **Do** give a data-dense stat row a small circular icon badge (tinted background, 600-weight icon) ahead of its label, and a dark (`neutral-900`) rounded tooltip bubble on chart hover — the one piece of "dashboard template" polish Horizon borrows deliberately, always rendered in Bank Blue / semantic tints, never the reference's own colors.
- **Do** use Bank Blue for exactly one primary action per screen; everything else is neutral, outline, or ghost.
- **Do** draw any new category/status color from the existing `success`/`pink`/`blue` semantic families rather than introducing a new hue.
- **Do** keep multi-currency amounts visually distinct per currency (separate totals/cards), never summed or blended into one number.

### Don't:
- **Don't** introduce a second typeface. Hierarchy comes from size/weight/color within Plus Jakarta Sans.
- **Don't** add drop shadows, glassmorphism (outside the one existing `.glassmorphism` utility, currently unused on any shipped page), or heavy borders as a substitute for the flat-plus-soft-shadow elevation model.
- **Don't** use the `bank-gradient` surface anywhere but the bank card, the active sidebar item, and primary CTA buttons — it's a signature, not a background pattern.
- **Don't** invent a new radius value outside the 8/10/12/18px scale (plus full-round for chips/avatars).
- **Don't** imply live, automatic account sync in copy or iconography (e.g. a spinning "syncing" state) — linking is manual today; say "add" or "update," not "connect" or "sync."
