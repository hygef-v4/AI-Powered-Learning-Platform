// CSP cho trang Next.js (SEC-004): script chỉ chạy khi mang nonce của request, nên script inline của Next.js
// (hydrate React) chạy được mà không cần 'unsafe-inline'. Nginx không gắn CSP cho trang, chỉ cho /api.

export const FRAME_SOURCES = ["https://www.youtube-nocookie.com", "https://embed.diagrams.net"];

export function buildCsp(nonce: string, isDev: boolean): string {
  const directives: Record<string, string[]> = {
    "default-src": ["'self'"],
    // 'unsafe-eval' chỉ cho `next dev` (React Refresh); bản build không có.
    "script-src": ["'self'", `'nonce-${nonce}'`, "'strict-dynamic'", ...(isDev ? ["'unsafe-eval'"] : [])],
    "style-src": ["'self'", `'nonce-${nonce}'`],
    "img-src": ["'self'", "blob:", "data:"],
    "font-src": ["'self'"],
    "connect-src": ["'self'"],
    "frame-src": FRAME_SOURCES,
    "object-src": ["'none'"],
    "base-uri": ["'self'"],
    "form-action": ["'self'"],
    "frame-ancestors": ["'none'"],
  };
  return Object.entries(directives)
    .map(([name, values]) => `${name} ${values.join(" ")}`)
    .join("; ");
}

export function createNonce(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return btoa(String.fromCharCode(...bytes));
}
