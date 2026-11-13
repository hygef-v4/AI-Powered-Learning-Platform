import { act, renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";

const kinds = (send: ReturnType<typeof vi.fn>) =>
  send.mock.calls.flatMap((call) => (call[0] as ActivityEventInput[]).map((event) => event.kind));

describe("useActivityTracker: tiêu điểm và gửi nốt", () => {
  it("chỉ tính mất tiêu điểm khi bật trackFocus", async () => {
    const send = vi.fn(async () => undefined);
    const off = renderHook(() => useActivityTracker({ enabled: true, send }));
    await act(async () => { window.dispatchEvent(new Event("blur")); await off.result.current(); });
    expect(kinds(send)).toEqual(["SESSION_START"]);
    off.unmount();

    const focused = vi.fn(async () => undefined);
    const on = renderHook(() => useActivityTracker({ enabled: true, send: focused, trackFocus: true }));
    await act(async () => {
      window.dispatchEvent(new Event("blur"));
      window.dispatchEvent(new Event("focus"));
      await on.result.current();
    });
    expect(kinds(focused)).toEqual(["SESSION_START", "HIDDEN", "VISIBLE"]);
    on.unmount();
  });

  it("gửi nốt hàng đợi trước khi nộp; lỗi gửi thì không treo", async () => {
    const send = vi.fn(async () => { await Promise.resolve(); });
    const { result, unmount } = renderHook(() => useActivityTracker({ enabled: true, send, trackFocus: true }));
    await act(async () => {
      window.dispatchEvent(new Event("blur"));
      await result.current();
    });
    expect(kinds(send)).toEqual(["SESSION_START", "HIDDEN"]);
    unmount();

    const failing = vi.fn(async () => { throw new Error("down"); });
    const broken = renderHook(() => useActivityTracker({ enabled: true, send: failing }));
    await act(async () => { await broken.result.current(); });
    expect(failing).toHaveBeenCalled();
    broken.unmount();
  });
});
