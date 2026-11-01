vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams(window.location.search) }));
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AiUsageHistoryPage } from "@/features/ai/pages/AiUsageHistoryPage";
import type { UsageItem, UsagePage } from "@/features/ai/model/types";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

const TOOL: UsageItem = {
  id: "u1",
  createdAt: "2026-10-21T03:00:00Z",
  source: "MCP",
  provider: null,
  model: null,
  taskType: null,
  toolName: "read_own_work",
  outcome: "OK",
  inputTokens: null,
  outputTokens: null,
  cachedTokens: null,
  reasoningTokens: null,
  provenance: "UNAVAILABLE",
};

const TASK: UsageItem = {
  ...TOOL,
  id: "u2",
  source: "PERSONAL",
  provider: "OPENAI",
  model: "gpt-4.1-mini",
  taskType: "QUIZ_QUESTION_DRAFT",
  toolName: null,
  outcome: "READY",
  inputTokens: 1200,
  outputTokens: 300,
  provenance: "PROVIDER_MEASURED",
};

function page(items: UsageItem[]): UsagePage {
  return {
    items,
    page: 0,
    size: 50,
    total: items.length,
    shared: { requests: 0, tokens: 0 },
    personal: { requests: 1, tokens: 1500 },
    mcp: { requests: 1, tokens: 0 },
  };
}

function json(body: unknown) {
  return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }));
}

describe("AiUsageHistoryPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("token không có số hiện Không có số liệu, không hiện 0", async () => {
    vi.stubGlobal("fetch", vi.fn((url: string) => (String(url).startsWith("/api/v1/ai/sources") ? json([]) : json(page([TOOL, TASK])))));
    render(<AiUsageHistoryPage />);

    expect(await screen.findByText("Tool: read_own_work")).toBeInTheDocument();
    expect(screen.getAllByText("Không có số liệu").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("1.200 / 300")).toBeInTheDocument();
    expect(screen.getByText("Tạo nháp câu hỏi")).toBeInTheDocument();
    expect(screen.queryByRole("columnheader", { name: "Nguồn số liệu" })).not.toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Mô hình" })).toBeInTheDocument();
    expect(screen.queryByText("0 / 0")).not.toBeInTheDocument();
  });

  it("đổi tab nguồn và khoảng ngày gửi lại truy vấn", async () => {
    const fetchMock = vi.fn((url: string) => (String(url).startsWith("/api/v1/ai/sources") ? json([]) : json(page([]))));
    vi.stubGlobal("fetch", fetchMock);
    render(<AiUsageHistoryPage />);
    await screen.findByText("Chưa có lượt sử dụng nào trong khoảng này.");

    fireEvent.click(screen.getByRole("tab", { name: "Agent MCP" }));
    fireEvent.change(screen.getByLabelText("Từ ngày"), { target: { value: "2026-10-01" } });

    await waitFor(() => {
      const last = new URL(String(fetchMock.mock.calls.filter(([u]) => String(u).startsWith("/api/v1/me/ai-usage")).at(-1)![0]), "http://x");
      expect(last.searchParams.get("source")).toBe("MCP");
      expect(last.searchParams.get("from")).toBe("2026-09-30T17:00:00.000Z");
    });
  });
});

beforeEach(() => { window.history.replaceState(null, "", "/ai/connection"); });

it("restores source from URL on remount and ignores unknown or unavailable MCP", async () => {
  const fetchMock = vi.fn((url: string) => String(url).startsWith("/api/v1/ai/sources") ? json([]) : json(page([])));
  vi.stubGlobal("fetch", fetchMock);
  const view = render(<AiUsageHistoryPage />);
  fireEvent.click(screen.getByRole("tab", { name: "Agent MCP" }));
  view.unmount();
  const restored = render(<AiUsageHistoryPage />);
  expect(screen.getByRole("tab", { name: "Agent MCP" })).toHaveAttribute("aria-selected", "true");
  restored.rerender(<AiUsageHistoryPage mcpEnabled={false} />);
  expect(screen.getByRole("tab", { name: "Tất cả" })).toHaveAttribute("aria-selected", "true");
  window.history.replaceState(null, "", "?source=unknown");
  restored.rerender(<AiUsageHistoryPage />);
  expect(screen.getByRole("tab", { name: "Tất cả" })).toHaveAttribute("aria-selected", "true");
  await screen.findByText("Chưa có lượt sử dụng nào trong khoảng này.");
});
