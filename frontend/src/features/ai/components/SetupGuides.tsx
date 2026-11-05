"use client";

import { useId, useState, useSyncExternalStore, type ReactNode } from "react";
import { Button, ButtonLink, IconButton, Select, SettingsGroup } from "@/shared/ui";

const KEY_STEPS: { provider: string; steps: string[] }[] = [
  {
    provider: "Gemini",
    steps: [
      "Mở Google AI Studio (aistudio.google.com), đăng nhập tài khoản Google.",
      "Chọn Get API key → Create API key.",
      "Sao chép khóa và dán vào popup Thêm khóa.",
    ],
  },
  {
    provider: "OpenAI",
    steps: [
      "Mở platform.openai.com → API keys.",
      "Bấm Create new secret key, đặt tên dễ nhớ.",
      "Nạp tín dụng cho tài khoản nếu chưa có, rồi dán khóa vào popup.",
    ],
  },
  {
    provider: "Claude",
    steps: [
      "Mở console.anthropic.com → API keys.",
      "Bấm Create Key, chọn workspace.",
      "Sao chép khóa (chỉ hiện một lần) và dán vào popup.",
    ],
  },
];

/** Hướng dẫn lấy khóa API từng nhà cung cấp; các bước có thể đổi theo trang của nhà cung cấp. */
export function ApiSetupGuide() {
  return (
    <SettingsGroup title="Hướng dẫn lấy khóa API">
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
      <p className="mt-3 text-[11px] text-muted">Không chia sẻ khóa cho người khác.</p>
    </SettingsGroup>
  );
}

/** Đoạn cấu hình MCP mẫu: URL /mcp trên địa chỉ hiện tại, token đặt trong header Authorization; thụt 4 dấu cách. */
export type McpClient = "codex" | "claude-code" | "opencode" | "antigravity";

export function mcpConfigSample(endpoint: string, token = "<token của bạn>", client: McpClient = "codex") {
  if (client === "codex") return '[mcp_servers.nen-tang-hoc-tap]\nurl = ' + JSON.stringify(endpoint)
    + '\nhttp_headers = { Authorization = ' + JSON.stringify(`Bearer ${token}`) + ' }';
  const server = client === "opencode"
    ? { type: "remote", url: endpoint, oauth: false, headers: { Authorization: `Bearer ${token}` } }
    : client === "claude-code"
      ? { type: "http", url: endpoint, headers: { Authorization: `Bearer ${token}` } }
      : { serverUrl: endpoint, headers: { Authorization: `Bearer ${token}` } };
  return JSON.stringify({ [client === "opencode" ? "mcp" : "mcpServers"]: { "nen-tang-hoc-tap": server } }, null, 4);
}

export function mcpEndpoint(): string {
  return typeof window === "undefined"
    ? "/mcp"
    : `${window.location.origin}/mcp`;
}

