import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AiConnectionPage } from "./AiConnectionPage";
import type { ConnectionView, McpCredential, ProviderInfo } from "./types";
import type { SourceOption } from "@/shared/ai";

const session = vi.hoisted(() => ({ role: "TEACHER" }));
vi.mock("@/features/identity/session", () => ({
  useSession: () => ({ status: "authenticated", profile: { role: session.role } }),
}));

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const PROVIDERS: ProviderInfo[] = [
  { provider: "GEMINI", models: ["gemini-2.5-flash", "gemini-2.5-pro"], maxOutputTokens: 8192 },
  { provider: "OPENAI", models: ["gpt-4.1-mini", "gpt-4.1"], maxOutputTokens: 8192 },
  { provider: "CLAUDE", models: ["claude-sonnet-5-5", "claude-haiku-5-5"], maxOutputTokens: 8192 },
];

const CLAUDE: ConnectionView = {
  provider: "CLAUDE",
  configured: true,
  model: "claude-sonnet-5-5",
  keyHint: "3kQf",
  status: "ACTIVE",
  lastTestAt: "2026-10-21T03:00:00Z",
  lastTestResult: "Đạt",
  version: 2,
};

const NOT_SET = (provider: ConnectionView["provider"]): ConnectionView => ({
  provider,
  configured: false,
  model: null,
  keyHint: null,
  status: null,
  lastTestAt: null,
  lastTestResult: null,
  version: 0,
});

const AGENT: McpCredential = {
  id: "c1",
  label: "Claude Desktop",
  access: "READ_ONLY",
  tokenHint: "AbCd",
  createdAt: "2026-10-01T03:00:00Z",
  expiresAt: "2026-10-31T03:00:00Z",
  lastUsedAt: null,
  status: "ACTIVE",
};

const SHARED: SourceOption = {
  source: "SHARED",
  provider: "GEMINI",
  model: "gemini-2.5-flash",
  keyHint: null,
  available: true,
  reason: null,
  quota: { used: 4000, limit: 10000, left: 6000, resetAt: "2026-10-21T17:00:00Z" },
  costBearer: "SCHOOL",
};

function json(body: unknown, status = 200) {
  return Promise.resolve(
    status === 204
      ? new Response(null, { status })
      : new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }),
  );
}

function backend(overrides: { connections?: ConnectionView[]; credentials?: McpCredential[]; sources?: SourceOption[] } = {}) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    calls.push({ url, method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    if (url === "/api/v1/ai/providers") return json(PROVIDERS);
    if (url === "/api/v1/me/ai-connections" && method === "GET")
      return json(overrides.connections ?? [NOT_SET("GEMINI"), NOT_SET("OPENAI"), CLAUDE]);
    if (url === "/api/v1/me/mcp-credentials" && method === "GET") return json(overrides.credentials ?? [AGENT]);
    if (url === "/api/v1/me/mcp-credentials" && method === "POST")
      return json({ credential: { ...AGENT, id: "c2", label: "Cursor" }, token: "mcp_SECRETtokenXYZ9", endpoint: "/mcp" }, 201);
    if (url === "/api/v1/ai/sources") return json(overrides.sources ?? [SHARED]);
    if (url.startsWith("/api/v1/me/ai-usage"))
      return json({ items: [], page: 0, size: 50, total: 0, shared: { requests: 0, tokens: 0 }, personal: { requests: 3, tokens: 0 }, mcp: { requests: 2, tokens: 0 } });
    if (url.startsWith("/api/v1/me/ai-connections/") && method === "PUT") return json(CLAUDE);
    if (method === "DELETE") return json(null, 204);
    return json({ status: 404 }, 404);
  });
  vi.stubGlobal("fetch", fetchMock);
  return calls;
}


function openTab(name: string) {
  fireEvent.click(within(screen.getByRole("tablist", { name: "Kết nối AI" })).getByRole("tab", { name }));
}

