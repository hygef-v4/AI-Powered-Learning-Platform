import { apiFetch } from "@/shared/api/client";
import type { SubjectManager } from "./types";

/** Ứng viên Chủ nhiệm môn lấy từ danh sách tài khoản của U01 (UC 08), chỉ role SUBJECT_MANAGER đang ACTIVE. */
export type ManagerCandidate = SubjectManager & { role: "SUBJECT_MANAGER"; status: "ACTIVE" };
export type SearchSubjectManagers = (query: { q: string; limit: 20 }) => Promise<ManagerCandidate[]>;

type AccountItem = { id: string; email: string; displayName: string };

export const searchSubjectManagers: SearchSubjectManagers = async ({ q, limit }) => {
  const params = new URLSearchParams({ role: "SUBJECT_MANAGER", status: "ACTIVE", page: "0", size: String(limit) });
  if (q.trim()) params.set("q", q.trim());
  const page = await apiFetch<{ items: AccountItem[] }>(`/api/v1/admin/accounts?${params}`);
  return page.items.map((account) => ({
    accountId: account.id,
    displayName: account.displayName,
    email: account.email,
    role: "SUBJECT_MANAGER",
    status: "ACTIVE",
  }));
};
