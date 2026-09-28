import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { buildLoginRequest, buildRegisterRequest, login, logout, me, refresh, register, resendOtp, verifyOtp } from "./auth";

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
  vi.stubGlobal("crypto", { randomUUID: () => "uuid-1" });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

function call(index = 0) {
  const [url, init] = fetchMock.mock.calls[index];
  return { url: String(url), method: (init as RequestInit).method, body: (init as RequestInit).body };
}

describe("auth endpoints", () => {
  it("sends each request to the contract path with the contract body", async () => {
    fetchMock.mockImplementation(async () => new Response(null, { status: 200 }));

    await register({ phone: "+256771234567", password: "pw123456", firstName: "Cal", lastName: "Nza", country: "UG" });
    await verifyOtp({ phone: "+256771234567", code: "123456" });
    await resendOtp({ phone: "+256771234567" });
    await login({ identifier: "a@b.co", password: "pw123456" });
    await logout();

    expect(call(0)).toMatchObject({ url: "/api/v1/auth/register", method: "POST" });
    expect(JSON.parse(String(call(0).body))).toEqual({
      phone: "+256771234567", password: "pw123456", firstName: "Cal", lastName: "Nza", country: "UG",
    });
    expect(call(1)).toMatchObject({ url: "/api/v1/auth/verify-otp", method: "POST", body: '{"phone":"+256771234567","code":"123456"}' });
    expect(call(2)).toMatchObject({ url: "/api/v1/auth/resend-otp", method: "POST", body: '{"phone":"+256771234567"}' });
    expect(call(3)).toMatchObject({ url: "/api/v1/auth/login", method: "POST", body: '{"identifier":"a@b.co","password":"pw123456"}' });
    expect(call(4)).toMatchObject({ url: "/api/v1/auth/logout", method: "POST" });
  });

  it("me returns the parsed user", async () => {
    const user = { id: "1", phone: "+256771234567", email: null, firstName: "Cal", lastName: "Nza", country: "UG", language: "en" };
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(user), { status: 200 }));

    await expect(me()).resolves.toEqual(user);
    expect(call(0)).toMatchObject({ url: "/api/v1/auth/me", method: "GET" });
  });

  it("refresh resolves true when POST /auth/refresh succeeds", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 200 }));

    await expect(refresh()).resolves.toBe(true);
    expect(call(0)).toMatchObject({ url: "/api/v1/auth/refresh", method: "POST" });
  });
});

describe("buildRegisterRequest", () => {
  const values = {
    firstName: " Caleb ",
    lastName: "Nzabanita",
    phone: "+256 771 234 567",
    email: " caleb@example.com ",
    nationalId: " CM9000000000AB ",
    password: "secret123",
  };

  it("normalizes the phone, derives the country and trims text", () => {
    expect(buildRegisterRequest(values)).toEqual({
      phone: "+256771234567",
      password: "secret123",
      firstName: "Caleb",
      lastName: "Nzabanita",
      country: "UG",
      email: "caleb@example.com",
      nationalId: "CM9000000000AB",
    });
    expect(buildRegisterRequest({ ...values, phone: "+243812345678" }).country).toBe("CD");
  });

  it("leaves out empty optional fields", () => {
    const request = buildRegisterRequest({ ...values, email: "", nationalId: undefined });
    expect(request).not.toHaveProperty("email");
    expect(request).not.toHaveProperty("nationalId");
  });

  it("throws on an unsupported phone or missing names", () => {
    expect(() => buildRegisterRequest({ ...values, phone: "+254712345678" })).toThrow();
    expect(() => buildRegisterRequest({ ...values, firstName: undefined })).toThrow();
  });
});

describe("buildLoginRequest", () => {
  it("sends the email as the identifier", () => {
    expect(buildLoginRequest({ email: " a@b.co ", password: "pw" })).toEqual({ identifier: "a@b.co", password: "pw" });
  });
});
