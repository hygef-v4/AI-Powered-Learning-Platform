import { afterEach, expect, it, vi } from "vitest";
import { getUsageSummary } from "@/features/ai/admin/api";

afterEach(() => vi.unstubAllGlobals());
it("forwards the caller's abort signal and still accepts the existing days-only call", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json({todayTokens: 1}));
  vi.stubGlobal("fetch", fetcher);
  const controller = new AbortController();
  await getUsageSummary(7, controller.signal);
  expect(fetcher).toHaveBeenCalledWith(expect.stringContaining("days=7"), expect.objectContaining({signal: controller.signal}));
  fetcher.mockResolvedValue(Response.json({todayTokens: 2}));
  await getUsageSummary(30);
  expect(fetcher).toHaveBeenLastCalledWith(expect.stringContaining("days=30"), expect.objectContaining({signal: undefined}));
});
