"use client";

import { useId, useState, useSyncExternalStore, type ReactNode } from "react";
import { Button, ButtonLink, Card, IconButton, Select, SettingsGroup } from "@/shared/ui";
import { msg } from "@/i18n/msg";

const KEY_STEPS: { provider: string; steps: string[] }[] = [
  {
    provider: "Gemini",
    steps: [
      msg("ai.components.SetupGuides.moGoogleAiStudioAistudioGoogle"),
      msg("ai.components.SetupGuides.chonGetApiKeyCreateApi"),
      msg("ai.components.SetupGuides.saoChepKhoaVaDanVao"),
    ],
  },
  {
    provider: "OpenAI",
    steps: [
      msg("ai.components.SetupGuides.moPlatformOpenaiComApiKeys"),
      msg("ai.components.SetupGuides.bamCreateNewSecretKeyDat"),
      msg("ai.components.SetupGuides.napTinDungChoTaiKhoan"),
    ],
  },
  {
    provider: "Claude",
    steps: [
      msg("ai.components.SetupGuides.moConsoleAnthropicComApiKeys"),
      msg("ai.components.SetupGuides.bamCreateKeyChonWorkspace"),
      msg("ai.components.SetupGuides.saoChepKhoaChiHienMot"),
    ],
  },
];

/** Hướng dẫn lấy khóa API từng nhà cung cấp; các bước có thể đổi theo trang của nhà cung cấp. */
export function ApiSetupGuide() {
  return (
    <SettingsGroup title={msg("ai.components.SetupGuides.huongDanLayKhoaApi")}>
      <div className="mt-2 grid gap-3 md:grid-cols-3">
        {KEY_STEPS.map((guide) => (
          <div
            key={guide.provider}
            className="rounded-xl border border-line p-3.5"
          >
            <p className="mb-1.5 text-[13px] font-bold text-ink">
              {guide.provider}
            </p>
            <ol className="list-decimal space-y-1 pl-4 text-xs text-body">
              {guide.steps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
          </div>
        ))}
      </div>
      <p className="mt-3 text-xs text-muted">{msg("ai.components.SetupGuides.khongChiaSeKhoaChoNguoi")}</p>
    </SettingsGroup>
  );
}

/** Đoạn cấu hình MCP mẫu: URL /mcp trên địa chỉ hiện tại, token đặt trong header Authorization; thụt 4 dấu cách. */
export type McpClient = "codex" | "claude-code" | "opencode" | "antigravity";

export function buildMcpConfigSample(endpoint: string, token = msg("ai.components.SetupGuides.tokenCuaBan"), client: McpClient = "codex") {
  if (client === "codex") return '[mcp_servers.nen-tang-hoc-tap]\nurl = ' + JSON.stringify(endpoint)
    + '\nhttp_headers = { Authorization = ' + JSON.stringify(`Bearer ${token}`) + ' }';
  const server = client === "opencode"
    ? { type: "remote", url: endpoint, oauth: false, headers: { Authorization: `Bearer ${token}` } }
    : client === "claude-code"
      ? { type: "http", url: endpoint, headers: { Authorization: `Bearer ${token}` } }
      : { serverUrl: endpoint, headers: { Authorization: `Bearer ${token}` } };
  return JSON.stringify({ [client === "opencode" ? "mcp" : "mcpServers"]: { "nen-tang-hoc-tap": server } }, null, 4);
}

export function resolveMcpEndpoint(): string {
  return typeof window === "undefined"
    ? "/mcp"
    : `${window.location.origin}/mcp`;
}

const subscribeToOrigin = () => () => {};
const resolveServerEndpoint = () => "/mcp";

const JSON_TOKEN = /("(?:[^"\\]|\\.)*"|[{}[\]]|[:,])/g;

