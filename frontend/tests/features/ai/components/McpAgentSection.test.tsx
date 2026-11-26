import { render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { McpAgentSection } from "@/features/ai/components/McpAgentSection";
import type { McpCredential } from "@/features/ai/model/types";

function makeCredential(index: number): McpCredential {
  return {
    id: `c${index}`, label: `Agent ${index}`, access: "READ_ONLY", tokenHint: `ab${index}`,
    createdAt: "2026-11-26T00:00:00Z", expiresAt: "2026-12-26T00:00:00Z", lastUsedAt: null, status: "ACTIVE",
  };
}

function renderSection(count: number) {
  render(
    <McpAgentSection
      credentials={Array.from({ length: count }, (_, i) => makeCredential(i))}
      onCreate={vi.fn()} onUpdate={vi.fn()} onRevoke={vi.fn()}
    />,
  );
}

it("explains why a new agent cannot be connected at the limit (KAN-233)", () => {
  renderSection(5);
  expect(screen.getByRole("button", { name: "Kết nối agent mới" })).toBeDisabled();
  expect(screen.getByTestId("mcp-agent-limit")).toHaveTextContent("tối đa 5 agent còn hiệu lực");
});

it("shows no limit hint below the limit", () => {
  renderSection(4);
  expect(screen.getByRole("button", { name: "Kết nối agent mới" })).toBeEnabled();
  expect(screen.queryByTestId("mcp-agent-limit")).toBeNull();
});