const subscribeToOrigin = () => () => {};
const serverEndpoint = () => "/mcp";

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
      <pre className="overflow-x-auto rounded-xl bg-[#17212b] p-4 pr-12 font-mono text-[12px] leading-relaxed text-[#d4dbe3]">
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
                  className={isKey ? "text-[#7cc7ff]" : "text-[#f5b97a]"}
                >
                  {part}
                </span>
              );
            }
            if (part === ":" || part === ",") {
              return (
                <span key={i} className="text-[#9aa7b4]">
                  {part}
                </span>
              );
            }
            return (
              <span key={i} className="text-[#f2d36b]">
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
  const endpoint = useSyncExternalStore(subscribeToOrigin, mcpEndpoint, serverEndpoint);
  const config = mcpConfigSample(endpoint, token, client);
  const locations = { codex: "~/.codex/config.toml", "claude-code": "~/.claude.json",
    opencode: "~/.config/opencode/opencode.json",
    antigravity: "~/.gemini/config/mcp_config.json" };
  const prompt = `Set up the nen-tang-hoc-tap MCP server for ${client} on this machine.
Personal configuration file: ${locations[client]} (~ means the current user's home directory).
${client === "antigravity" ? "Confirm the active file through Settings → Customizations → Installed MCP Servers → Open MCP Config; older versions may use MCP Servers → Manage MCP Servers → View raw config and ~/.gemini/antigravity/mcp_config.json.\n" : ""}Read the existing file before editing. Add or update only nen-tang-hoc-tap; preserve all other servers and settings. If the file is invalid, report the problem instead of overwriting it. Make a private local backup before editing. Keep configuration files and backups containing tokens out of Git.
${token ? "The configuration below contains the real token I explicitly provided. Do not repeat it in your response, log it, or commit it." : "No real token is provided. Guide me to create one under AI Connections → Agent MCP (Kết nối AI → Agent MCP) and enter it directly in my personal configuration file; do not ask me to paste the token into chat. Never authenticate with a placeholder token."}
Merge this configuration using the exact client-specific format${client === "claude-code" ? "; place mcpServers at the top level of ~/.claude.json" : ""}:

${mcpConfigSample(endpoint, token ?? "<your MCP token>", client)}

After saving, reload MCP if supported. If restarting the application is required, explain what I need to do and wait; do not close my running session. Check the connection, then call get_my_capabilities and one read-only tool allowed by the returned capabilities. Do not create assignments, save proposals, or publish grades to test the connection. Report success only after an actual read succeeds. If the token is missing or you cannot verify the connection, clearly state the remaining step. Never send the token to another endpoint.`;
  return <div className="space-y-3">
    <label htmlFor={id} className="block text-xs font-semibold text-body">Ứng dụng agent</label>
    <Select id={id} value={client} onChange={(event) => { setClient(event.target.value as McpClient); setCopyState(""); }}>
      <option value="codex">Codex</option>
      <option value="claude-code">Claude Code</option>
      <option value="opencode">OpenCode</option><option value="antigravity">Antigravity</option>
    </Select>
    {client !== "codex" && <p className="text-xs text-muted">Mẫu theo tài liệu chính thức; chưa kiểm chứng kết nối thực tế với ứng dụng này trong môi trường hiện tại.</p>}
    <div className="flex flex-wrap items-center gap-2">
      <Button size="sm" icon="copy" onClick={async () => {
        try {
          await navigator.clipboard.writeText(prompt);
          setCopyState("Đã sao chép prompt. Dán vào agent để thiết lập.");
        } catch {
          setCopyState("Không sao chép được. Mở Xem prompt để sao chép thủ công.");
        }
      }}>Sao chép prompt</Button>
      <span className="text-xs text-muted">Dán vào agent để nhờ tự thiết lập MCP.</span>
    </div>
    {token && <p className="text-xs text-muted">Prompt chứa token của bạn. Chỉ dán vào agent bạn tin cậy.</p>}
    <details className="text-xs text-body">
      <summary className="cursor-pointer font-semibold">Xem prompt</summary>
      <pre className="mt-2 max-h-72 overflow-y-auto rounded-lg bg-sunken p-3 font-mono text-xs whitespace-pre-wrap break-words">{prompt}</pre>
    </details>
    <ol className="list-decimal space-y-2 pl-4 text-[13px] text-body">
      <li><strong>Tạo token.</strong> Bấm Kết nối agent mới. Chọn Chỉ đọc để xem dữ liệu, hoặc Đọc và ghi để tạo đề và lưu đề xuất chấm.</li>
      <li><strong>Mở cấu hình.</strong> {client === "antigravity" ? "Mở Settings → Customizations → Installed MCP Servers → Open MCP Config. Bản cũ: MCP Servers → Manage MCP Servers → View raw config." : "Mở file bên dưới bằng trình soạn thảo; tạo file nếu chưa có."} <code className="break-words">{locations[client]}</code>. Dấu ~ là thư mục người dùng, trên Windows thường là C:\Users\tên-bạn.</li>
      <li><strong>Dán cấu hình bên dưới.</strong> Ghép vào cấu hình hiện có, giữ nguyên các server khác. {token ? "Cấu hình đã chứa token vừa tạo." : "Thay <token của bạn> bằng token vừa tạo."} Lưu trong cấu hình cá nhân, không đưa file chứa token lên Git.</li>
    </ol>
    <JsonCode text={config} action={token ? <IconButton icon="copy" label="Sao chép cấu hình"
      className="text-[#d4dbe3] hover:bg-white/10 hover:text-white" onClick={() => {
        void navigator.clipboard.writeText(config).then(() => setCopyState("Đã sao chép"), () => setCopyState("Không sao chép được. Hãy chọn và sao chép đoạn cấu hình."));
      }} /> : undefined} />
    <p className="text-[13px] text-body"><strong>4. Kiểm tra kết nối.</strong> Tải lại MCP trong ứng dụng; nếu phiên bản đang dùng yêu cầu khởi động lại, lưu công việc trước. Nhờ agent: “Gọi get_my_capabilities, sau đó dùng một tool chỉ đọc được tài khoản cho phép.” Chỉ xem là kết nối thành công khi đọc được dữ liệu thực tế.</p>
    <p role="status" className="text-xs text-muted">{copyState}</p>
  </div>;
}

export function McpSetupGuide() {
  return <section aria-label="Trợ giúp MCP" className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-line bg-surface p-5">
    <div className="min-w-0">
      <h2 className="text-sm font-semibold text-ink">Tài liệu MCP</h2>
      <p className="mt-1 text-[13px] text-muted">Cách kết nối ứng dụng, tạo đề, chấm bài và sử dụng từng tool.</p>
    </div>
    <ButtonLink href="/ai/mcp-guide" size="sm">Xem tài liệu MCP</ButtonLink>
  </section>;
}
