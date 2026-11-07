import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AiProposalPanel } from "@/features/grading/components/AiProposalPanel";
import { BatchActionDialog } from "@/features/grading/components/BatchActionDialog";

const mocks = vi.hoisted(() => ({ fetch: vi.fn(), requestBatch: vi.fn(), estimateBatch: vi.fn() }));
vi.mock("@/shared/api/client", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: mocks.fetch,
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
    render(<BatchActionDialog action={action} items={items} onClose={vi.fn()} onDone={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("status")).toHaveTextContent(action === "publish" ? "Đã công bố điểm cho 1 bài." : "Đã chốt điểm cho 1 bài.");
    expect(perform).toHaveBeenCalledExactlyOnceWith(items);
    expect(mocks.requestBatch).not.toHaveBeenCalled();
  });

  it("shows the token estimate and sends a batch AI request without a source", async () => {
    mocks.requestBatch.mockResolvedValue({ items: [{ evaluationId: "e", outcome: "QUEUED" }] });
    render(<BatchActionDialog action="ai" items={items} onClose={vi.fn()} onDone={vi.fn()} />);
    expect(await screen.findByText("Ước tính khoảng 5.123 token.")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    await waitFor(() => expect(mocks.requestBatch).toHaveBeenCalledExactlyOnceWith(items, { requestRef: expect.any(String) }));
    expect(await screen.findByText(/QUEUED/)).toBeInTheDocument();
  });
});
