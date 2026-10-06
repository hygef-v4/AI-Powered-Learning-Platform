import { act, render, renderHook, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { StatusBadge } from "./StatusBadge";
import { usePollStatus } from "./usePollStatus";

type Scan = { scanStatus: string };

function respondWith(...statuses: string[]) {
  const fn = vi.fn();
  statuses.forEach((s) => fn.mockResolvedValueOnce(Response.json({ scanStatus: s })));
  vi.stubGlobal("fetch", fn);
  return fn;
}

const isFinal = (d: Scan) => ["INDEXED", "FAILED"].includes(d.scanStatus);

beforeEach(() => vi.useFakeTimers());
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("usePollStatus", () => {
  it("poll mỗi 3 giây và dừng ở trạng thái cuối", async () => {
    const fetchMock = respondWith("PENDING", "SCANNING", "INDEXED");
    const { result } = renderHook(() => usePollStatus<Scan>("/lessons/1/scan", { isFinal }));

    await act(async () => {});
    expect(result.current.data?.scanStatus).toBe("PENDING");
    await act(async () => vi.advanceTimersByTimeAsync(3000));
    expect(result.current.data?.scanStatus).toBe("SCANNING");
    await act(async () => vi.advanceTimersByTimeAsync(3000));
    expect(result.current.data?.scanStatus).toBe("INDEXED");
    expect(result.current.polling).toBe(false);

    await act(async () => vi.advanceTimersByTimeAsync(10_000));
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it("dừng poll khi rời trang", async () => {
    const fetchMock = respondWith("PENDING", "PENDING", "PENDING");
    const { unmount } = renderHook(() => usePollStatus<Scan>("/lessons/1/scan", { isFinal }));
    await act(async () => {});
    unmount();
    await act(async () => vi.advanceTimersByTimeAsync(10_000));
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("không poll khi chưa bật", async () => {
    const fetchMock = respondWith("PENDING");
    const { result } = renderHook(() => usePollStatus<Scan>("/x", { isFinal, enabled: false }));
    await act(async () => {});
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.polling).toBe(false);
  });
});

describe("StatusBadge", () => {
  it("hiện nhãn tiếng Việt của unit, rơi về mã trạng thái khi thiếu nhãn", () => {
    render(
      <>
        <StatusBadge status="SCANNING" labels={{ SCANNING: { text: "Đang quét", tone: "progress" } }} />
        <StatusBadge status="UNKNOWN" labels={{}} />
      </>,
    );
    expect(screen.getByText("Đang quét")).toBeInTheDocument();
    expect(screen.getByText("UNKNOWN")).toBeInTheDocument();
  });
});
