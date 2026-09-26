import { describe, expect, it } from "vitest";
import {
  REFRESH_SESSION_PATH,
  SESSION_EXPIRED_URL,
  refreshSessionUrl,
  routeDecision,
  safeNextPath,
  unauthenticatedRedirect,
} from "./session";

const guest = { hasAccessCookie: false, sessionExpired: false };
const member = { hasAccessCookie: true, sessionExpired: false };

describe("routeDecision without an access cookie", () => {
  it("sends protected pages to sign-in", () => {
    for (const pathname of ["/", "/my-banks", "/transaction-history", "/payment-transfer", "/my-banks/123"]) {
      expect(routeDecision({ pathname, ...guest })).toBe("/sign-in");
    }
  });

  it("lets the auth pages through", () => {
    for (const pathname of ["/sign-in", "/sign-up", "/verify", REFRESH_SESSION_PATH]) {
      expect(routeDecision({ pathname, ...guest })).toBeNull();
    }
  });
});

describe("routeDecision with an access cookie", () => {
  it("sends sign-in and sign-up to the dashboard", () => {
    expect(routeDecision({ pathname: "/sign-in", ...member })).toBe("/");
    expect(routeDecision({ pathname: "/sign-up", ...member })).toBe("/");
  });

  it("lets protected pages, /verify and /refresh-session through", () => {
    for (const pathname of ["/", "/my-banks", "/verify", REFRESH_SESSION_PATH]) {
      expect(routeDecision({ pathname, ...member })).toBeNull();
    }
  });

  it("stops bouncing away from sign-in when the session is known to be expired", () => {
    expect(routeDecision({ pathname: "/sign-in", hasAccessCookie: true, sessionExpired: true })).toBeNull();
  });
});

describe("safeNextPath", () => {
  it("keeps same-site paths with their query", () => {
    expect(safeNextPath("/my-banks")).toBe("/my-banks");
    expect(safeNextPath("/transaction-history?page=2")).toBe("/transaction-history?page=2");
  });

  it("falls back to / for anything else", () => {
    for (const raw of [null, undefined, "", "my-banks", "//evil.example", "https://evil.example", "/\\evil", "/sign-in", "/refresh-session?next=/", "/a b"]) {
      expect(safeNextPath(raw)).toBe("/");
    }
  });
});

describe("refresh flow redirects", () => {
  it("builds the refresh URL with an encoded, validated next path", () => {
    expect(refreshSessionUrl("/transaction-history?page=2")).toBe(
      "/refresh-session?next=%2Ftransaction-history%3Fpage%3D2"
    );
    expect(refreshSessionUrl("//evil.example")).toBe("/refresh-session?next=%2F");
  });

  it("goes to sign-in instead of looping when a refresh was already tried", () => {
    expect(unauthenticatedRedirect("/my-banks", false)).toBe("/refresh-session?next=%2Fmy-banks");
    expect(unauthenticatedRedirect("/my-banks", true)).toBe(SESSION_EXPIRED_URL);
  });
});
