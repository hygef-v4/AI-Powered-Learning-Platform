import { apiFetch } from "@/shared/api/client";
import type { AuditLogPage, AuditQuery } from "./types";

const BASE = "/api/v1/admin/audit-logs";

export function getAuditLogs(query: AuditQuery): Promise<AuditLogPage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  return apiFetch<AuditLogPage>(`${BASE}?${params.toString()}`);
}

type AccountSearchPage = { items: { id: string; email: string }[] };

/**
 * Đổi email người thực hiện thành accountId bằng API danh sách tài khoản của U01 (tham số q).
 * Không có tài khoản trùng đúng email, hoặc API chưa có, thì trả null.
 */
export async function findActorIdByEmail(email: string): Promise<string | null> {
  const wanted = email.trim().toLowerCase();
  try {
    const page = await apiFetch<AccountSearchPage>(`/api/v1/admin/accounts?q=${encodeURIComponent(wanted)}&size=20`);
    return page.items.find((account) => account.email.toLowerCase() === wanted)?.id ?? null;
  } catch {
    return null;
  }
}
