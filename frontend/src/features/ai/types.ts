import type { AiProvider } from "@/shared/ai";

export type ProviderInfo = { provider: AiProvider; models: string[]; maxOutputTokens: number };

export type ConnectionStatus = "ACTIVE" | "DISABLED" | "REVOKED";

/** Kết nối đã che: chỉ 4 ký tự cuối, không có khóa (P4). */
export type ConnectionView = {
  provider: AiProvider;
  configured: boolean;
  model: string | null;
  keyHint: string | null;
  status: ConnectionStatus | null;
  lastTestAt: string | null;
  lastTestResult: string | null;
  version: number;
  /** Khóa của trường: định danh từng khóa (một nhà cung cấp có thể có nhiều khóa). */
  id?: string | null;
};

export type SaveConnection = { apiKey?: string; model: string; version?: number };

export type McpAccess = "READ_ONLY" | "READ_WRITE";
export type McpStatus = "ACTIVE" | "EXPIRED" | "REVOKED";

export type McpCredential = {
  id: string;
  label: string;
  access: McpAccess;
  tokenHint: string;
  createdAt: string;
  expiresAt: string;
  lastUsedAt: string | null;
  status: McpStatus;
  /** Có bản mã token: chủ agent xem lại được trong popup Sửa. */
  revealable?: boolean;
};

export type McpCreated = { credential: McpCredential; token: string; endpoint: string };

export type UsageSource = "SHARED" | "PERSONAL" | "MCP";
export type Provenance = "PROVIDER_MEASURED" | "ESTIMATED" | "EXTERNALLY_REPORTED" | "UNAVAILABLE";

export type UsageItem = {
  id: string;
  createdAt: string;
  source: UsageSource;
  provider: AiProvider | null;
  model: string | null;
  taskType: string | null;
  toolName: string | null;
  outcome: string;
  inputTokens: number | null;
  outputTokens: number | null;
  cachedTokens: number | null;
  reasoningTokens: number | null;
  provenance: Provenance;
};

export type Totals = { requests: number; tokens: number };

export type UsagePage = {
  items: UsageItem[];
  page: number;
  size: number;
  total: number;
  shared: Totals;
  personal: Totals;
  mcp: Totals;
};

export type UsageQuery = { source?: UsageSource; from?: string; to?: string; page: number; size: number };