/**
 * Khối JSON tô màu như trình soạn code: khóa, chuỗi, dấu {} [], dấu : và , mỗi loại một màu; nền tối ở cả hai giao diện.
 * {@code action} (nút sao chép) nằm ở góc trên bên phải trong khối.
 */
export function JsonCode({
  text,
  action,
}: {
  text: string;
  action?: ReactNode;
}) {
  const parts = text.split(JSON_TOKEN);
  return (
    <div className="relative">
      {action && <div className="absolute top-2 right-2">{action}</div>}
      <pre className="overflow-x-auto rounded-xl bg-code p-4 pr-12 font-mono text-[12px] leading-relaxed text-code-ink">
        <code>
          {parts.map((part, i) => {
            if (i % 2 === 0) return part;
            if (part.startsWith('"')) {
              const isKey =
                /^\s*:/.test(parts[i + 1] ?? "") ||
                (parts[i + 1] === "" && parts[i + 2] === ":");
              return (
                <span
                  key={i}
                  className={isKey ? "text-code-key" : "text-code-string"}
                >
                  {part}
                </span>
              );
            }
            if (part === ":" || part === ",") {
              return (
                <span key={i} className="text-code-muted">
                  {part}
                </span>
              );
            }
            return (
              <span key={i} className="text-code-number">
                {part}
              </span>
            );
          })}
        </code>
      </pre>
    </div>
  );
}

