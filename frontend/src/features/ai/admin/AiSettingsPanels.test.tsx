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
  id: "k1",
  provider: "GEMINI",
  configured: true,
  model: "gemini-2.5-flash",
  keyHint: "Zx9q",
  status: "ACTIVE",
  lastTestAt: "2026-10-21T03:00:00Z",
  lastTestResult: "Đạt",
  version: 4,
};
const GEMINI_2: ConnectionView = { ...GEMINI, id: "k2", keyHint: "Ab12", lastTestAt: null, lastTestResult: null, version: 1 };

type Call = { url: string; method: string; body?: unknown };

function json(body: unknown, status = 200) {
  return Promise.resolve(
    status === 204
      ? new Response(null, { status })
      : new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }),
  );
}

function backend(options: { statusConflict?: boolean; keys?: ConnectionView[]; quotas?: unknown[] } = {}) {
  const calls: Call[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      calls.push({ url, method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
      if (url === "/api/v1/admin/ai/connections" && method === "GET") return json(options.keys ?? [GEMINI, GEMINI_2]);
      if (url === "/api/v1/ai/providers") return json([{ provider: "GEMINI", models: ["gemini-2.5-flash"], maxOutputTokens: 8192 }]);
      if (url.endsWith("/status"))
        return options.statusConflict
          ? json({ status: 409, code: "AI_CONNECTION_VERSION_CONFLICT" }, 409)
          : json({ ...GEMINI, status: "DISABLED", version: 5 });
      if (url === "/api/v1/admin/ai/connections/k1/key") return json({ apiKey: "AIzaSy-saved-Zx9q" });
      if (url === "/api/v1/admin/ai/connections:test") return json({ ok: false, message: "Key API không hợp lệ (HTTP 401)" });
      if (url.endsWith(":test")) return json({ ...GEMINI_2, lastTestResult: "Đạt" });
      if (url === "/api/v1/admin/ai/quota-policy" && method === "GET")
        return json({ defaultDayTokens: 20000, globalDayTokens: 500000, version: 3, updatedAt: null, configured: true });
      if (url === "/api/v1/admin/ai/quota-policy")
        return json({ defaultDayTokens: 30000, globalDayTokens: 500000, version: 4, updatedAt: null, configured: true });
      if (url === "/api/v1/admin/ai/teacher-quotas")
        return json(
          options.quotas ?? [{ accountId: "t1", name: "Nguyễn Văn Bình", email: "binhnv@fpt.edu.vn", dayTokens: 0, updatedAt: "2026-10-20T03:00:00Z" }],
        );
      if (url.startsWith("/api/v1/admin/ai/teacher-candidates"))
        return json([{ accountId: "t2", name: "Trần Thị Hoa", email: "hoatt@fpt.edu.vn" }]);
      if (url.startsWith("/api/v1/admin/ai/usage-summary"))
        return json({
          days: 7,
          todayTokens: 12000,
          globalDayTokens: 500000,
          periodTokens: 84000,
          teachersUsed: 3,
          byDay: [{ day: "2026-10-21", tokens: 12000 }],
          byTeacher: [
            { accountId: "t1", name: "Nguyễn Văn Bình", email: "binhnv@fpt.edu.vn", periodTokens: 84000, todayTokens: 12000, dayLimit: null, requests: 17 },
          ],
          byModel: [{ provider: "GEMINI", model: "gemini-2.5-flash", requests: 40, tokens: 84000, todayTokens: 12000 }],
        });
      return json({ status: 404 }, 404);
    }),
  );
  return calls;
}

async function openSettings() {
  fireEvent.click(await screen.findByRole("tab", { name: "Cài đặt" }));
}

