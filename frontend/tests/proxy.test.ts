// @vitest-environment node
import { NextRequest } from "next/server";
import { afterEach, expect, it, vi } from "vitest";
import { proxy } from "@/proxy";

afterEach(() => vi.unstubAllEnvs());

it("uses unpredictable per-request nonces and overwrites client CSP/nonce headers", () => {
  vi.stubEnv("NODE_ENV", "production");
  const request = new NextRequest("http://localhost/materials/example", {
    headers: { "x-nonce": "attacker", "Content-Security-Policy": "script-src *" },
  });
  const first = proxy(request);
  const second = proxy(request);
  const csp = first.headers.get("Content-Security-Policy")!;
  const nonce = csp.match(/'nonce-([^']+)'/)![1]!;
  expect(Buffer.from(nonce, "base64")).toHaveLength(32);
  expect(second.headers.get("Content-Security-Policy")).not.toBe(csp);
  expect(first.headers.get("x-middleware-request-x-nonce")).toBe(nonce);
  expect(first.headers.get("x-middleware-request-content-security-policy")).toBe(csp);
  expect(csp).not.toContain("attacker");
  const script = csp.split(";").find(value => value.trim().startsWith("script-src"))!;
  expect(script).toContain("'strict-dynamic'");
  expect(script).not.toMatch(/unsafe-inline|unsafe-eval/);
  expect(csp).toContain("frame-ancestors 'none'");
  expect(csp).toContain("frame-src 'self' blob: https://www.youtube-nocookie.com https://embed.diagrams.net");
  expect(first.headers.get("X-Frame-Options")).toBe("DENY");
  expect(first.headers.get("X-Content-Type-Options")).toBe("nosniff");
  expect(first.headers.get("Referrer-Policy")).toBe("strict-origin-when-cross-origin");
  expect(first.headers.get("Strict-Transport-Security")).toBe("max-age=31536000; includeSubDomains");
  expect(first.headers.get("Cache-Control")).toBe("private, no-store");
});

it("permits eval and WebSocket debugging only in development", () => {
  vi.stubEnv("NODE_ENV", "development");
  const csp = proxy(new NextRequest("http://localhost/login")).headers.get("Content-Security-Policy")!;
  expect(csp).toContain("'unsafe-eval'");
  expect(csp).toContain("connect-src 'self' ws: wss:");
});
