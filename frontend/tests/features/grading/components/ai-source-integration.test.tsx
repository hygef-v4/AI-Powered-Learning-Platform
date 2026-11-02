import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AiProposalPanel } from "@/features/grading/components/AiProposalPanel";
import { BatchActionDialog } from "@/features/grading/components/BatchActionDialog";
import type { SourceOption } from "@/shared/ai";

const mocks = vi.hoisted(() => ({ fetch: vi.fn(), requestBatch: vi.fn(), estimateBatch: vi.fn() }));
vi.mock("@/shared/api/client", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/shared/api/client")>(), apiFetch: mocks.fetch,
}));
vi.mock("@/features/grading/api", () => ({ gradingApi: {
  requestBatch: mocks.requestBatch,
  estimateBatch: mocks.estimateBatch,
} }));

const sources: SourceOption[] = [
  { source: "SHARED", provider: "GEMINI", model: "school-model", keyHint: null,
    available: false, reason: "Đã hết hạn mức hôm nay",
    quota: { used: 100, limit: 100, left: 0, resetAt: "2026-10-23T00:00:00Z" }, costBearer: "SCHOOL" },
  { source: "PERSONAL", provider: "CLAUDE", model: "personal-model", keyHint: "abcd",
    available: true, reason: null, quota: null, costBearer: "SELF" },
];
const items = [{ evaluationId: "e", version: 7 }];

beforeEach(() => {
  vi.resetAllMocks();
  mocks.fetch.mockResolvedValue(sources);
  mocks.estimateBatch.mockResolvedValue({ estimatedTokens: 5123 });
});
afterEach(cleanup);

async function selectPersonal() {
  expect(await screen.findByText("Đã hết hạn mức hôm nay")).toBeInTheDocument();
  expect(screen.getByRole("radio", { name: /API của trường/ })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Gửi yêu cầu" })).toBeDisabled();
  fireEvent.click(screen.getByRole("radio", { name: /API cá nhân · Claude/ }));
  fireEvent.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));
}