describe("AiConnectionsPanel", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("mở tab Usage trước, tab Usage nằm bên trái tab Cài đặt", async () => {
    backend();
    render(<AiConnectionsPanel />);
    const tabs = screen.getAllByRole("tab").map((t) => t.textContent);
    expect(tabs).toEqual(["Usage", "Cài đặt", "Agent MCP"]);
    expect(screen.getByRole("tab", { name: "Usage" })).toHaveAttribute("aria-selected", "true");
    expect(await screen.findByText("Giảng viên đã dùng hôm nay")).toBeInTheDocument();
  });

  it("tab Usage: bảng giảng viên có số yêu cầu, bảng nhà cung cấp có cột Hôm nay, không có cột Yêu cầu", async () => {
    backend();
    render(<AiConnectionsPanel />);
    const teachers = (await screen.findByText("Theo giảng viên")).closest("section")!;
    expect(within(teachers).getByRole("columnheader", { name: "Yêu cầu" })).toBeInTheDocument();
    expect(within(teachers).getByText("17")).toBeInTheDocument();
    const models = screen.getByText("Theo nhà cung cấp").closest("section")!;
    expect(within(models).getByRole("columnheader", { name: "Hôm nay" })).toBeInTheDocument();
    expect(within(models).queryByRole("columnheader", { name: "Yêu cầu" })).not.toBeInTheDocument();
    expect(screen.getAllByText("84.000").length).toBeGreaterThan(0);
  });

  it("nhiều khóa cùng một nhà cung cấp, bật/tắt theo từng khóa gửi kèm version", async () => {
    const calls = backend();
    render(<AiConnectionsPanel />);
    await openSettings();

    expect(await screen.findByText("••••Zx9q")).toBeInTheDocument();
    expect(screen.getByText("••••Ab12")).toBeInTheDocument();
    expect(screen.getByText("0 · chặn")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("switch", { name: "Bật khóa Gemini ••••Zx9q" }));

    await waitFor(() => expect(calls.some((c) => c.url.endsWith("/status"))).toBe(true));
    const status = calls.find((c) => c.url.endsWith("/status"))!;
    expect(status.url).toBe("/api/v1/admin/ai/connections/k1/status");
    expect(status.body).toEqual({ enabled: false, version: 4 });
  });

  it("chưa kiểm tra thì chỉ có nút Kiểm tra; đã kiểm tra thì hiện kết quả, không hiện thời gian", async () => {
    const calls = backend();
    render(<AiConnectionsPanel />);
    await openSettings();
    const tested = (await screen.findByText("••••Zx9q")).closest("tr")!;
    expect(within(tested).getByText("Đạt")).toBeInTheDocument();
    expect(within(tested).queryByText(/21\/10/)).not.toBeInTheDocument();
    const untested = screen.getByText("••••Ab12").closest("tr")!;
    fireEvent.click(within(untested).getByRole("button", { name: "Kiểm tra" }));
    await waitFor(() => expect(calls.some((c) => c.url === "/api/v1/admin/ai/connections/k2:test")).toBe(true));
  });

  it("chưa có khóa, chưa có mức riêng thì bảng chỉ ghi chưa có", async () => {
    backend({ keys: [], quotas: [] });
    render(<AiConnectionsPanel />);
    await openSettings();
    expect(await screen.findByText("Chưa có khóa")).toBeInTheDocument();
    expect(await screen.findByText("Chưa có mức riêng")).toBeInTheDocument();
  });

  it("409 khi đổi trạng thái thì báo tải lại", async () => {
    backend({ statusConflict: true });
    render(<AiConnectionsPanel />);
    await openSettings();
    fireEvent.click(await screen.findByRole("switch", { name: "Bật khóa Gemini ••••Zx9q" }));
    expect(await screen.findByText("Khóa vừa được thay đổi ở nơi khác, hãy tải lại.")).toBeInTheDocument();
  });

  it("lưu hạn mức chung gửi version hiện tại", async () => {
    const calls = backend();
    render(<AiConnectionsPanel />);
    await openSettings();
    const field = await screen.findByLabelText("Giảng viên / ngày");
    await waitFor(() => expect(field).toHaveValue("20.000"));
    fireEvent.change(field, { target: { value: "30.0001" } });
    expect(field).toHaveValue("300.001");
    fireEvent.change(field, { target: { value: "30000" } });
    expect(field).toHaveValue("30.000");
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

    expect(await screen.findByText("Đã lưu hạn mức.")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "PUT" && c.url.endsWith("/quota-policy"))!.body).toEqual({
      defaultDayTokens: 30000,
      globalDayTokens: 500000,
      version: 3,
    });
  });

  it("popup thêm khóa có nút Kiểm tra với khóa vừa nhập; sửa dùng cùng popup, chỉ đổi tiêu đề", async () => {
    const calls = backend();
    render(<AiConnectionsPanel />);
    await openSettings();
    await screen.findByText("••••Zx9q");
    fireEvent.click(screen.getByRole("button", { name: "Thêm khóa" }));
    expect(screen.getByRole("dialog", { name: "Thêm khóa API" })).toBeInTheDocument();
    expect(screen.getByText("Kiểm tra có thể tính phí nhỏ.", { selector: "[role=dialog] p" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/Khóa API/), { target: { value: "AIzaSy-new-key-9999" } });
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Kiểm tra" }));
    // Không đạt thì chỉ 1–2 từ; câu đầy đủ của máy chủ ở tooltip.
    expect(await screen.findByText("Sai khóa")).toHaveAttribute("title", "Key API không hợp lệ (HTTP 401)");
    expect(calls.find((c) => c.url === "/api/v1/admin/ai/connections:test")!.body).toEqual({
      provider: "GEMINI",
      apiKey: "AIzaSy-new-key-9999",
      model: "gemini-2.5-flash",
      id: null,
    });
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Đóng" }));

    fireEvent.click(screen.getAllByRole("button", { name: "Thay khóa Gemini" })[0]!);
    expect(screen.getByRole("dialog", { name: "Sửa khóa API" })).toBeInTheDocument();
    expect(within(screen.getByRole("dialog")).getByRole("button", { name: "Kiểm tra" })).toBeInTheDocument();

    const keyField = screen.getByLabelText(/Khóa API/);
    expect(keyField).toHaveValue("");
    expect(keyField).toHaveAttribute("placeholder", "••••••••Zx9q");
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Xem khóa" }));
    await waitFor(() => expect(keyField).toHaveValue("AIzaSy-saved-Zx9q"));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Ẩn khóa" }));
    expect(keyField).toHaveValue("");
    expect(calls.some((c) => c.url === "/api/v1/admin/ai/connections/k1/key")).toBe(true);
  });

  it("thêm mức riêng: gõ email tới đâu gợi ý tới đó", async () => {
    const calls = backend();
    render(<AiConnectionsPanel />);
    await openSettings();
    await screen.findByText("Nguyễn Văn Bình");
    fireEvent.click(screen.getByRole("button", { name: "Thêm" }));
    fireEvent.change(screen.getByLabelText(/Email giảng viên/), { target: { value: "h" } });
    fireEvent.click(await screen.findByRole("button", { name: /hoatt@fpt.edu.vn/ }));
    expect(calls.some((c) => c.url === "/api/v1/admin/ai/teacher-candidates?q=h")).toBe(true);
    expect(screen.getByLabelText(/Email giảng viên/)).toHaveValue("hoatt@fpt.edu.vn");
  });
});
