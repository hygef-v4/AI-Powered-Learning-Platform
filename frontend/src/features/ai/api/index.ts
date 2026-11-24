import type { AiProvider, SourceOption } from "@/shared/ai";
import { fetchApi } from "@/shared/api/client";
import type {
  ConnectionView,
  McpAccess,
  McpCreated,
  McpCredential,
  ProviderInfo,
  SaveConnection,
  UsagePage,
  UsageQuery,
} from "../model/types";

const CONNECTIONS = "/api/v1/me/ai-connections";
const MCP = "/api/v1/me/mcp-credentials";

export const getProviders = () => fetchApi<ProviderInfo[]>("/api/v1/ai/providers");
export const getSources = () => fetchApi<SourceOption[]>("/api/v1/ai/sources");

export const getConnections = () => fetchApi<ConnectionView[]>(CONNECTIONS);

// Khóa API cá nhân: như khóa của trường (yêu cầu 2026-11-05), nhiều khóa cho một nhà cung cấp, thao tác theo id.
const buildKeyUrl = (id: string) => `${CONNECTIONS}/${encodeURIComponent(id)}`;

export const addConnection = (provider: AiProvider, body: SaveConnection) =>
  fetchApi<ConnectionView>(CONNECTIONS, { method: "POST", body: JSON.stringify({ provider, ...body }) });

export const testConnectionDraft = (provider: AiProvider, body: SaveConnection, id?: string | null) =>
  fetchApi<{ ok: boolean; message: string }>(`${CONNECTIONS}:test`, {
    method: "POST",
    body: JSON.stringify({ provider, apiKey: body.apiKey, model: body.model, id: id ?? null }),
  });

export const saveConnection = (id: string, body: SaveConnection) =>
  fetchApi<ConnectionView>(buildKeyUrl(id), { method: "PUT", body: JSON.stringify(body) });

export const testConnection = (id: string) => fetchApi<ConnectionView>(`${buildKeyUrl(id)}:test`, { method: "POST" });

/** Công tắc khóa cá nhân: tắt vẫn giữ khóa, không dùng khi gọi AI. */
export const setConnectionEnabled = (id: string, enabled: boolean, version: number) =>
  fetchApi<ConnectionView>(`${buildKeyUrl(id)}/status`, { method: "PUT", body: JSON.stringify({ enabled, version }) });

export const revokeConnection = (id: string) => fetchApi<void>(buildKeyUrl(id), { method: "DELETE" });

export const getMcpCredentials = () => fetchApi<McpCredential[]>(MCP);

export const createMcpCredential = (label: string, access: McpAccess, days: number) =>
  fetchApi<McpCreated>(MCP, { method: "POST", body: JSON.stringify({ label, access, days }) });

/** Sửa nhãn và quyền của agent còn hiệu lực; token, hạn dùng giữ nguyên. */
export const updateMcpCredential = (id: string, label: string, access: McpAccess) =>
  fetchApi<McpCredential>(`${MCP}/${encodeURIComponent(id)}`, { method: "PATCH", body: JSON.stringify({ label, access }) });

export const revokeMcpCredential = (id: string) =>
  fetchApi<void>(`${MCP}/${encodeURIComponent(id)}`, { method: "DELETE" });

export function getUsage(query: UsageQuery): Promise<UsagePage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  return fetchApi<UsagePage>(`/api/v1/me/ai-usage?${params.toString()}`);
}
