import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AiConnectionsPanel } from "./AiSettingsPanels";
import type { ConnectionView } from "../types";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const GEMINI: ConnectionView = {
  provider: "GEMINI",
  configured: true,
  model: "gemini-2.5-flash",
  keyHint: "Zx9q",
  status: "ACTIVE",
  lastTestAt: "2026-10-21T03:00:00Z",
  lastTestResult: "Đạt",
  version: 4,
};

type Call = { url: string; method: string; body?: unknown };

function json(body: unknown, status = 200) {
  return Promise.resolve(
    status === 204
      ? new Response(null, { status })
      : new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }),
  );
}

function backend(options: { statusConflict?: boolean } = {}) {
  const calls: Call[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      calls.push({ url, method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
      if (url === "/api/v1/admin/ai/connections") return json([GEMINI]);
      if (url === "/api/v1/ai/providers") return json([{ provider: "GEMINI", models: ["gemini-2.5-flash"], maxOutputTokens: 8192 }]);
      if (url.endsWith("/status"))
        return options.statusConflict
          ? json({ status: 409, code: "AI_CONNECTION_VERSION_CONFLICT" }, 409)
          : json({ ...GEMINI, status: "DISABLED", version: 5 });
      if (url === "/api/v1/admin/ai/quota-policy" && method === "GET")
        return json({ defaultDayTokens: 20000, globalDayTokens: 500000, version: 3, updatedAt: null, configured: true });
      if (url === "/api/v1/admin/ai/quota-policy")
        return json({ defaultDayTokens: 30000, globalDayTokens: 500000, version: 4, updatedAt: null, configured: true });
      if (url === "/api/v1/admin/ai/teacher-quotas")
        return json([{ accountId: "t1", name: "Nguyễn Văn Bình", email: "binhnv@fpt.edu.vn", dayTokens: 0, updatedAt: "2026-10-20T03:00:00Z" }]);
      if (url.startsWith("/api/v1/admin/ai/usage-summary"))
        return json({
          days: 7,
          todayTokens: 12000,
          globalDayTokens: 500000,
          periodTokens: 84000,
          teachersUsed: 3,
          byDay: [{ day: "2026-10-21", tokens: 12000 }],
          byTeacher: [],
          byModel: [{ provider: "GEMINI", model: "gemini-2.5-flash", requests: 40, tokens: 84000 }],
        });
      return json({ status: 404 }, 404);
    }),
  );
  return calls;
}

describe("AiConnectionsPanel", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("bật/tắt khóa gửi kèm version, hiện mức riêng 0 là chặn", async () => {
    const calls = backend();
    render(<AiConnectionsPanel />);

    expect(await screen.findByText("••••Zx9q")).toBeInTheDocument();
    expect(await screen.findByText("0 · chặn")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("switch", { name: "Bật khóa Gemini" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/status"))).toBe(true));
    const status = calls.find((c) => c.url.endsWith("/status"))!;
    expect(status.url).toBe("/api/v1/admin/ai/connections/GEMINI/status");
    expect(status.body).toEqual({ enabled: false, version: 4 });
  });

  it("409 khi đổi trạng thái thì báo tải lại", async () => {
    backend({ statusConflict: true });
    render(<AiConnectionsPanel />);
    fireEvent.click(await screen.findByRole("switch", { name: "Bật khóa Gemini" }));
    expect(await screen.findByText("Khóa vừa được thay đổi ở nơi khác, hãy tải lại.")).toBeInTheDocument();
  });

  it("lưu hạn mức chung gửi version hiện tại", async () => {
    const calls = backend();
    render(<AiConnectionsPanel />);
    const field = await screen.findByLabelText(/Mỗi giảng viên/);
    await waitFor(() => expect(field).toHaveValue("20000"));
    fireEvent.change(field, { target: { value: "30000" } });
    fireEvent.click(screen.getByRole("button", { name: /Lưu hạn mức/ }));

    expect(await screen.findByText("Đã lưu hạn mức.")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "PUT" && c.url.endsWith("/quota-policy"))!.body).toEqual({
      defaultDayTokens: 30000,
      globalDayTokens: 500000,
      version: 3,
    });
  });

  it("tab Usage chỉ có số liệu API của trường", async () => {
    backend();
    render(<AiConnectionsPanel />);
    fireEvent.click(screen.getByRole("tab", { name: "Usage" }));

    expect(await screen.findByText("Chỉ tính yêu cầu dùng API của trường.")).toBeInTheDocument();
    const models = (await screen.findByText("Theo nhà cung cấp và mô hình")).closest("section")!;
    expect(within(models).getByText("gemini-2.5-flash")).toBeInTheDocument();
    expect(screen.getAllByText("84.000").length).toBeGreaterThan(0);
  });
});
