import { afterEach, describe, expect, it, vi } from "vitest";

/** Hằng số SEO đọc biến môi trường lúc nạp module nên mỗi ca nạp lại module sau khi đặt biến. */
async function loadRoutes(env: Record<string, string | undefined>) {
  vi.resetModules();
  for (const [key, value] of Object.entries(env)) vi.stubEnv(key, value);
  const [{ default: robots }, { default: sitemap }] = await Promise.all([import("@/app/robots"), import("@/app/sitemap")]);
  return { robots, sitemap };
}

describe("robots.txt và sitemap.xml", () => {
  afterEach(() => vi.unstubAllEnvs());

  it("production cho đọc trang, chặn API và trỏ tới sitemap", async () => {
    const { robots } = await loadRoutes({ SEO_INDEXABLE: "true", SITE_URL: "https://alapedu.online" });
    expect(robots()).toEqual({
      rules: { userAgent: "*", allow: "/", disallow: ["/api/", "/post-login"] },
      sitemap: "https://alapedu.online/sitemap.xml",
    });
  });

  it.each([undefined, "false", "1"])("SEO_INDEXABLE=%s thì chặn toàn bộ", async (value) => {
    const { robots } = await loadRoutes({ SEO_INDEXABLE: value, SITE_URL: "https://alapedu.online" });
    expect(robots()).toEqual({ rules: { userAgent: "*", disallow: "/" } });
  });

  it("sitemap chỉ có các trang công khai, URL tuyệt đối", async () => {
    const { sitemap } = await loadRoutes({ SITE_URL: "https://alapedu.online" });
    expect(sitemap().map((entry) => entry.url)).toEqual([
      "https://alapedu.online/",
      "https://alapedu.online/login",
      "https://alapedu.online/privacy",
    ]);
  });
});
