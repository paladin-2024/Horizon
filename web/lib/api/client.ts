export const API_PREFIX = "/api/v1";
export const CLIENT_HEADER = "X-Horizon-Client";

export type ApiFieldError = { field: string; message: string };

/** Error thrown for every non-2xx response, parsed from the RFC 7807 body. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly detail: string;
  readonly errors: ApiFieldError[];
  /** Seconds from the Retry-After header (429 responses), otherwise null. */
  readonly retryAfterSeconds: number | null;

  constructor(
    status: number,
    code: string,
    detail: string,
    extras: { errors?: ApiFieldError[]; retryAfterSeconds?: number | null } = {}
  ) {
    super(detail);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.detail = detail;
    this.errors = extras.errors ?? [];
    this.retryAfterSeconds = extras.retryAfterSeconds ?? null;
  }
}

/** Same as RequestInit, but `body` may also be a plain object or array (sent as JSON). */
export type ApiInit = Omit<RequestInit, "body"> & {
  body?: BodyInit | null | object;
};

// A 401 on these paths means "wrong credentials or code", not "session expired",
// so they never trigger a token refresh.
const NO_REFRESH_PATHS = new Set([
  "/auth/login",
  "/auth/register",
  "/auth/verify-otp",
  "/auth/resend-otp",
  "/auth/refresh",
]);

function isJsonBody(body: unknown): body is object {
  if (body === null || typeof body !== "object") return false;
  return Array.isArray(body) || Object.getPrototypeOf(body) === Object.prototype;
}

function buildRequest(path: string, init: ApiInit): { url: string; init: RequestInit } {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  headers.set(CLIENT_HEADER, "web");
  if (!headers.has("Accept")) headers.set("Accept", "application/json");
  if (method === "POST" && !headers.has("Idempotency-Key")) {
    headers.set("Idempotency-Key", crypto.randomUUID());
  }

  let body: BodyInit | null | undefined;
  if (isJsonBody(init.body)) {
    if (!headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    body = JSON.stringify(init.body);
  } else {
    body = init.body as BodyInit | null | undefined;
  }

  return {
    url: `${API_PREFIX}${path}`,
    init: { ...init, method, headers, body, credentials: "include" },
  };
}

async function toApiError(res: Response): Promise<ApiError> {
  const text = await res.text().catch(() => "");
  let body: Record<string, unknown> = {};
  try {
    const parsed: unknown = text ? JSON.parse(text) : {};
    if (parsed && typeof parsed === "object") body = parsed as Record<string, unknown>;
  } catch {
    // Not JSON (for example an HTML error page from a proxy): keep the defaults.
  }

  const detail =
    (typeof body.detail === "string" && body.detail) ||
    (typeof body.title === "string" && body.title) ||
    res.statusText ||
    `HTTP ${res.status}`;
  const code = typeof body.code === "string" ? body.code : "unknown_error";
  const errors = Array.isArray(body.errors) ? (body.errors as ApiFieldError[]) : [];
  const retryAfter = Number(res.headers.get("Retry-After"));

  return new ApiError(res.status, code, detail, {
    errors,
    retryAfterSeconds: Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : null,
  });
}

let refreshInFlight: Promise<boolean> | null = null;

/**
 * Rotates the session cookies with POST /auth/refresh. Returns true on success,
 * false on any failure, and never throws. Concurrent calls share one request:
 * the server revokes a refresh token family when an old token is reused, so two
 * parallel refreshes would log the user out.
 */
export function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const { url, init } = buildRequest("/auth/refresh", { method: "POST" });
        const res = await fetch(url, init);
        return res.ok;
      } catch {
        return false;
      }
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export async function apiFetch<T>(path: string, init: ApiInit = {}): Promise<T> {
  // Built once so a retry after a refresh reuses the same Idempotency-Key.
  const request = buildRequest(path, init);

  let res = await fetch(request.url, request.init);
  if (res.status === 401 && !NO_REFRESH_PATHS.has(path) && (await refreshSession())) {
    res = await fetch(request.url, request.init);
  }

  if (!res.ok) throw await toApiError(res);

  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
