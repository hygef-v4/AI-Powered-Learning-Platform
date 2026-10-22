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
} from "./types";

const CONNECTIONS = "/api/v1/me/ai-connections";
const MCP = "/api/v1/me/mcp-credentials";

export const getProviders = () => apiFetch<ProviderInfo[]>("/api/v1/ai/providers");
export const getSources = () => apiFetch<SourceOption[]>("/api/v1/ai/sources");

export const getConnections = () => apiFetch<ConnectionView[]>(CONNECTIONS);

export const saveConnection = (provider: AiProvider, body: SaveConnection) =>
  apiFetch<ConnectionView>(`${CONNECTIONS}/${provider}`, { method: "PUT", body: JSON.stringify(body) });

export const testConnection = (provider: AiProvider) =>
  apiFetch<ConnectionView>(`${CONNECTIONS}/${provider}:test`, { method: "POST" });

export const revokeConnection = (provider: AiProvider) =>
  apiFetch<void>(`${CONNECTIONS}/${provider}`, { method: "DELETE" });

export const getMcpCredentials = () => apiFetch<McpCredential[]>(MCP);

export const createMcpCredential = (label: string, access: McpAccess, days: number) =>
  apiFetch<McpCreated>(MCP, { method: "POST", body: JSON.stringify({ label, access, days }) });

export const revokeMcpCredential = (id: string) =>
  apiFetch<void>(`${MCP}/${encodeURIComponent(id)}`, { method: "DELETE" });

export function getUsage(query: UsageQuery): Promise<UsagePage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  return apiFetch<UsagePage>(`/api/v1/me/ai-usage?${params.toString()}`);
}
