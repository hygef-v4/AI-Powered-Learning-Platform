import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AiSourcePicker } from "@/shared/ai/AiSourcePicker";
import type { SourceOption } from "@/shared/ai/types";

const OPTIONS: SourceOption[] = [
  {
    source: "SHARED",
    provider: "GEMINI",
    model: "gemini-2.5-flash",
    keyHint: null,
    available: false,
    reason: "Đã hết hạn mức hôm nay",
    quota: { used: 10000, limit: 10000, left: 0, resetAt: "2026-10-21T17:00:00Z" },
    costBearer: "SCHOOL",
  },
  {
    source: "PERSONAL",
    provider: "CLAUDE",
    model: "claude-sonnet-5-5",
    keyHint: "3kQf",
    available: true,
    reason: null,
    quota: null,
    costBearer: "SELF",
  },
];

describe("AiSourcePicker", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("không chọn sẵn, nguồn hết hạn mức hiện mờ kèm lý do, gửi đúng nguồn đã chọn", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(new Response(JSON.stringify(OPTIONS), { headers: { "Content-Type": "application/json" } }))),
    );
    const onSubmit = vi.fn();
    render(<AiSourcePicker open task="Sinh câu hỏi · Bài 3" onCancel={() => undefined} onSubmit={onSubmit} />);

    expect(await screen.findByText("Đã hết hạn mức hôm nay")).toBeInTheDocument();
    const submit = screen.getByRole("button", { name: "Gửi yêu cầu" });
    expect(submit).toBeDisabled();
    expect(screen.getByRole("radio", { name: /API của trường/ })).toBeDisabled();

    fireEvent.click(screen.getByRole("radio", { name: /API cá nhân · Claude/ }));
    fireEvent.click(submit);
    expect(onSubmit).toHaveBeenCalledWith({ source: "PERSONAL", provider: "CLAUDE" });
  });

  it("nguồn mặc định dùng được: tự gửi ngay, không bắt chọn lại", async () => {
    const options = OPTIONS.map((o) => (o.source === "PERSONAL" ? { ...o, preferred: true } : o));
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(new Response(JSON.stringify(options), { headers: { "Content-Type": "application/json" } }))),
    );
    const onSubmit = vi.fn();
    render(<AiSourcePicker open task="Sinh câu hỏi · Bài 3" onCancel={() => undefined} onSubmit={onSubmit} />);

    expect(await screen.findByText(/Đang gửi bằng nguồn mặc định: API cá nhân · Claude/)).toBeInTheDocument();
    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({ source: "PERSONAL", provider: "CLAUDE" });
  });

  it("nguồn mặc định không dùng được: báo lý do, không tự đổi nguồn", async () => {
    const options = OPTIONS.map((o) => (o.source === "SHARED" ? { ...o, preferred: true } : o));
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(new Response(JSON.stringify(options), { headers: { "Content-Type": "application/json" } }))),
    );
    const onSubmit = vi.fn();
    render(<AiSourcePicker open task="Sinh câu hỏi · Bài 3" onCancel={() => undefined} onSubmit={onSubmit} />);

    expect(await screen.findByText(/Nguồn mặc định API của trường chưa dùng được: Đã hết hạn mức hôm nay/)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Gửi yêu cầu" })).toBeDisabled();
  });
});
