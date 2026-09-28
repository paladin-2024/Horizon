// Pure route and session rules shared by middleware.ts, the (root) layout and
// the /refresh-session page. No Node or Next imports: middleware runs on the edge.

export const ACCESS_COOKIE = "hz_access";
/** Short-lived marker set by /refresh-session so a failed refresh cannot loop. */
export const GATE_COOKIE = "hz_gate";
/** Request header set by middleware.ts with the path and query being requested. */
export const PATH_HEADER = "x-hz-path";

export const SIGN_IN_PATH = "/sign-in";
export const SIGN_UP_PATH = "/sign-up";
export const VERIFY_PATH = "/verify";
export const REFRESH_SESSION_PATH = "/refresh-session";
export const SESSION_EXPIRED_URL = "/sign-in?session=expired";

// Pages that signed-in users are sent away from.
const GUEST_ONLY_PATHS = [SIGN_IN_PATH, SIGN_UP_PATH];
// Pages that need no session.
const PUBLIC_PATHS = [...GUEST_ONLY_PATHS, VERIFY_PATH, REFRESH_SESSION_PATH];

function isUnder(pathname: string, base: string): boolean {
  return pathname === base || pathname.startsWith(`${base}/`);
}

/**
 * Where middleware should redirect a page request, or null to let it through.
 * It only looks at whether the hz_access cookie exists: middleware cannot
 * verify the JWT (the signing key lives in the API), so a present cookie may
 * still hold an expired or invalid token. The (root) layout checks it for real
 * by calling GET /auth/me.
 */
export function routeDecision(input: {
  pathname: string;
  hasAccessCookie: boolean;
  sessionExpired: boolean;
}): string | null {
  const { pathname, hasAccessCookie, sessionExpired } = input;
  if (!hasAccessCookie) {
    return PUBLIC_PATHS.some((p) => isUnder(pathname, p)) ? null : SIGN_IN_PATH;
  }
  if (GUEST_ONLY_PATHS.some((p) => isUnder(pathname, p)) && !sessionExpired) return "/";
  return null;
}

/** Accepts only same-site paths that are not auth pages; anything else becomes "/". */
export function safeNextPath(raw: string | null | undefined): string {
  if (!raw || !/^\/(?!\/)[^\\\s]*$/.test(raw)) return "/";
  const pathname = raw.split(/[?#]/)[0];
  return PUBLIC_PATHS.some((p) => isUnder(pathname, p)) ? "/" : raw;
}

export function refreshSessionUrl(path: string): string {
  return `${REFRESH_SESSION_PATH}?next=${encodeURIComponent(safeNextPath(path))}`;
}

/**
 * Where the (root) layout sends a user whose access token was rejected: first
 * to /refresh-session to try the refresh cookie, and, if that already failed
 * once (the marker cookie is set), to sign-in.
 */
export function unauthenticatedRedirect(path: string, refreshAlreadyTried: boolean): string {
  return refreshAlreadyTried ? SESSION_EXPIRED_URL : refreshSessionUrl(path);
}

/**
 * Placeholder balances shown on the dashboard until accounts (plan 04/07) are wired.
 * app/(root)/page.tsx still reads this; the signed-in identity itself is now real
 * (see app/(root)/layout.tsx, which calls GET /auth/me instead of this).
 */
export const mockCurrentUser = {
  firstName: "Caleb",
  lastName: "Nzabanita",
  email: "cnzabb@gmail.com",
};
