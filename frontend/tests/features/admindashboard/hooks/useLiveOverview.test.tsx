import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { REFRESH_MS, SOURCE_TIMEOUT_MS, useLiveOverview } from "@/features/admindashboard/hooks/useLiveOverview";
import { dashboardFixture, routes, usageFixture } from "../model/testFixtures";

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });
const deferred = () => {
  let resolve!: (value: Response) => void;
  const promise = new Promise<Response>((done) => { resolve = done; });
  return { promise, resolve };
};

it("retains optional results arriving before primary counts", async () => {
  const primary = deferred(), ok = routes();
  vi.stubGlobal("fetch", vi.fn((url: string) => url.endsWith("/dashboard") ? primary.promise : ok(url)));
  const { result } = renderHook(useLiveOverview);
  await waitFor(() => expect(ok).toHaveBeenCalledTimes(3));
  await act(async () => { primary.resolve(Response.json(dashboardFixture())); });
  await waitFor(() => expect(result.current.data?.usage?.todayTokens).toBe(182000));
  expect(result.current.data?.loading).toEqual({activity: false, usage: false, backups: false});
});

it("ignores an older optional response after refresh and clears requests on unmount", async () => {
  vi.useFakeTimers({shouldAdvanceTime: true});
  const old = deferred(), ok = routes();
  let calls = 0;
  vi.stubGlobal("fetch", vi.fn((url: string) => url.includes("usage-summary") && calls++ === 0 ? old.promise : ok(url)));
  const { result, unmount } = renderHook(useLiveOverview);
  await waitFor(() => expect(result.current.data).not.toBeNull());
  await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
  await waitFor(() => expect(result.current.data?.usage?.todayTokens).toBe(182000));
  await act(async () => { old.resolve(Response.json({...usageFixture(), todayTokens: 1})); });
  expect(result.current.data?.usage?.todayTokens).toBe(182000);
  unmount();
  expect(vi.getTimerCount()).toBe(0);
});

it.each([false, true])("bounds primary timeouts with previous data = %s", async (hasPrevious) => {
  vi.useFakeTimers({shouldAdvanceTime: true});
  const ok = routes();
  let hang = !hasPrevious;
  vi.stubGlobal("fetch", vi.fn((url: string) => hang && url.endsWith("/dashboard") ? new Promise<Response>(() => {}) : ok(url)));
  const { result } = renderHook(useLiveOverview);
  if (hasPrevious) {
    await waitFor(() => expect(result.current.data).not.toBeNull());
    hang = true;
    await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
  }
  await act(async () => { await vi.advanceTimersByTimeAsync(SOURCE_TIMEOUT_MS); });
  expect(result.current.live).toBe(false);
  expect(result.current.data !== null).toBe(hasPrevious);
  expect(result.current.error !== null).toBe(!hasPrevious);
});

it("denial clears displayed data, stops polling and rejects late optional results", async () => {
  vi.useFakeTimers({shouldAdvanceTime: true});
  const late = deferred(), ok = routes();
  let denied = false;
  const fetcher = vi.fn((url: string) => denied
    ? url.includes("usage-summary") ? late.promise : Promise.resolve(Response.json({}, {status: 403}))
    : ok(url));
  vi.stubGlobal("fetch", fetcher);
  const { result } = renderHook(useLiveOverview);
  await waitFor(() => expect(result.current.data).not.toBeNull());
  denied = true;
  await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
  expect(result.current.data).toBeNull();
  const calls = fetcher.mock.calls.length;
  await act(async () => { late.resolve(Response.json(usageFixture())); await vi.advanceTimersByTimeAsync(REFRESH_MS); });
  expect(result.current.data).toBeNull();
  expect(fetcher).toHaveBeenCalledTimes(calls);
});

it("settles cancelled pending cards when a replacement primary refresh fails", async () => {
  vi.useFakeTimers({shouldAdvanceTime: true});
  const ok = routes();
  let failing = false;
  vi.stubGlobal("fetch", vi.fn((url: string) => url.includes("usage-summary")
    ? new Promise<Response>(() => {})
    : failing && url.endsWith("/dashboard") ? Promise.resolve(Response.json({}, {status: 503})) : ok(url)));
  const { result } = renderHook(useLiveOverview);
  await waitFor(() => expect(result.current.data?.loading.usage).toBe(true));
  failing = true;
  await act(async () => { fireEventVisibility(); });
  await waitFor(() => expect(result.current.live).toBe(false));
  await act(async () => { await vi.advanceTimersByTimeAsync(SOURCE_TIMEOUT_MS + 1); });
  expect(result.current.data?.dashboard.accounts.data?.activeTotal).toBe(3);
  expect(result.current.data?.loading).toEqual({activity: false, usage: false, backups: false});
});

function fireEventVisibility() {
  Object.defineProperty(document, "visibilityState", {configurable: true, value: "visible"});
  document.dispatchEvent(new Event("visibilitychange"));
}