describe("U09 consumes the U06 source picker", () => {
  it("keeps the existing shared request DTO without a personal provider and respects a disabled workspace", async () => {
    mocks.fetch.mockResolvedValue([{ ...sources[0], available: true, reason: null,
      quota: { used: 0, limit: 100, left: 100, resetAt: "2026-10-23T00:00:00Z" } }]);
    const request = vi.fn();
    const props = { evaluationId: "e", proposals: [], onRequest: request, onApply: vi.fn() };
    const { rerender } = render(<AiProposalPanel {...props} disabled />);
    expect(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" })).toBeDisabled();
    expect(mocks.fetch).not.toHaveBeenCalled();
    rerender(<AiProposalPanel {...props} />);
    fireEvent.click(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" }));
    fireEvent.click(await screen.findByRole("radio", { name: /API của trường/ }));
    fireEvent.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));
    expect(request).toHaveBeenCalledExactlyOnceWith({ source: "SHARED", requestRef: expect.any(String) });
  });

  it.each(["finalize", "publish"] as const)("preserves the %s batch action without loading AI sources", async (action) => {
    const api = await import("@/features/grading/api");
    const perform = vi.fn().mockResolvedValue({ items: [{ evaluationId: "e", outcome: "SUCCESS" }] });
    Object.assign(api.gradingApi, { [action]: perform });
    render(<BatchActionDialog action={action} items={items} onClose={vi.fn()} onDone={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    expect(await screen.findByRole("status")).toHaveTextContent(action === "publish" ? "Đã công bố điểm cho 1 bài." : "Đã chốt điểm cho 1 bài.");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(perform).toHaveBeenCalledExactlyOnceWith(items);
    expect(mocks.fetch).not.toHaveBeenCalled();
    expect(mocks.requestBatch).not.toHaveBeenCalled();
  });

  it("loads available sources only on demand and sends the selected source without saving grades", async () => {
    const request = vi.fn();
    const apply = vi.fn();
    render(<AiProposalPanel evaluationId="e" proposals={[]} onRequest={request} onApply={apply} />);
    expect(mocks.fetch).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" }));
    await selectPersonal();
    expect(mocks.fetch).toHaveBeenCalledWith("/api/v1/ai/sources");
    expect(request).toHaveBeenCalledExactlyOnceWith({ source: "PERSONAL", provider: "CLAUDE", requestRef: expect.any(String) });
    expect(apply).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("cancel sends nothing and reopening requires a fresh selection", async () => {
    const request = vi.fn();
    render(<AiProposalPanel evaluationId="e" proposals={[]} onRequest={request} onApply={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" }));
    await screen.findByText("Đã hết hạn mức hôm nay");
    fireEvent.click(screen.getByRole("radio", { name: /API cá nhân · Claude/ }));
    fireEvent.click(screen.getByRole("button", { name: "Hủy" }));
    expect(request).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" }));
    await waitFor(() => expect(mocks.fetch).toHaveBeenCalledTimes(2));
    expect(screen.getByRole("button", { name: "Gửi yêu cầu" })).toBeDisabled();
  });

  it("does not expose the previous source while options are refreshing after reopen", async () => {
    let refresh!: (value: SourceOption[]) => void;
    const request = vi.fn();
    render(<AiProposalPanel evaluationId="e" proposals={[]} onRequest={request} onApply={vi.fn()} />);

    fireEvent.click(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" }));
    await selectPersonal();
    mocks.fetch.mockImplementationOnce(() => new Promise(resolve => { refresh = resolve; }));
    fireEvent.click(screen.getByRole("button", { name: "Yêu cầu đề xuất mới" }));

    expect(await screen.findByText("Đang tải...")).toBeInTheDocument();
    expect(screen.queryByRole("radio", { name: /API cá nhân · Claude/ })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Gửi yêu cầu" })).toBeDisabled();

    refresh(sources);
    expect(await screen.findByRole("radio", { name: /API cá nhân · Claude/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Gửi yêu cầu" })).toBeDisabled();
    expect(request).toHaveBeenCalledOnce();
  });

  it("keeps batch versions and the chosen source, blocks duplicate dispatch while pending and shows outcomes", async () => {
    let finish!: (value: { items: { evaluationId: string; outcome: string }[] }) => void;
    mocks.requestBatch.mockReturnValue(new Promise(resolve => { finish = resolve; }));
    const done = vi.fn();
    render(<BatchActionDialog action="ai" items={items} onClose={vi.fn()} onDone={done} />);
    await selectPersonal();
    expect(mocks.requestBatch).toHaveBeenCalledExactlyOnceWith(items, { source: "PERSONAL", provider: "CLAUDE", requestRef: expect.any(String) });
    expect(screen.getByRole("button", { name: "Gửi yêu cầu" })).toBeDisabled();
    finish({ items: [{ evaluationId: "e", outcome: "PENDING" }] });
    expect(await screen.findByText("Bài 1: PENDING")).toBeInTheDocument();
    expect(done).toHaveBeenCalledOnce();
  });

  it("shows a server-derived token estimate before submitting a batch AI request", async () => {
    render(<BatchActionDialog action="ai" items={items} onClose={vi.fn()} onDone={vi.fn()} />);

    await waitFor(() => expect(mocks.estimateBatch).toHaveBeenCalledExactlyOnceWith(items));
    expect(await screen.findByRole("radio", { name: /ước tính 5\.123 token/ })).toBeInTheDocument();
    expect(mocks.requestBatch).not.toHaveBeenCalled();
  });

  it("a failed batch stays failed until explicit retry and never falls back to another source", async () => {
    mocks.requestBatch.mockRejectedValue(new Error("offline"));
    const done = vi.fn();
    render(<BatchActionDialog action="ai" items={items} onClose={vi.fn()} onDone={done} />);
    await selectPersonal();
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(mocks.requestBatch).toHaveBeenCalledOnce();
    expect(done).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
    await waitFor(() => expect(mocks.fetch).toHaveBeenCalledTimes(2));
    expect(screen.getByRole("button", { name: "Gửi yêu cầu" })).toBeDisabled();
    expect(mocks.requestBatch).toHaveBeenCalledOnce();
  });
});
