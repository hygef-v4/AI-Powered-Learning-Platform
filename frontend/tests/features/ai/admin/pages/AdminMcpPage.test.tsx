import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { McpAgentsTab } from "@/features/ai/components/McpAgentsTab";

afterEach(() => vi.unstubAllGlobals());

const AGENT = {
  id: "a1",
  label: "Reporting",
  access: "READ_ONLY",
  tokenHint: "ONCE",
  createdAt: "2026-10-26T10:00:00Z",
  expiresAt: "2026-11-02T10:00:00Z",
  lastUsedAt: null,
  status: "ACTIVE",
};

function backend(list: unknown[]) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      calls.push({ url, method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
      if (!url.startsWith("/api/v1/me/mcp-credentials")) return Promise.reject(new Error("unexpected personal endpoint"));
      const body =
        method === "POST"
          ? { credential: { ...AGENT, id: "new" }, token: "mcp_TEST_ONCE", endpoint: "/mcp" }
          : method === "PATCH"
            ? { ...AGENT, label: "Báo cáo", access: "READ_ONLY" }
            : list;
      return Promise.resolve(
        new Response(JSON.stringify(body), { status: method === "POST" ? 201 : 200, headers: { "Content-Type": "application/json" } }),
      );
    }),
  );
  return calls;
}

it("Admin tạo agent: popup token có biểu tượng sao chép, cấu hình mẫu điền sẵn token, nút Đóng", async () => {
  const calls = backend([]);
  render(<McpAgentsTab />);
  expect(await screen.findByText("Chưa có agent")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: /Kết nối agent mới/ }));
  const dialog = await screen.findByRole("dialog", { name: "Kết nối agent mới" });
  expect(within(dialog).getByRole("radio", { name: "Chỉ đọc" })).toBeChecked();
  fireEvent.change(within(dialog).getByLabelText(/Nhãn/), { target: { value: "Reporting" } });
  fireEvent.click(within(dialog).getByRole("button", { name: /Tạo token/ }));

  const token = await screen.findByRole("dialog", { name: "Token agent" });
  expect(within(token).getByText("mcp_TEST_ONCE")).toBeInTheDocument();
  expect(within(token).getByRole("button", { name: "Sao chép token" })).toBeInTheDocument();
  expect(token.querySelector("pre")!.textContent).toContain("Bearer mcp_TEST_ONCE");
  expect(token.querySelector("pre")!.textContent).toContain('[mcp_servers.nen-tang-hoc-tap]');
  expect(within(token).queryByText("Địa chỉ MCP")).not.toBeInTheDocument();
  fireEvent.click(within(token).getAllByRole("button", { name: "Đóng" }).at(-1)!);
  await waitFor(() => expect(screen.queryByText("mcp_TEST_ONCE")).not.toBeInTheDocument());
  expect(calls.every((c) => c.url === "/api/v1/me/mcp-credentials")).toBe(true);
});

it("Sửa agent dùng cùng popup: đổi nhãn, hạ quyền; token chỉ hiện dạng che; lưu xong không hiện cách kết nối", async () => {
  const calls = backend([{ ...AGENT, access: "READ_WRITE" }]);
  render(<McpAgentsTab />);
  expect(await screen.findByText("Hiệu lực")).toBeInTheDocument();
  expect(screen.queryByRole("columnheader", { name: /Lần dùng cuối/ })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Sửa agent Reporting" }));
  const dialog = await screen.findByRole("dialog", { name: "Sửa agent" });
  expect(within(dialog).queryByText(/Hạn dùng/)).not.toBeInTheDocument();

  expect(within(dialog).getByText("mcp_••••••••ONCE")).toBeInTheDocument();
  expect(within(dialog).queryByRole("button", { name: "Xem token" })).not.toBeInTheDocument();
  expect(within(dialog).getByText(/chỉ hiện một lần/)).toBeInTheDocument();

  fireEvent.change(within(dialog).getByLabelText(/Nhãn/), { target: { value: "Báo cáo" } });
  fireEvent.click(within(dialog).getByRole("radio", { name: "Chỉ đọc" }));
  fireEvent.click(within(dialog).getByRole("button", { name: "Lưu" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(calls.find((c) => c.method === "PATCH")).toEqual({
    url: "/api/v1/me/mcp-credentials/a1",
    method: "PATCH",
    body: { label: "Báo cáo", access: "READ_ONLY" },
  });
  expect(calls.some((c) => c.url.endsWith("/token"))).toBe(false);
  expect(screen.queryByRole("dialog", { name: "Token agent" })).not.toBeInTheDocument();
});

it("agent chỉ đọc không nâng lên đọc và ghi được: lựa chọn bị khóa và có hướng dẫn tạo agent mới", async () => {
  backend([AGENT]);
  render(<McpAgentsTab />);
  fireEvent.click(await screen.findByRole("button", { name: "Sửa agent Reporting" }));
  const dialog = await screen.findByRole("dialog", { name: "Sửa agent" });
  expect(within(dialog).getByRole("radio", { name: "Đọc và ghi" })).toBeDisabled();
  expect(within(dialog).getByText(/hãy tạo agent mới/)).toBeInTheDocument();
});

it("shows a retry action when Admin credential loading fails", async () => {
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(new Response(JSON.stringify({ status: 503 }), { status: 503 }))));
  render(<McpAgentsTab />);
  expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được");
  expect(screen.getByRole("button", { name: "Tải lại" })).toBeInTheDocument();
});
