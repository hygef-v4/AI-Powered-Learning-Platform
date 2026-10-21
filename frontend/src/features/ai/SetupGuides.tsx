"use client";

import { Section } from "@/shared/ui";

const KEY_STEPS: { provider: string; steps: string[] }[] = [
  {
    provider: "Gemini",
    steps: ["Mở Google AI Studio (aistudio.google.com), đăng nhập tài khoản Google.", "Chọn Get API key → Create API key.", "Sao chép khóa và dán vào popup Thêm khóa."],
  },
  {
    provider: "OpenAI",
    steps: ["Mở platform.openai.com → API keys.", "Bấm Create new secret key, đặt tên dễ nhớ.", "Nạp tín dụng cho tài khoản nếu chưa có, rồi dán khóa vào popup."],
  },
  {
    provider: "Claude",
    steps: ["Mở console.anthropic.com → API keys.", "Bấm Create Key, chọn workspace.", "Sao chép khóa (chỉ hiện một lần) và dán vào popup."],
  },
];

/** Hướng dẫn lấy khóa API từng nhà cung cấp; các bước có thể đổi theo trang của nhà cung cấp. */
export function ApiSetupGuide() {
  return (
    <Section title="Hướng dẫn lấy khóa API" icon="help_center" tone="info">
      <div className="grid gap-3 md:grid-cols-3">
        {KEY_STEPS.map((guide) => (
          <div key={guide.provider} className="rounded-xl border border-line p-3.5">
            <p className="mb-1.5 text-[13px] font-bold text-ink">{guide.provider}</p>
            <ol className="list-decimal space-y-1 pl-4 text-xs text-body">
              {guide.steps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
          </div>
        ))}
      </div>
      <p className="text-[11px] text-muted">Không chia sẻ khóa cho người khác. Nền tảng chỉ lưu bản mã hóa và không hiện lại khóa.</p>
    </Section>
  );
}

/** Đoạn cấu hình MCP mẫu: URL /mcp trên địa chỉ hiện tại, token đặt trong header Authorization. */
export function mcpConfigSample(endpoint: string, token = "<token của bạn>") {
  return JSON.stringify(
    { mcpServers: { "nen-tang-hoc-tap": { type: "http", url: endpoint, headers: { Authorization: `Bearer ${token}` } } } },
    null,
    2,
  );
}

export function mcpEndpoint(): string {
  return typeof window === "undefined" ? "/mcp" : `${window.location.origin}/mcp`;
}

/** Các bước cài agent; chưa ứng dụng agent nào được nhóm kiểm chứng nên ghi rõ. */
export function McpSetupGuide() {
  return (
    <Section title="Hướng dẫn cài agent" icon="extension" tone="accent">
      <ol className="list-decimal space-y-1 pl-4 text-xs text-body">
        <li>Bấm Kết nối agent mới, chọn quyền và hạn dùng, sao chép token (chỉ hiện một lần).</li>
        <li>Mở cấu hình MCP của ứng dụng agent (Streamable HTTP) và dán đoạn dưới, thay token.</li>
        <li>Khởi động lại agent; agent gọi công cụ của nền tảng theo đúng quyền tài khoản của bạn.</li>
      </ol>
      <pre className="overflow-x-auto rounded-xl bg-shell p-3 font-mono text-[11.5px] text-ink">{mcpConfigSample(mcpEndpoint())}</pre>
      <p className="text-[11px] text-muted">
        Chưa kiểm chứng với ứng dụng agent cụ thể; tên trường cấu hình có thể khác giữa các ứng dụng. Agent dùng mô hình của
        chính nó, nền tảng không gọi AI thay agent.
      </p>
    </Section>
  );
}
