import { renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useGroupDocStream } from "@/features/groupdocs/hooks/useGroupDocStream";

class FakeSource {
  static CLOSED = 2;
  static created: FakeSource[] = [];
  readyState = 0;
  onmessage: ((m: { data: string }) => void) | null = null;
  onerror: (() => void) | null = null;
  onopen: (() => void) | null = null;
  close = vi.fn(() => { this.readyState = FakeSource.CLOSED; });
  constructor(readonly url: string) { FakeSource.created.push(this); }
}

describe("group document stream", () => {
  beforeEach(() => {
    FakeSource.created = [];
    vi.useFakeTimers();
    vi.stubGlobal("EventSource", FakeSource);
    vi.stubGlobal("fetch", vi.fn(async () => new Response(null, { status: 204 })));
  });
  afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals(); });

  it("refreshes the session and reopens the stream when the server rejects the connection", async () => {
    const onReconnect = vi.fn();
    const { unmount } = renderHook(() => useGroupDocStream("doc-1", vi.fn(), onReconnect));
    const first = FakeSource.created[0]!;

    first.readyState = FakeSource.CLOSED; // 401: trình duyệt không tự nối lại
    first.onerror?.();
    await vi.advanceTimersByTimeAsync(1000);

    expect(fetch).toHaveBeenCalledWith("/api/v1/auth/refresh", expect.objectContaining({ method: "POST" }));
    expect(FakeSource.created).toHaveLength(2);
    FakeSource.created[1]!.onopen?.();
    expect(onReconnect).toHaveBeenCalledOnce();
    unmount();
    expect(FakeSource.created[1]!.close).toHaveBeenCalled();
  });

  it("leaves transient drops to the browser's own reconnect", async () => {
    renderHook(() => useGroupDocStream("doc-1", vi.fn(), vi.fn()));
    FakeSource.created[0]!.onerror?.();
    await vi.advanceTimersByTimeAsync(5000);
    expect(FakeSource.created).toHaveLength(1);
  });
});
