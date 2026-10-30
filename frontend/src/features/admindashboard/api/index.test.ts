import {afterEach, expect, it, vi} from "vitest";
import {getAdminDashboard, getBackupHistory} from "./index";
import {dashboardFixture, historyFixture} from "../model/testFixtures";
afterEach(() => vi.unstubAllGlobals());
it("reads both endpoints without caching and carries cancellation", async () => {
  const request = vi.fn((url: string) => Promise.resolve(Response.json(url.includes("/backups") ? historyFixture(1) : dashboardFixture())));
  vi.stubGlobal("fetch", request);
  const controller = new AbortController();
  expect((await getAdminDashboard(controller.signal)).accounts.data?.total).toBe(5);
  expect((await getBackupHistory(1, 20, controller.signal)).page).toBe(1);
  expect(request).toHaveBeenCalledWith("/api/v1/admin/dashboard", expect.objectContaining({cache: "no-store", signal: controller.signal}));
  expect(request).toHaveBeenCalledWith("/api/v1/admin/dashboard/backups?page=1&size=20", expect.objectContaining({signal: controller.signal}));
});
it("does not turn failed history into a successful empty list", async () => {
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(Response.json({}, {status: 503}))));
  await expect(getBackupHistory(0, 20)).rejects.toMatchObject({problem: {status: 503}});
});
