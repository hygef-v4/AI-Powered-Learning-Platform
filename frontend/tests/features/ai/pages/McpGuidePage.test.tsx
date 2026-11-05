import { render, screen, within } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { McpGuidePage } from "@/features/ai/pages/McpGuidePage";
import { McpAgentsTab } from "@/features/ai/components/McpAgentsTab";

vi.mock("@/features/ai/api", () => ({
  getMcpCredentials: vi.fn().mockRejectedValue(new Error("offline")),
  createMcpCredential: vi.fn(), updateMcpCredential: vi.fn(), revokeMcpCredential: vi.fn(),
}));

it("has usable contents anchors, native client setup and a return link to the MCP tab", () => {
  render(<McpGuidePage />);
  expect(screen.getByRole("heading", { level: 1, name: "Tài liệu MCP" })).toBeInTheDocument();
  const contents = screen.getByRole("navigation", { name: "Mục lục tài liệu MCP" });
  for (const link of within(contents).getAllByRole("link")) {
    expect(document.getElementById(link.getAttribute("href")!.slice(1))).not.toBeNull();
  }
  expect(screen.getByRole("link", { name: "Quản lý kết nối MCP" })).toHaveAttribute("href", "/ai/connection?tab=mcp");
  expect(screen.getByLabelText("Ứng dụng agent")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Sao chép prompt" })).toBeInTheDocument();
});

it("explains proposal approval, role scopes, safe retries and rubric limitations", () => {
  render(<McpGuidePage />);
  expect(screen.getByText("create_assignment_draft", { selector: "code" })).toBeInTheDocument();
  expect(screen.getByText("record_grading_proposal", { selector: "code" })).toBeInTheDocument();
  expect(screen.getByText("record_private_feedback", { selector: "code" })).toBeInTheDocument();
  expect(screen.getByText(/MCP không bảo đảm/)).toBeInTheDocument();
  expect(screen.getByText(/Admin không có quyền/)).toBeInTheDocument();
  expect(screen.getByText(/Áp dụng đề xuất/)).toBeInTheDocument();
  expect(screen.getByText(/cùng requestRef/)).toBeInTheDocument();
});

it("keeps documentation reachable when credential loading fails", async () => {
  render(<McpAgentsTab />);
  expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được agent MCP");
  expect(screen.getByRole("link", { name: "Xem tài liệu MCP" })).toHaveAttribute("href", "/ai/mcp-guide");
});
