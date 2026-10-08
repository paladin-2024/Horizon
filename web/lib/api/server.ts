// Server-side helpers (server components only). Server components cannot use
// the /api rewrite or the browser cookie jar, so they call the API directly
// and forward the visitor's cookies.

const apiBaseUrl = () => process.env.API_URL ?? "http://localhost:8080";

export function cookieHeader(cookies: { name: string; value: string }[]): string {
  return cookies.map((c) => `${c.name}=${c.value}`).join("; ");
}

/**
 * GET /auth/me with the visitor's cookies. Returns the user, or null when the
 * API answers 401 (missing, expired or invalid access token). Any other
 * failure throws so the page shows the error boundary instead of a login loop.
 */
export async function fetchMe(cookieHeaderValue: string): Promise<ApiUser | null> {
  const res = await fetch(`${apiBaseUrl()}/api/v1/auth/me`, {
    headers: { Accept: "application/json", Cookie: cookieHeaderValue },
    cache: "no-store",
  });
  if (res.status === 401) return null;
  if (!res.ok) throw new Error(`GET /auth/me failed with status ${res.status}`);
  return (await res.json()) as ApiUser;
}

/**
 * GET /accounts with the visitor's cookies. The (root) layout has already
 * confirmed the session via fetchMe before any page using this runs, so a 401
 * here is unexpected and throws like any other failure.
 */
export async function fetchAccounts(cookieHeaderValue: string): Promise<AccountListResponse> {
  const res = await fetch(`${apiBaseUrl()}/api/v1/accounts`, {
    headers: { Accept: "application/json", Cookie: cookieHeaderValue },
    cache: "no-store",
  });
  if (!res.ok) throw new Error(`GET /accounts failed with status ${res.status}`);
  return (await res.json()) as AccountListResponse;
}

/** GET /institutions with the visitor's cookies. */
export async function fetchInstitutions(
  cookieHeaderValue: string,
  country?: InstitutionCountry
): Promise<InstitutionListResponse> {
  const query = country ? `?country=${country}` : "";
  const res = await fetch(`${apiBaseUrl()}/api/v1/institutions${query}`, {
    headers: { Accept: "application/json", Cookie: cookieHeaderValue },
    cache: "no-store",
  });
  if (!res.ok) throw new Error(`GET /institutions failed with status ${res.status}`);
  return (await res.json()) as InstitutionListResponse;
}
