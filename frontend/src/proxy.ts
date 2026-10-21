import { randomBytes } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";

/** SEC-004: generate a fresh nonce before Next renders any HTML page. */
export function proxy(request: NextRequest) {
  const nonce = randomBytes(32).toString("base64");
  const development = process.env.NODE_ENV === "development";
  const csp = [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${development ? " 'unsafe-eval'" : ""}`,
    `style-src 'self' 'nonce-${nonce}'`,
    // Existing React components use dynamic style attributes; this exception cannot execute scripts.
    "style-src-attr 'unsafe-inline'",
    "font-src 'self'",
    "img-src 'self' data: blob:",
    `connect-src 'self'${development ? " ws: wss:" : ""}`,
    "frame-src 'self' blob: https://www.youtube-nocookie.com https://embed.diagrams.net",
    "frame-ancestors 'none'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join("; ");

  const requestHeaders = new Headers(request.headers);
  // Overwrite client-supplied values; Next reads this CSP to nonce its own scripts.
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", csp);
  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", csp);
  response.headers.set("X-Content-Type-Options", "nosniff");
  response.headers.set("X-Frame-Options", "DENY");
  response.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  response.headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  response.headers.set("Cache-Control", "private, no-store");
  return response;
}

export const config = {
  matcher: ["/((?!api(?:/|$)|_next/static|_next/image|fonts/|favicon.ico|robots.txt|sitemap.xml).*)"],
};
