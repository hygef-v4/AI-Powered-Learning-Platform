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
  byTeacher: { accountId: string; name: string; email: string; periodTokens: number; todayTokens: number; dayLimit: number | null }[];
  byModel: { provider: string; model: string; requests: number; tokens: number }[];
};

export const getInstitutional = () => apiFetch<ConnectionView[]>(`${BASE}/connections`);

export const saveInstitutional = (provider: AiProvider, body: SaveConnection) =>
  apiFetch<ConnectionView>(`${BASE}/connections/${provider}`, { method: "PUT", body: JSON.stringify(body) });

export const testInstitutional = (provider: AiProvider) =>
  apiFetch<ConnectionView>(`${BASE}/connections/${provider}:test`, { method: "POST" });

export const setInstitutionalEnabled = (provider: AiProvider, enabled: boolean, version: number) =>
  apiFetch<ConnectionView>(`${BASE}/connections/${provider}/status`, {
    method: "PUT",
    body: JSON.stringify({ enabled, version }),
  });

export const revokeInstitutional = (provider: AiProvider) =>
  apiFetch<void>(`${BASE}/connections/${provider}`, { method: "DELETE" });

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
