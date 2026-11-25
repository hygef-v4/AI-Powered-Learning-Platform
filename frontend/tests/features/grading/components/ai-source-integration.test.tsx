import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AiProposalPanel } from "@/features/grading/components/AiProposalPanel";
import { Toaster } from "@/shared/ui";
import { BatchActionDialog } from "@/features/grading/components/BatchActionDialog";
import { ApiError } from "@/shared/api/client";

const mocks = vi.hoisted(() => ({ fetch: vi.fn(), requestBatch: vi.fn(), estimateBatch: vi.fn() }));
vi.mock("@/shared/api/client", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/shared/api/client")>(), fetchApi: mocks.fetch,
}));
vi.mock("@/features/grading/api", () => ({ gradingApi: {
  requestProposals: mocks.requestBatch,
  estimateProposalTokens: mocks.estimateBatch,
} }));

const items = [{ evaluationId: "e", version: 7 }];

beforeEach(() => {
  vi.resetAllMocks();
  mocks.estimateBatch.mockResolvedValue({ estimatedTokens: 5123 });
});
afterEach(cleanup);

/** Không còn popup chọn nguồn (yêu cầu 2026-11-05): hệ thống tự chọn API chung rồi khóa cá nhân. */
describe("U09 AI requests choose their source automatically", () => {
  it("requests a proposal right away without a source, and respects a disabled workspace", async () => {
    const request = vi.fn();
    const props = { evaluationId: "e", proposals: [], onRequest: request, onApply: vi.fn() };
    const { rerender } = render(<AiProposalPanel {...props} disabled />);
    expect(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" })).toBeDisabled();
    rerender(<AiProposalPanel {...props} />);
    fireEvent.click(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" }));
    expect(request).toHaveBeenCalledExactlyOnceWith({ requestRef: expect.any(String) });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(mocks.fetch).not.toHaveBeenCalled();
  });

  it.each(["finalize", "publish"] as const)("preserves the %s batch action", async (action) => {
    const api = await import("@/features/grading/api");
    const perform = vi.fn().mockResolvedValue({ items: [{ evaluationId: "e", outcome: "SUCCESS" }] });
    Object.assign(api.gradingApi, { [action === "finalize" ? "finalizeGrades" : "publishGrades"]: perform });
    render(<><Toaster /><BatchActionDialog action={action} items={items} onClose={vi.fn()} onDone={vi.fn()} /></>);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("status")).toHaveTextContent(action === "publish" ? "Đã công bố điểm cho 1 bài." : "Đã chốt điểm cho 1 bài.");
    expect(perform).toHaveBeenCalledExactlyOnceWith(items);
    expect(mocks.requestBatch).not.toHaveBeenCalled();
  });

  it("reuses the batch request after a lost response for the same selection", async () => {
    mocks.requestBatch.mockRejectedValueOnce(new ApiError({ status: 502, detail: "Response lost" }))
      .mockResolvedValue({ items: [{ evaluationId: "e", outcome: "READY" }] });
    const props = { action: "ai" as const, onClose: vi.fn(), onDone: vi.fn() };
    const { rerender } = render(<BatchActionDialog {...props} items={items} />);

    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Response lost");
    const firstRequest = mocks.requestBatch.mock.calls[0]?.[1];
    expect(firstRequest.requestRef).toEqual(expect.any(String));

    rerender(<BatchActionDialog {...props} items={items.map((item) => ({ ...item }))} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    await screen.findByRole("button", { name: "Đóng" });
    expect(mocks.requestBatch).toHaveBeenNthCalledWith(2, items, firstRequest);
  });

  it("discards the batch retry reference when the selected versions change", async () => {
    mocks.requestBatch.mockRejectedValueOnce(new TypeError("Response lost"))
      .mockResolvedValue({ items: [{ evaluationId: "e", outcome: "READY" }] });
    const props = { action: "ai" as const, onClose: vi.fn(), onDone: vi.fn() };
    const { rerender } = render(<BatchActionDialog {...props} items={items} />);

    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Response lost");
    const firstRequest = mocks.requestBatch.mock.calls[0]?.[1];

    rerender(<BatchActionDialog {...props} items={[{ evaluationId: "e", version: 8 }]} />);
    rerender(<BatchActionDialog {...props} items={items} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    await screen.findByRole("button", { name: "Đóng" });
    expect(mocks.requestBatch).toHaveBeenCalledTimes(2);
    expect(mocks.requestBatch).not.toHaveBeenNthCalledWith(2, items, firstRequest);
  });
});
