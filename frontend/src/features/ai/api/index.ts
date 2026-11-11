import type { AiProvider, SourceOption } from "@/shared/ai";
import { apiFetch } from "@/shared/api/client";
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

export const getProviders = () => apiFetch<ProviderInfo[]>("/api/v1/ai/providers");
export const getSources = () => apiFetch<SourceOption[]>("/api/v1/ai/sources");

export const getConnections = () => apiFetch<ConnectionView[]>(CONNECTIONS);

// Khóa API cá nhân: như khóa của trường (yêu cầu 2026-11-05), nhiều khóa cho một nhà cung cấp, thao tác theo id.
const keyUrl = (id: string) => `${CONNECTIONS}/${encodeURIComponent(id)}`;

export const addConnection = (provider: AiProvider, body: SaveConnection) =>
  apiFetch<ConnectionView>(CONNECTIONS, { method: "POST", body: JSON.stringify({ provider, ...body }) });

export const testConnectionDraft = (provider: AiProvider, body: SaveConnection, id?: string | null) =>
  apiFetch<{ ok: boolean; message: string }>(`${CONNECTIONS}:test`, {
    method: "POST",
    body: JSON.stringify({ provider, apiKey: body.apiKey, model: body.model, id: id ?? null }),
  });

export const saveConnection = (id: string, body: SaveConnection) =>
  apiFetch<ConnectionView>(keyUrl(id), { method: "PUT", body: JSON.stringify(body) });

export const testConnection = (id: string) => apiFetch<ConnectionView>(`${keyUrl(id)}:test`, { method: "POST" });

/** Công tắc khóa cá nhân: tắt vẫn giữ khóa, không dùng khi gọi AI. */
export const setConnectionEnabled = (id: string, enabled: boolean, version: number) =>
  apiFetch<ConnectionView>(`${keyUrl(id)}/status`, { method: "PUT", body: JSON.stringify({ enabled, version }) });

export const revokeConnection = (id: string) => apiFetch<void>(keyUrl(id), { method: "DELETE" });

export const getMcpCredentials = () => apiFetch<McpCredential[]>(MCP);

export const createMcpCredential = (label: string, access: McpAccess, days: number) =>
  apiFetch<McpCreated>(MCP, { method: "POST", body: JSON.stringify({ label, access, days }) });

/** Sửa nhãn và quyền của agent còn hiệu lực; token, hạn dùng giữ nguyên. */
export const updateMcpCredential = (id: string, label: string, access: McpAccess) =>
  apiFetch<McpCredential>(`${MCP}/${encodeURIComponent(id)}`, { method: "PATCH", body: JSON.stringify({ label, access }) });

/** Token rõ của agent còn hiệu lực, chỉ chủ agent xem được. */
export const revealMcpToken = (id: string) => apiFetch<{ token: string }>(`${MCP}/${encodeURIComponent(id)}/token`);

export const revokeMcpCredential = (id: string) =>
  apiFetch<void>(`${MCP}/${encodeURIComponent(id)}`, { method: "DELETE" });

export function getUsage(query: UsageQuery): Promise<UsagePage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  return apiFetch<UsagePage>(`/api/v1/me/ai-usage?${params.toString()}`);
}