/** Client configuration is shared by the guide and explicit token-copy dialog. */
export function McpClientSetup({ token }: { token?: string }) {
  const id = useId();
  const [client, setClient] = useState<McpClient>("codex");
  const [copyState, setCopyState] = useState("");
  const endpoint = useSyncExternalStore(subscribeToOrigin, resolveMcpEndpoint, resolveServerEndpoint);
  const config = buildMcpConfigSample(endpoint, token, client);
  const locations = { codex: "~/.codex/config.toml", "claude-code": "~/.claude.json",
    opencode: "~/.config/opencode/opencode.json",
    antigravity: "~/.gemini/config/mcp_config.json" };
  const prompt = `Set up the nen-tang-hoc-tap MCP server for ${client} on this machine.
Personal configuration file: ${locations[client]} (~ means the current user's home directory).
${client === "antigravity" ? "Confirm the active file through Settings → Customizations → Installed MCP Servers → Open MCP Config; older versions may use MCP Servers → Manage MCP Servers → View raw config and ~/.gemini/antigravity/mcp_config.json.\n" : ""}Read the existing file before editing. Add or update only nen-tang-hoc-tap; preserve all other servers and settings. If the file is invalid, report the problem instead of overwriting it. Make a private local backup before editing. Keep configuration files and backups containing tokens out of Git.
${token ? "The configuration below contains the real token I explicitly provided. Do not repeat it in your response, log it, or commit it." : msg("ai.components.SetupGuides.noRealTokenIsProvidedGuide")}
Merge this configuration using the exact client-specific format${client === "claude-code" ? "; place mcpServers at the top level of ~/.claude.json" : ""}:

${buildMcpConfigSample(endpoint, token ?? "<your MCP token>", client)}

After saving, reload MCP if supported. If restarting the application is required, explain what I need to do and wait; do not close my running session. Check the connection, then call get_my_capabilities and one read-only tool allowed by the returned capabilities. Do not create assignments, save proposals, or publish grades to test the connection. Report success only after an actual read succeeds. If the token is missing or you cannot verify the connection, clearly state the remaining step. Never send the token to another endpoint.`;
  return <div className="space-y-3">
    <label htmlFor={id} className="block text-xs font-semibold text-body">{msg("ai.components.SetupGuides.ungDungAgent")}</label>
    <Select id={id} value={client} onChange={(event) => { setClient(event.target.value as McpClient); setCopyState(""); }}>
      <option value="codex">Codex</option>
      <option value="claude-code">Claude Code</option>
      <option value="opencode">OpenCode</option><option value="antigravity">Antigravity</option>
    </Select>
    {client !== "codex" && <p className="text-xs text-muted">{msg("ai.components.SetupGuides.mauTheoTaiLieuChinhThuc")}</p>}
    <div className="flex flex-wrap items-center gap-2">
      <Button size="sm" icon="copy" onClick={async () => {
        try {
          await navigator.clipboard.writeText(prompt);
          setCopyState(msg("ai.components.SetupGuides.daSaoChepPromptDanVao"));
        } catch {
          setCopyState(msg("ai.components.SetupGuides.khongSaoChepDuocMoXem"));
        }
      }}>{msg("ai.components.SetupGuides.saoChepPrompt")}</Button>
      <span className="text-xs text-muted">{msg("ai.components.SetupGuides.danVaoAgentDeNhoTu")}</span>
    </div>
    {token && <p className="text-xs text-muted">{msg("ai.components.SetupGuides.promptChuaTokenCuaBanChi")}</p>}
    <details className="text-xs text-body">
      <summary className="cursor-pointer font-semibold">Xem prompt</summary>
      <pre className="mt-2 max-h-72 overflow-y-auto rounded-lg bg-sunken p-3 font-mono text-xs whitespace-pre-wrap break-words">{prompt}</pre>
    </details>
    <ol className="list-decimal space-y-2 pl-4 text-[13px] text-body">
      <li><strong>{msg("ai.components.SetupGuides.taoToken")}</strong> {msg("ai.components.SetupGuides.bamKetNoiAgentMoiChon")}</li>
      <li><strong>{msg("ai.components.SetupGuides.moCauHinh")}</strong> {client === "antigravity" ? msg("ai.components.SetupGuides.moSettingsCustomizationsInstalledMcpServers") : msg("ai.components.SetupGuides.moFileBenDuoiBangTrinh")} <code className="break-words">{locations[client]}</code>{msg("ai.components.SetupGuides.dauLaThuMucNguoiDung")}</li>
      <li><strong>{msg("ai.components.SetupGuides.danCauHinhBenDuoi")}</strong> {msg("ai.components.SetupGuides.ghepVaoCauHinhHienCo")}{token ? msg("ai.components.SetupGuides.cauHinhDaChuaTokenVua") : msg("ai.components.SetupGuides.thayTokenCuaBanBangToken")} {msg("ai.components.SetupGuides.luuTrongCauHinhCaNhan")}</li>
    </ol>
    <JsonCode text={config} action={token ? <IconButton icon="copy" label={msg("ai.components.SetupGuides.saoChepCauHinh")}
      className="text-code-ink hover:bg-white/10 hover:text-white" onClick={() => {
        void navigator.clipboard.writeText(config).then(() => setCopyState(msg("ai.components.SetupGuides.daSaoChep")), () => setCopyState(msg("ai.components.SetupGuides.khongSaoChepDuocHayChon")));
      }} /> : undefined} />
    <p className="text-[13px] text-body"><strong>{msg("ai.components.SetupGuides.t4KiemTraKetNoi")}</strong> {msg("ai.components.SetupGuides.taiLaiMcpTrongUngDung")}</p>
    <p role="status" className="text-xs text-muted">{copyState}</p>
  </div>;
}

export function McpSetupGuide() {
  return <Card as="section" padding="lg" className="flex flex-wrap items-center justify-between gap-4" aria-label={msg("ai.components.SetupGuides.troGiupMcp")}>
    <div className="min-w-0">
      <h2 className="text-sm font-semibold text-ink">{msg("ai.components.SetupGuides.taiLieuMcp")}</h2>
      <p className="mt-1 text-[13px] text-muted">{msg("ai.components.SetupGuides.cachKetNoiUngDungTao")}</p>
    </div>
    <ButtonLink href="/ai/mcp-guide" size="sm">{msg("ai.components.SetupGuides.xemTaiLieuMcp")}</ButtonLink>
  </Card>;
}
