import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { renderToString } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";
import { McpClientSetup, McpSetupGuide, mcpConfigSample } from "@/features/ai/components/SetupGuides";

afterEach(() => vi.unstubAllGlobals());

describe("MCP client setup", () => {
  it("uses a stable relative endpoint during server rendering before reading the browser origin", () => {
    const html = renderToString(<McpClientSetup />);
    expect(html).not.toContain(window.location.origin);
    expect(html).toContain("/mcp");
  });
  it("links to dedicated documentation without showing the long setup form in the connection tab", () => {
    render(<McpSetupGuide />);
    expect(screen.getByRole("link", { name: "Xem tài liệu MCP" })).toHaveAttribute("href", "/ai/mcp-guide");
    expect(screen.queryByLabelText("Ứng dụng agent")).not.toBeInTheDocument();
  });
  it("generates each client's native personal configuration for copy and paste", () => {
    const endpoint = "http://localhost:3000/mcp";
    const openCode = JSON.parse(mcpConfigSample(endpoint, "secret", "opencode"));
    expect(openCode.mcp["nen-tang-hoc-tap"]).toEqual({ type: "remote", url: endpoint,
      oauth: false, headers: { Authorization: "Bearer secret" } });
    const codex = mcpConfigSample(endpoint, "secret", "codex");
    expect(codex).toContain('http_headers = { Authorization = "Bearer secret" }');
    expect(mcpConfigSample(endpoint, undefined, "codex")).not.toContain("secret");
    const claude = JSON.parse(mcpConfigSample(endpoint, "secret", "claude-code"));
    expect(claude.mcpServers["nen-tang-hoc-tap"]).toEqual({ type: "http", url: endpoint,
      headers: { Authorization: "Bearer secret" } });
    expect(JSON.parse(mcpConfigSample(endpoint, "secret", "antigravity"))
      .mcpServers["nen-tang-hoc-tap"]).toEqual({ serverUrl: endpoint, headers: { Authorization: "Bearer secret" } });
  });

  it("shows concrete configuration locations for the requested clients", () => {
    render(<McpClientSetup />);
    fireEvent.change(screen.getByLabelText("Ứng dụng agent"), { target: { value: "opencode" } });
    expect(screen.getByText("~/.config/opencode/opencode.json")).toBeInTheDocument();
    expect(screen.getByText(/chưa kiểm chứng kết nối thực tế/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Ứng dụng agent"), { target: { value: "antigravity" } });
    expect(screen.getByText("~/.gemini/config/mcp_config.json")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Ứng dụng agent"), { target: { value: "claude-code" } });
    expect(screen.getByText("~/.claude.json")).toBeInTheDocument();
    expect(screen.queryByText(/LEARNING_PLATFORM_MCP_TOKEN/)).not.toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Codex" })).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: /Gemini/ })).not.toBeInTheDocument();
    expect(screen.getAllByRole("option")).toHaveLength(4);
  });

  it("copies a setup prompt for the selected client without inventing a token", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    render(<McpClientSetup />);
    fireEvent.change(screen.getByLabelText("Ứng dụng agent"), { target: { value: "codex" } });
    fireEvent.click(screen.getByRole("button", { name: "Sao chép prompt" }));
    await waitFor(() => expect(writeText).toHaveBeenCalledOnce());
    const prompt = writeText.mock.calls[0]?.[0];
    expect(prompt).toContain("~/.codex/config.toml");
    expect(prompt).toContain(mcpConfigSample(`${window.location.origin}/mcp`, "<your MCP token>", "codex"));
    expect(prompt).toContain("No real token is provided");
    expect(prompt).toContain("preserve all other servers");
    expect(prompt).toContain("get_my_capabilities");
    expect(prompt).toContain("Do not create assignments, save proposals, or publish grades");
    expect(await screen.findByRole("status")).toHaveTextContent("Đã sao chép prompt");
  });

  it("includes the explicit dialog token only when copying and updates the selected config", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    render(<McpClientSetup token="mcp_dummy_secret" />);
    expect(writeText).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("Ứng dụng agent"), { target: { value: "opencode" } });
    fireEvent.click(screen.getByRole("button", { name: "Sao chép prompt" }));
    await waitFor(() => expect(writeText).toHaveBeenCalledOnce());
    const prompt = writeText.mock.calls[0]?.[0];
    expect(prompt).toContain("Bearer mcp_dummy_secret");
    expect(prompt).toContain("~/.config/opencode/opencode.json");
    expect(prompt).not.toContain("No real token is provided");
  });

  it("offers manual prompt copying if clipboard access fails", async () => {
    vi.stubGlobal("navigator", { clipboard: { writeText: vi.fn().mockRejectedValue(new Error("denied")) } });
    render(<McpClientSetup />);
    fireEvent.click(screen.getByRole("button", { name: "Sao chép prompt" }));
    expect(await screen.findByRole("status")).toHaveTextContent("Xem prompt");
    expect(screen.getByText("Xem prompt")).toBeInTheDocument();
  });
});
