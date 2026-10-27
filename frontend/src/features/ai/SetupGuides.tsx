"use client";

import type { ReactNode } from "react";
import { Section, SettingsGroup } from "@/shared/ui";

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
    <Section title="Hướng dẫn lấy khóa API" icon="help_center" tone="info">
      <div className="grid gap-3 md:grid-cols-3">
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
      <p className="text-[11px] text-muted">
        Không chia sẻ khóa cho người khác. Nền tảng chỉ lưu bản mã hóa và không
        hiện lại khóa.
      </p>
    </Section>
  );
}

/** Đoạn cấu hình MCP mẫu: URL /mcp trên địa chỉ hiện tại, token đặt trong header Authorization; thụt 4 dấu cách. */
export function mcpConfigSample(endpoint: string, token = "<token của bạn>") {
  return JSON.stringify(
    {
      mcpServers: {
        "nen-tang-hoc-tap": {
          type: "http",
          url: endpoint,
          headers: { Authorization: `Bearer ${token}` },
        },
      },
    },
    null,
    4,
  );
}

export function mcpEndpoint(): string {
  return typeof window === "undefined"
    ? "/mcp"
    : `${window.location.origin}/mcp`;
}

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

/** Các bước cài agent và đoạn cấu hình mẫu (tô màu như code). */
export function McpSetupGuide() {
  return (
    <SettingsGroup title="Hướng dẫn cài agent">
      <ol className="list-decimal space-y-1 pl-4 text-[13px] text-body">
        <li>Bấm Kết nối agent mới, chọn quyền và hạn dùng, sao chép token.</li>
        <li>
          Mở cấu hình MCP của ứng dụng agent (Streamable HTTP) và dán đoạn dưới,
          thay token.
        </li>
        <li>Khởi động lại agent.</li>
      </ol>
      <div className="mt-3">
        <JsonCode text={mcpConfigSample(mcpEndpoint())} />
      </div>
    </SettingsGroup>
  );
}
