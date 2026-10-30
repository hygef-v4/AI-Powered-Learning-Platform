import { act, fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BatchActionDialog } from "./BatchActionDialog";
import type { BatchResult, ItemVersion } from "../model/types";

const mocks = vi.hoisted(() => ({ finalize: vi.fn(), publish: vi.fn(), done: vi.fn() }));
vi.mock("../api", () => ({ gradingApi: mocks }));

const items = [{ evaluationId: "e1", version: 3 }, { evaluationId: "e2", version: 8 }];
const success: BatchResult = { items: items.map((item) => ({ ...item, outcome: "SUCCESS", code: null, newVersion: item.version + 1 })) };

function Harness({ action, selected = items }: { action: "finalize" | "publish"; selected?: ItemVersion[] }) {
  const [open, setOpen] = useState(true);
  return <>{open && <BatchActionDialog action={action} items={selected} onClose={() => setOpen(false)} onDone={mocks.done} />}
    <button>Trang chấm bài</button></>;
}

beforeEach(() => { vi.resetAllMocks(); });
afterEach(() => { vi.useRealTimers(); });

describe("grade action success feedback", () => {
  it.each(["finalize", "publish"] as const)("replaces a successful %s dialog with a four-second notice and refreshes once", async (action) => {
    vi.useFakeTimers();
    mocks[action].mockResolvedValue(success);
    render(<Harness action={action} />);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(mocks[action]).not.toHaveBeenCalled();
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Xác nhận" })); });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent(action === "publish" ? "Đã công bố điểm cho 2 bài." : "Đã chốt điểm cho 2 bài.");
    expect(mocks[action]).toHaveBeenCalledExactlyOnceWith(items);
    expect(mocks.done).toHaveBeenCalledOnce();
    expect(screen.getByRole("button", { name: "Trang chấm bài" })).toBeEnabled();
    act(() => { vi.advanceTimersByTime(3999); });
    expect(screen.getByRole("status")).toBeInTheDocument();
    act(() => { vi.advanceTimersByTime(1); });
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(mocks[action]).toHaveBeenCalledOnce();
  });

  it("allows dismissing success immediately", async () => {
    mocks.publish.mockResolvedValue(success);
    render(<Harness action="publish" />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    fireEvent.click(await screen.findByRole("button", { name: "Đóng thông báo" }));
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("keeps the successful notice when refreshing a filtered list removes the selected rows", async () => {
    mocks.finalize.mockResolvedValue(success);
    const { rerender } = render(<BatchActionDialog action="finalize" items={items} onClose={vi.fn()} onDone={mocks.done} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("status")).toHaveTextContent("Đã chốt điểm cho 2 bài.");
    rerender(<BatchActionDialog action="finalize" items={[]} onClose={vi.fn()} onDone={mocks.done} />);
    expect(screen.getByRole("status")).toHaveTextContent("Đã chốt điểm cho 2 bài.");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it.each([
    { items: [success.items[0]!, { ...success.items[1]!, outcome: "FAILED", code: "GRADE_VERSION_CONFLICT" }] },
    { items: [] },
    { items: [success.items[0]!] },
    { items: [{ ...success.items[0]!, evaluationId: "unexpected" }, success.items[1]!] },
  ])("keeps partial, empty or incomplete responses visible: %j", async (result) => {
    mocks.finalize.mockResolvedValue(result);
    render(<Harness action="finalize" />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    await act(async () => {});
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(mocks.done).toHaveBeenCalledOnce();
  });

  it("retains server errors and permits an explicit retry", async () => {
    mocks.publish.mockRejectedValue(new Error("Mất kết nối"));
    render(<Harness action="publish" />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Mất kết nối");
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Xác nhận" })).toBeEnabled();
    expect(mocks.done).not.toHaveBeenCalled();
  });
});
