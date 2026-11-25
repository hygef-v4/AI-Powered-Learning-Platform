import { randomBytes } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";
import { LANDING_DEFAULT_LOCALE, LANDING_LOCALE_HEADER } from "@/i18n/locales";

/** SEC-004: generate a fresh nonce before Next renders any HTML page. */
export function proxy(request: NextRequest) {
  const nonce = randomBytes(32).toString("base64");
  const development = process.env.NODE_ENV === "development";
  // Chỉ khi chạy dev: cho công cụ thiết kế impeccable live (localhost:8400) nạp script và chèn <style> xem trước.
  // 'strict-dynamic' làm trình duyệt bỏ qua danh sách host, còn nonce làm 'unsafe-inline' mất tác dụng, nên dev bỏ cả hai.
  const liveDev = development ? " http://localhost:8400" : "";
  const csp = [
    "default-src 'self'",
    development
      ? `script-src 'self' 'nonce-${nonce}' 'unsafe-eval'${liveDev}`
      : `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'`,
    development ? `style-src 'self' 'unsafe-inline'${liveDev}` : `style-src 'self' 'nonce-${nonce}'`,
    // Existing React components use dynamic style attributes; this exception cannot execute scripts.
    "style-src-attr 'unsafe-inline'",
    // docx-preview nạp font nhúng trong tệp Word qua blob: URL.
    "font-src 'self' blob:",
    "img-src 'self' data: blob:",
    `connect-src 'self'${development ? ` ws: wss:${liveDev}` : ""}`,
    "frame-src 'self' blob: https://www.youtube-nocookie.com https://embed.diagrams.net",
    "frame-ancestors 'none'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join("; ");

  const requestHeaders = new Headers(request.headers);
  // Overwrite client-supplied values; Next reads this CSP to nonce its own scripts.
  requestHeaders.set("x-nonce", nonce);
  // Trang chủ chưa có cookie ngôn ngữ thì dựng bằng tiếng Việt (SEO tiếng Việt); trang khác giữ mặc định tiếng Anh.
  if (request.nextUrl.pathname === "/") requestHeaders.set(LANDING_LOCALE_HEADER, LANDING_DEFAULT_LOCALE);
  else requestHeaders.delete(LANDING_LOCALE_HEADER);
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
  matcher: ["/((?!api(?:/|$)|_next/static|_next/image|fonts/|favicon.ico|opengraph-image|robots.txt|sitemap.xml).*)"],
};
