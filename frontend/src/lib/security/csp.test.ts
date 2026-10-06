import { describe, expect, it } from "vitest";
import { buildCsp, createNonce } from "./csp";

describe("buildCsp", () => {
  it("chỉ cho script có nonce, không có unsafe-inline, giữ frame YouTube và Draw.io (SEC-004)", () => {
    const csp = buildCsp("abc", false);
    expect(csp).toContain("script-src 'self' 'nonce-abc' 'strict-dynamic'");
    expect(csp).not.toContain("unsafe-inline");
    expect(csp).not.toContain("unsafe-eval");
    expect(csp).toContain("frame-src https://www.youtube-nocookie.com https://embed.diagrams.net");
    expect(csp).toContain("frame-ancestors 'none'");
  });

  it("chỉ thêm unsafe-eval khi chạy next dev", () => {
    expect(buildCsp("abc", true)).toContain("'unsafe-eval'");
  });

  it("nonce khác nhau mỗi lần", () => {
    expect(createNonce()).not.toBe(createNonce());
  });
});
