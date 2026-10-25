/** Same-origin mutation transports share the cookie/header contract issued by U01. */
export const CSRF_HEADER = "X-XSRF-TOKEN";

export function csrfToken(): string | undefined {
  if (typeof document === "undefined") return undefined;
  const cookie = document.cookie.split(";").map((part) => part.trim()).find((part) => part.startsWith("XSRF-TOKEN="));
  if (!cookie) return undefined;
  try {
    return decodeURIComponent(cookie.slice("XSRF-TOKEN=".length));
  } catch {
    // An invalid cookie must not become a fabricated token or prevent a safe error response.
    return undefined;
  }
}
