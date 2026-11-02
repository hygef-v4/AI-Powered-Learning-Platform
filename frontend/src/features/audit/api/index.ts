import { apiFetch } from "@/shared/api/client";
import type { AuditLogPage, AuditQuery } from "../model/types";

const BASE = "/api/v1/admin/audit-logs";

export function getAuditLogs(query: AuditQuery): Promise<AuditLogPage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  return apiFetch<AuditLogPage>(`${BASE}?${params.toString()}`);
}
