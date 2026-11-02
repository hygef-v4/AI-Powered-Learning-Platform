import type { AiProvider } from "@/shared/ai";
import { apiFetch } from "@/shared/api/client";
import type { ConnectionView, SaveConnection } from "../types";

const BASE = "/api/v1/admin/ai";

export type QuotaPolicy = {
  defaultDayTokens: number | null;
  globalDayTokens: number | null;
  version: number;
  updatedAt: string | null;
  configured: boolean;
};

export type TeacherQuota = { accountId: string; name: string; email: string; dayTokens: number; updatedAt: string };
export type TeacherCandidate = { accountId: string; name: string; email: string };

export type UsageSummary = {
  days: number;
  todayTokens: number;
  globalDayTokens: number | null;
  periodTokens: number;
  teachersUsed: number;
  byDay: { day: string; tokens: number }[];
  byTeacher: { accountId: string; name: string; email: string; periodTokens: number; todayTokens: number; dayLimit: number | null; requests: number }[];
  byModel: { provider: string; model: string; requests: number; tokens: number; todayTokens: number }[];
};

export const getInstitutional = () => apiFetch<ConnectionView[]>(`${BASE}/connections`);

/** Thêm một khóa của trường; một nhà cung cấp có thể có nhiều khóa. */
export const addInstitutional = (provider: AiProvider, body: SaveConnection) =>
  apiFetch<ConnectionView>(`${BASE}/connections`, { method: "POST", body: JSON.stringify({ provider, ...body }) });

/** Kiểm tra trong popup trước khi lưu: khóa vừa nhập, hoặc khóa đã lưu của {@code id} khi để trống ô khóa. */
export const testInstitutionalDraft = (provider: AiProvider, body: SaveConnection, id?: string | null) =>
  apiFetch<{ ok: boolean; message: string }>(`${BASE}/connections:test`, {
    method: "POST",
    body: JSON.stringify({ provider, apiKey: body.apiKey, model: body.model, id: id ?? null }),
  });

export const saveInstitutional = (id: string, body: SaveConnection) =>
  apiFetch<ConnectionView>(`${BASE}/connections/${encodeURIComponent(id)}`, { method: "PUT", body: JSON.stringify(body) });

/** Khóa rõ đã lưu, cho Admin xem lại hoặc sao chép trong popup Sửa (mỗi lần xem ghi audit). */
export const revealInstitutional = (id: string) =>
  apiFetch<{ apiKey: string }>(`${BASE}/connections/${encodeURIComponent(id)}/key`);

export const testInstitutional = (id: string) =>
  apiFetch<ConnectionView>(`${BASE}/connections/${encodeURIComponent(id)}:test`, { method: "POST" });

export const setInstitutionalEnabled = (id: string, enabled: boolean, version: number) =>
  apiFetch<ConnectionView>(`${BASE}/connections/${encodeURIComponent(id)}/status`, {
    method: "PUT",
    body: JSON.stringify({ enabled, version }),
  });

export const revokeInstitutional = (id: string) =>
  apiFetch<void>(`${BASE}/connections/${encodeURIComponent(id)}`, { method: "DELETE" });

export const getPolicy = () => apiFetch<QuotaPolicy>(`${BASE}/quota-policy`);

export const savePolicy = (defaultDayTokens: number, globalDayTokens: number, version: number) =>
  apiFetch<QuotaPolicy>(`${BASE}/quota-policy`, {
    method: "PUT",
    body: JSON.stringify({ defaultDayTokens, globalDayTokens, version }),
  });

export const getTeacherQuotas = () => apiFetch<TeacherQuota[]>(`${BASE}/teacher-quotas`);

export const setTeacherQuota = (accountId: string, dayTokens: number) =>
  apiFetch<void>(`${BASE}/teacher-quotas/${encodeURIComponent(accountId)}`, {
    method: "PUT",
    body: JSON.stringify({ dayTokens }),
  });

export const deleteTeacherQuota = (accountId: string) =>
  apiFetch<void>(`${BASE}/teacher-quotas/${encodeURIComponent(accountId)}`, { method: "DELETE" });

export const findTeachers = (q: string) =>
  apiFetch<TeacherCandidate[]>(`${BASE}/teacher-candidates?q=${encodeURIComponent(q)}`);

export const getUsageSummary = (days: 7 | 30) => apiFetch<UsageSummary>(`${BASE}/usage-summary?days=${days}`);
