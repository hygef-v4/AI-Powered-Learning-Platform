vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(window.location.search), useRouter: () => ({ push: vi.fn(), back: vi.fn() }) }));
import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { AiConnectionPage } from "@/features/ai/pages/AiConnectionPage";
import type { ConnectionView, McpCredential, ProviderInfo } from "@/features/ai/model/types";
import type { SourceOption } from "@/shared/ai";

const session = vi.hoisted(() => ({ role: "TEACHER" }));
vi.mock("@/features/identity/hooks/SessionProvider", () => ({
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
  id: "k-claude",
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
    if (url === "/api/v1/me/ai-source-preference" && method === "PUT") return json(JSON.parse(String(init?.body)));
    if (url.startsWith("/api/v1/me/ai-usage"))
      return json({ items: [], page: 0, size: 50, total: 0, shared: { requests: 0, tokens: 0 }, personal: { requests: 3, tokens: 0 }, mcp: { requests: 2, tokens: 0 } });
    if (url.startsWith("/api/v1/me/ai-connections/") && method === "PUT") return json(CLAUDE);
    if (url === "/api/v1/me/ai-connections" && method === "POST") return json({ ...CLAUDE, id: "k-new", provider: "GEMINI" });
    if (method === "DELETE") return json(null, 204);
    return json({ status: 404 }, 404);
  });
  vi.stubGlobal("fetch", fetchMock);
  return calls;
}





it("restores personal AI tab from URL and rejects unauthorized MCP", async () => {
  backend();
  session.role = "TEACHER";
  window.history.replaceState(null, "", "/ai/connection?tab=settings");
  const view = render(<AiConnectionPage />);
  expect(await screen.findByText("••••3kQf")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("tab", { name: "Agent MCP" }));
  view.unmount();
  const restored = render(<AiConnectionPage />);
  expect(await screen.findByText("Claude Desktop")).toBeInTheDocument();
  session.role = "ADMIN";
  restored.rerender(<AiConnectionPage />);
  expect(screen.queryByRole("tab", { name: "Agent MCP" })).not.toBeInTheDocument();
  expect(screen.getByRole("tab", { name: "Lịch sử sử dụng" })).toHaveAttribute("aria-selected", "true");
});
