import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Placeholder session check until real auth is wired (see docs/superpowers/plans-draft/
// 03-auth.md and 06-web-auth.md). `hz_access` is the cookie name the real backend will set;
// today it never exists, so every visit to a protected page redirects to sign-in.
const SESSION_COOKIE = "hz_access";
const PUBLIC_PATHS = ["/sign-in", "/sign-up"];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isPublicPath = PUBLIC_PATHS.some(
    (path) => pathname === path || pathname.startsWith(`${path}/`)
  );
  const hasSession = request.cookies.has(SESSION_COOKIE);

  if (!hasSession && !isPublicPath) {
    return NextResponse.redirect(new URL("/sign-in", request.url));
  }

  if (hasSession && isPublicPath) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|icons|.*\\..*).*)"],
};
