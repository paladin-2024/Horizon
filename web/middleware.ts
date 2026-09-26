import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE, PATH_HEADER, routeDecision } from "@/lib/session";

// Decides only from the presence of the hz_access cookie. It cannot verify the
// JWT (the signing key is in the API), so this is a redirect convenience: the
// (root) layout and the API itself do the real check.
export function middleware(request: NextRequest) {
  const { pathname, search, searchParams } = request.nextUrl;

  const target = routeDecision({
    pathname,
    hasAccessCookie: request.cookies.has(ACCESS_COOKIE),
    sessionExpired: searchParams.get("session") === "expired",
  });
  if (target) return NextResponse.redirect(new URL(target, request.url));

  // Server components cannot see the URL; pass it along for the (root) layout.
  const headers = new Headers(request.headers);
  headers.set(PATH_HEADER, `${pathname}${search}`);
  return NextResponse.next({ request: { headers } });
}

export const config = {
  // Everything except the API proxy, Next internals, and files with an extension
  // (public/icons, images, favicon).
  matcher: ["/((?!api|_next/static|_next/image|.*\\..*).*)"],
};
