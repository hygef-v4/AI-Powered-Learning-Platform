import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AdminMcpPage } from "./AdminMcpPage";

afterEach(() => vi.unstubAllGlobals());

it("Admin manages MCP independently of personal AI and sees the token only once", async () => {
  const calls: string[] = [];
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input); calls.push(url);
    if(url !== "/api/v1/me/mcp-credentials") return Promise.reject(new Error("unexpected personal endpoint"));
    const body = init?.method === "POST"
      ? { credential: { id: "new", label: "Reporting", access: "READ_ONLY", status: "ACTIVE", expiresAt: "2026-11-02T10:00:00Z" }, token: "mcp_TEST_ONCE", endpoint: "/mcp" }
      : [];
    return Promise.resolve(new Response(JSON.stringify(body), { status: init?.method === "POST" ? 201 : 200, headers: { "Content-Type": "application/json" } }));
  }));
  render(<AdminMcpPage />);
  fireEvent.click(await screen.findByRole("button", { name: /Kết nối agent mới/ }));
  const dialog = await screen.findByRole("dialog");
  expect(within(dialog).getByRole("radio", { name: /^Chỉ đọc/ })).toBeChecked();
  fireEvent.change(within(dialog).getByLabelText(/Nhãn/), { target: { value: "Reporting" } });
  fireEvent.click(within(dialog).getByRole("button", { name: /Tạo token/ }));
  expect(await screen.findByText("mcp_TEST_ONCE")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Tôi đã sao chép, đóng" }));
  await waitFor(() => expect(screen.queryByText("mcp_TEST_ONCE")).not.toBeInTheDocument());
  expect(calls.every(url => url === "/api/v1/me/mcp-credentials")).toBe(true);
});

it("shows a retry action when Admin credential loading fails", async () => {
  vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(new Response(JSON.stringify({ status: 503 }), { status: 503 }))));
  render(<AdminMcpPage />);
  expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được");
  expect(screen.getByRole("button", { name: "Tải lại" })).toBeInTheDocument();
});
