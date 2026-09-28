import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch, refreshSession } from "./client";

const fetchMock = vi.fn<typeof fetch>();

function json(status: number, body: unknown, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  });
}

async function failure(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise;
  } catch (error) {
    return error as ApiError;
  }
  throw new Error("Expected the request to fail");
}

function lastCall(index = 0) {
  const [url, init] = fetchMock.mock.calls[index];
  const headers = new Headers((init as RequestInit).headers);
  return { url: String(url), init: init as RequestInit, headers };
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
  vi.stubGlobal("crypto", { randomUUID: () => "uuid-1" });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("apiFetch requests", () => {
  it("prefixes /api/v1 and sends cookies and the client header", async () => {
    fetchMock.mockResolvedValueOnce(json(200, { id: "u1" }));

    const result = await apiFetch<{ id: string }>("/auth/me");

    const call = lastCall();
    expect(result).toEqual({ id: "u1" });
    expect(call.url).toBe("/api/v1/auth/me");
    expect(call.init.credentials).toBe("include");
    expect(call.init.method).toBe("GET");
    expect(call.headers.get("X-Horizon-Client")).toBe("web");
    expect(call.headers.has("Idempotency-Key")).toBe(false);
  });

  it("adds an Idempotency-Key on POST and JSON-encodes object bodies", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 202 }));

    const result = await apiFetch<void>("/auth/register", {
      method: "POST",
      body: { phone: "+256771234567" },
    });

    const call = lastCall();
    expect(result).toBeUndefined();
    expect(call.headers.get("Idempotency-Key")).toBe("uuid-1");
    expect(call.headers.get("Content-Type")).toBe("application/json");
    expect(call.init.body).toBe('{"phone":"+256771234567"}');
  });

  it("keeps a caller-supplied Idempotency-Key", async () => {
    fetchMock.mockResolvedValueOnce(json(201, {}));

    await apiFetch("/accounts", {
      method: "POST",
      headers: { "Idempotency-Key": "mine" },
      body: { a: 1 },
    });

    expect(lastCall().headers.get("Idempotency-Key")).toBe("mine");
  });

  it("does not JSON-encode FormData or strings", async () => {
    fetchMock.mockResolvedValueOnce(json(200, {}));
    const form = new FormData();
    form.append("file", "x");

    await apiFetch("/accounts/1/imports", { method: "POST", body: form });

    const call = lastCall();
    expect(call.init.body).toBe(form);
    expect(call.headers.has("Content-Type")).toBe(false);
  });
});

describe("apiFetch errors", () => {
  it("throws ApiError parsed from the RFC 7807 body", async () => {
    fetchMock.mockResolvedValueOnce(
      json(403, { type: "about:blank", title: "Forbidden", status: 403, detail: "Verify your phone", code: "phone_not_verified" })
    );

    const error = await failure(apiFetch("/auth/login", { method: "POST", body: {} }));

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 403, code: "phone_not_verified", detail: "Verify your phone" });
    expect(error.message).toBe("Verify your phone");
  });

  it("keeps validation errors and Retry-After", async () => {
    fetchMock.mockResolvedValueOnce(
      json(400, { status: 400, detail: "Invalid", code: "validation_failed", errors: [{ field: "phone", message: "bad" }] })
    );
    const validation = await failure(apiFetch("/auth/register", { method: "POST", body: {} }));
    expect(validation.errors).toEqual([{ field: "phone", message: "bad" }]);

    fetchMock.mockResolvedValueOnce(json(429, { status: 429, detail: "Slow down", code: "rate_limited" }, { "Retry-After": "42" }));
    const limited = await failure(apiFetch("/auth/resend-otp", { method: "POST", body: {} }));
    expect(limited.retryAfterSeconds).toBe(42);
  });

  it("falls back to unknown_error when the body is not JSON", async () => {
    fetchMock.mockResolvedValueOnce(new Response("<html>bad gateway</html>", { status: 502, statusText: "Bad Gateway" }));

    const error = await failure(apiFetch("/auth/me"));

    expect(error).toMatchObject({ status: 502, code: "unknown_error", detail: "Bad Gateway" });
  });
});

describe("refresh on 401", () => {
  it("refreshes once and retries the original request with the same key", async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, { status: 401, code: "unauthenticated", detail: "expired" }))
      .mockResolvedValueOnce(new Response(null, { status: 200 })) // POST /auth/refresh
      .mockResolvedValueOnce(json(200, { ok: true }));

    const result = await apiFetch<{ ok: boolean }>("/accounts", { method: "POST", body: { a: 1 } });

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(lastCall(1).url).toBe("/api/v1/auth/refresh");
    expect(lastCall(1).init.method).toBe("POST");
    expect(lastCall(2).url).toBe("/api/v1/accounts");
    expect(lastCall(2).headers.get("Idempotency-Key")).toBe(lastCall(0).headers.get("Idempotency-Key"));
  });

  it("throws the original 401 when the refresh fails, without retrying", async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, { status: 401, code: "unauthenticated", detail: "expired" }))
      .mockResolvedValueOnce(json(401, { status: 401, code: "invalid_refresh_token", detail: "no" }));

    const error = await failure(apiFetch("/auth/me"));

    expect(error).toMatchObject({ status: 401, code: "unauthenticated" });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("does not loop when the retried request is 401 again", async () => {
    fetchMock
      .mockResolvedValueOnce(json(401, { status: 401, code: "unauthenticated", detail: "x" }))
      .mockResolvedValueOnce(new Response(null, { status: 200 }))
      .mockResolvedValueOnce(json(401, { status: 401, code: "unauthenticated", detail: "x" }));

    const error = await failure(apiFetch("/auth/me"));

    expect(error).toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it("never refreshes for login, register, verify, resend or refresh itself", async () => {
    for (const path of ["/auth/login", "/auth/register", "/auth/verify-otp", "/auth/resend-otp"]) {
      fetchMock.mockReset();
      fetchMock.mockResolvedValueOnce(json(401, { status: 401, code: "bad_credentials", detail: "no" }));

      const error = await failure(apiFetch(path, { method: "POST", body: {} }));

      expect(error).toMatchObject({ status: 401, code: "bad_credentials" });
      expect(fetchMock).toHaveBeenCalledTimes(1);
    }
  });

  it("shares one refresh request between concurrent 401s", async () => {
    let releaseRefresh: (r: Response) => void = () => {};
    const refreshResponse = new Promise<Response>((resolve) => {
      releaseRefresh = resolve;
    });
    fetchMock.mockImplementation(async (input) => {
      const url = String(input);
      if (url.endsWith("/auth/refresh")) return refreshResponse;
      const refreshed = fetchMock.mock.calls.some(([u]) => String(u).endsWith("/auth/refresh"));
      return refreshed ? json(200, { ok: true }) : json(401, { status: 401, code: "unauthenticated", detail: "x" });
    });

    const a = apiFetch("/accounts");
    const b = apiFetch("/transactions");
    await vi.waitFor(() => expect(fetchMock.mock.calls.filter(([u]) => String(u).endsWith("/auth/refresh")).length).toBe(1));
    releaseRefresh(new Response(null, { status: 200 }));

    await expect(Promise.all([a, b])).resolves.toEqual([{ ok: true }, { ok: true }]);
    expect(fetchMock.mock.calls.filter(([u]) => String(u).endsWith("/auth/refresh"))).toHaveLength(1);
  });
});

describe("refreshSession", () => {
  it("returns false instead of throwing on network failure", async () => {
    fetchMock.mockRejectedValueOnce(new TypeError("Failed to fetch"));

    await expect(refreshSession()).resolves.toBe(false);
  });
});
