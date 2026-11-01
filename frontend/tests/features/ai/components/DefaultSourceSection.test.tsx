import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { DefaultSourceSection } from "@/features/ai/components/DefaultSourceSection";
import type { SourceOption } from "@/shared/ai";

const OPTIONS: SourceOption[] = [
  { source: "SHARED", provider: "GEMINI", model: "gemini-3.6-flash", keyHint: null, available: true, reason: null, quota: null, costBearer: "SCHOOL" },
  { source: "PERSONAL", provider: "OPENAI", model: null, keyHint: null, available: false, reason: "Chưa lưu khóa API cho nhà cung cấp này", quota: null, costBearer: "SELF", preferred: true },
];

function backend() {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      const body = init?.body ? JSON.parse(String(init.body)) : undefined;
      calls.push({ url, method, body });
      const payload = url.endsWith("/ai/sources") ? OPTIONS : body;
      return Promise.resolve(new Response(JSON.stringify(payload), { headers: { "Content-Type": "application/json" } }));
    }),
  );
  return calls;
}

describe("DefaultSourceSection", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("hiện nguồn mặc định hiện tại kèm lý do chưa dùng được, đổi sang API trường rồi lưu", async () => {
    const calls = backend();
    render(<DefaultSourceSection />);

    const select = await screen.findByLabelText("Nguồn AI mặc định");
    await waitFor(() => expect(select).toHaveValue("PERSONAL:OPENAI"));
    expect(screen.getByText("Chưa lưu khóa API cho nhà cung cấp này")).toBeInTheDocument();

    fireEvent.change(select, { target: { value: "SHARED:GEMINI" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByText("Đã đặt API của trường làm nguồn mặc định.")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "PUT")).toMatchObject({
      url: expect.stringContaining("/api/v1/me/ai-source-preference"),
      body: { source: "SHARED", provider: "GEMINI" },
    });
  });

  it("chọn Hỏi mỗi lần thì gửi source rỗng", async () => {
    const calls = backend();
    render(<DefaultSourceSection />);
    const select = await screen.findByLabelText("Nguồn AI mặc định");
    await waitFor(() => expect(select).toHaveValue("PERSONAL:OPENAI"));

    fireEvent.change(select, { target: { value: "ASK" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByText("Đã chuyển về hỏi nguồn mỗi lần.")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "PUT")?.body).toEqual({ source: null, provider: null });
  });
});
