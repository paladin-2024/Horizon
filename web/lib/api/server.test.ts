import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cookieHeader, fetchMe } from "./server";

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
  vi.stubEnv("API_URL", "http://localhost:8106");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("cookieHeader", () => {
  it("joins cookies into a Cookie header", () => {
    expect(cookieHeader([{ name: "hz_access", value: "a" }, { name: "x", value: "b" }])).toBe("hz_access=a; x=b");
  });
});

describe("fetchMe", () => {
  it("calls the API directly with the forwarded cookies", async () => {
    const user = { id: "1", firstName: "Cal" };
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(user), { status: 200 }));

    await expect(fetchMe("hz_access=a")).resolves.toEqual(user);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe("http://localhost:8106/api/v1/auth/me");
    expect(new Headers((init as RequestInit).headers).get("Cookie")).toBe("hz_access=a");
    expect((init as RequestInit).cache).toBe("no-store");
  });

  it("returns null on 401", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));

    await expect(fetchMe("")).resolves.toBeNull();
  });

  it("throws on other failures", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 500 }));

    await expect(fetchMe("hz_access=a")).rejects.toThrow("status 500");
  });
});