describe("AiConnectionPage", () => {
  beforeEach(() => { session.role = "TEACHER"; });
  afterEach(() => vi.unstubAllGlobals());

  it("mở tab Usage trước: API hệ thống hôm nay và số lượt gọi", async () => {
    backend();
    render(<AiConnectionPage />);
    expect(screen.getByRole("tab", { name: "Usage" })).toHaveAttribute("aria-selected", "true");
    expect(await screen.findByText("4.000")).toBeInTheDocument();
    expect(screen.getByText("/ 10.000 token", { exact: false })).toBeInTheDocument();
    expect(await screen.findByText("3")).toBeInTheDocument();
  });

  it("tab Cài đặt chỉ liệt kê khóa đã thêm, khóa luôn che", async () => {
    backend();
    render(<AiConnectionPage initialTab="settings" />);
    expect(await screen.findByText("••••3kQf")).toBeInTheDocument();
    expect(within(screen.getByRole("table")).queryByText("Gemini")).not.toBeInTheDocument();
  });

  it("học sinh có tab Agent MCP và quản lý agent của chính mình", async () => {
    session.role = "STUDENT";
    const calls = backend({ sources: [] });
    render(<AiConnectionPage initialTab="settings" />);
    await screen.findByText("••••3kQf");
    fireEvent.click(screen.getByRole("tab", { name: "Agent MCP" }));
    expect(await screen.findByText("Claude Desktop")).toBeInTheDocument();
    expect(calls.some((c) => c.url === "/api/v1/me/mcp-credentials")).toBe(true);
  });

  it("admin không thấy Agent MCP kể cả khi mở tab MCP trực tiếp", async () => {
    session.role = "ADMIN";
    const calls = backend({ sources: [] });
    render(<AiConnectionPage initialTab="mcp" />);
    expect(screen.queryByRole("tab", { name: "Agent MCP" })).not.toBeInTheDocument();
    await waitFor(() => expect(calls.some((c) => c.url.includes("ai-usage"))).toBe(true));
    expect(calls.some((c) => c.url.includes("mcp-credentials"))).toBe(false);
  });

  it("thêm khóa gửi khóa và mô hình, không giữ khóa trên màn sau khi lưu", async () => {
    const calls = backend();
    render(<AiConnectionPage initialTab="settings" />);
    await screen.findByText("••••3kQf");

    fireEvent.click(screen.getByRole("button", { name: /Thêm khóa/ }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/Khóa API/), { target: { value: "AIzaSy-test-key-0000" } });
    fireEvent.click(within(dialog).getByRole("button", { name: /Lưu khóa/ }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    const put = calls.find((c) => c.method === "PUT")!;
    expect(put.url).toBe("/api/v1/me/ai-connections/GEMINI");
    expect(put.body).toEqual({ apiKey: "AIzaSy-test-key-0000", model: "gemini-2.5-flash" });
    expect(screen.queryByDisplayValue("AIzaSy-test-key-0000")).not.toBeInTheDocument();
  });

  it("nút Thêm khóa không cho chọn nhà cung cấp đã có khóa (tránh 409, dùng Thay khóa)", async () => {
    backend();
    render(<AiConnectionPage initialTab="settings" />);
    await screen.findByText("••••3kQf");

    fireEvent.click(screen.getByRole("button", { name: /Thêm khóa/ }));
    const dialog = await screen.findByRole("dialog");
    const options = within(within(dialog).getByLabelText(/Nhà cung cấp/)).getAllByRole("option").map((o) => o.textContent);
    expect(options.some((t) => t?.startsWith("Claude"))).toBe(false);
    expect(options.some((t) => t?.startsWith("Gemini"))).toBe(true);
  });

  it("thay khóa đã lưu chỉ để đổi mô hình thì không bắt nhập lại khóa", async () => {
    const calls = backend();
    render(<AiConnectionPage initialTab="settings" />);
    await screen.findByText("••••3kQf");

    fireEvent.click(screen.getByRole("button", { name: "Thay khóa Claude" }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/Mô hình/), { target: { value: "claude-haiku-5-5" } });
    fireEvent.click(within(dialog).getByRole("button", { name: /Lưu khóa/ }));

    await waitFor(() => expect(calls.some((c) => c.method === "PUT")).toBe(true));
    expect(calls.find((c) => c.method === "PUT")!.body).toEqual({ model: "claude-haiku-5-5", version: 2 });
  });

  it("tạo agent hiện token một lần, đóng rồi không còn trên màn", async () => {
    const calls = backend();
    render(<AiConnectionPage />);
    openTab("Agent MCP");
    await screen.findByText("Claude Desktop");

    fireEvent.click(screen.getByRole("button", { name: /Kết nối agent mới/ }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText(/Nhãn/), { target: { value: "Cursor" } });
    fireEvent.click(within(dialog).getByRole("radio", { name: "90 ngày" }));
    fireEvent.click(within(dialog).getByRole("button", { name: /Tạo token/ }));

    expect(await screen.findByText("mcp_SECRETtokenXYZ9")).toBeInTheDocument();
    expect(calls.find((c) => c.method === "POST")!.body).toEqual({ label: "Cursor", access: "READ_ONLY", days: 90 });
    fireEvent.click(screen.getAllByRole("button", { name: "Đóng" }).at(-1)!);
    expect(screen.queryByText("mcp_SECRETtokenXYZ9")).not.toBeInTheDocument();
  });

  it("đủ 5 agent còn hiệu lực thì khóa nút kết nối mới", async () => {
    backend({ credentials: [1, 2, 3, 4, 5].map((i) => ({ ...AGENT, id: `c${i}`, label: `Agent ${i}` })) });
    render(<AiConnectionPage initialTab="mcp" />);
    await screen.findByText("Agent 1");
    expect(screen.getByRole("button", { name: /Kết nối agent mới/ })).toBeDisabled();
  });
});
