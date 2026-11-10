import { act, fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BatchActionDialog } from "@/features/grading/components/BatchActionDialog";
import type { BatchResult, ItemVersion } from "@/features/grading/model/types";

const mocks = vi.hoisted(() => ({ finalizeGrades: vi.fn(), publishGrades: vi.fn(), loadEvaluation: vi.fn(), done: vi.fn() }));
vi.mock("@/features/grading/api", () => ({ gradingApi: mocks }));

const items = [{ evaluationId: "e1", version: 3 }, { evaluationId: "e2", version: 8 }];
const success: BatchResult = { items: items.map((item) => ({ ...item, outcome: "SUCCESS", code: null, newVersion: item.version + 1 })) };

function Harness({ action, selected = items }: { action: "finalize" | "publish"; selected?: ItemVersion[] }) {
  const [open, setOpen] = useState(true);
  return <>{open && <BatchActionDialog action={action} items={selected} onClose={() => setOpen(false)} onDone={mocks.done} />}
    <button>Trang chấm bài</button></>;
}

beforeEach(() => { vi.resetAllMocks(); mocks.loadEvaluation.mockResolvedValue({ evaluation: { state: "DRAFT", version: 3 } }); });
afterEach(() => { vi.useRealTimers(); });

describe("grade action success feedback", () => {
  it.each([
    ["finalize", "FINALIZED"], ["finalize", "PUBLISHED"], ["publish", "PUBLISHED"],
  ] as const)("skips repeated %s of %s with a readable notice", async (action, state) => {
    mocks[action === "finalize" ? "finalizeGrades" : "publishGrades"].mockResolvedValue({ items: [
      { evaluationId: "e1", outcome: "FAILED", code: "INVALID_GRADE_STATE", newVersion: null },
    ] });
    mocks.loadEvaluation.mockResolvedValue({ evaluation: { state, version: 3 } });
    render(<Harness action={action} selected={[items[0]!]} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("status")).toHaveTextContent(/trước đó/);
    expect(screen.getByRole("status")).toHaveTextContent(/giữ nguyên/);
    expect(screen.queryByText(/INVALID_GRADE_STATE|FAILED/)).not.toBeInTheDocument();
    expect(mocks.loadEvaluation).toHaveBeenCalledExactlyOnceWith("e1");
    if (action === "publish") expect(mocks.finalizeGrades).not.toHaveBeenCalled();
  });

  it("keeps an unconfirmed state failure readable without trying to finalize", async () => {
    mocks.publishGrades.mockResolvedValue({ items: [{ evaluationId: "e1", outcome: "FAILED", code: "INVALID_GRADE_STATE", newVersion: null }] });
    mocks.loadEvaluation.mockRejectedValue(new Error("Mất kết nối"));
    render(<Harness action="publish" selected={[items[0]!]} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByText(/Trạng thái bài đã thay đổi/)).toBeInTheDocument();
    expect(mocks.finalizeGrades).not.toHaveBeenCalled();
    expect(screen.queryByText(/FAILED|INVALID_GRADE_STATE/)).not.toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Đóng" }).every((button) => !button.hasAttribute("disabled"))).toBe(true);
  });

  it("counts changed and already-completed items separately in a mixed batch", async () => {
    mocks.publishGrades.mockResolvedValue({ items: [success.items[0],
      { evaluationId: "e2", outcome: "FAILED", code: "INVALID_GRADE_STATE", newVersion: null }] });
    mocks.loadEvaluation.mockResolvedValue({ evaluation: { state: "PUBLISHED", version: 8 } });
    render(<Harness action="publish" />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("status")).toHaveTextContent("Đã công bố điểm cho 1 bài. 1 bài đã hoàn tất trước đó, điểm được giữ nguyên.");
    expect(mocks.publishGrades).toHaveBeenCalledExactlyOnceWith(items);
    expect(mocks.finalizeGrades).not.toHaveBeenCalled();
  });
  it.each(["finalize", "publish"] as const)("replaces a successful %s dialog with a four-second notice and refreshes once", async (action) => {
    vi.useFakeTimers();
    mocks[action === "finalize" ? "finalizeGrades" : "publishGrades"].mockResolvedValue(success);
    render(<Harness action={action} />);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(mocks[action === "finalize" ? "finalizeGrades" : "publishGrades"]).not.toHaveBeenCalled();
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Xác nhận" })); });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent(action === "publish" ? "Đã công bố điểm cho 2 bài." : "Đã chốt điểm cho 2 bài.");
    expect(mocks[action === "finalize" ? "finalizeGrades" : "publishGrades"]).toHaveBeenCalledExactlyOnceWith(items);
    expect(mocks.done).toHaveBeenCalledOnce();
    expect(screen.getByRole("button", { name: "Trang chấm bài" })).toBeEnabled();
    act(() => { vi.advanceTimersByTime(3999); });
    expect(screen.getByRole("status")).toBeInTheDocument();
    act(() => { vi.advanceTimersByTime(1); });
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(mocks[action === "finalize" ? "finalizeGrades" : "publishGrades"]).toHaveBeenCalledOnce();
  });

  it("allows dismissing success immediately", async () => {
    mocks.publishGrades.mockResolvedValue(success);
    render(<Harness action="publish" />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    fireEvent.click(await screen.findByRole("button", { name: "Đóng thông báo" }));
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("keeps the successful notice when refreshing a filtered list removes the selected rows", async () => {
    mocks.finalizeGrades.mockResolvedValue(success);
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
    mocks.finalizeGrades.mockResolvedValue(result);
    render(<Harness action="finalize" />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    await act(async () => {});
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(mocks.done).toHaveBeenCalledOnce();
  });

  it("retains server errors and permits an explicit retry", async () => {
    mocks.publishGrades.mockRejectedValue(new Error("Mất kết nối"));
    render(<Harness action="publish" />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Mất kết nối");
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Xác nhận" })).toBeEnabled();
    expect(mocks.done).not.toHaveBeenCalled();
  });
});


it("only publishes successfully finalized items and retains review-required errors", async () => {
  mocks.publishGrades.mockResolvedValueOnce({ items: items.map(i => ({ ...i, outcome: "FAILED", code: "INVALID_GRADE_STATE", newVersion: null })) })
    .mockResolvedValueOnce({ items: [success.items[0]] });
  mocks.finalizeGrades.mockResolvedValue({ items: [success.items[0], { ...items[1], outcome: "FAILED", code: "STRUCTURED_FEEDBACK_REVIEW_REQUIRED", newVersion: null }] });
  render(<Harness action="publish" />);
  fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
  await act(async () => {});
  expect(mocks.publishGrades).toHaveBeenLastCalledWith([{ evaluationId: "e1", version: 4 }]);
  expect(mocks.finalizeGrades).toHaveBeenCalledExactlyOnceWith(items);
  expect(screen.getByRole("dialog")).toHaveTextContent(/xác nhận|duyệt/i);
});
