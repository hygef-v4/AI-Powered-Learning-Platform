import { apiFetch } from "@/shared/api/client";
import type { Role } from "@/shared/auth/roles";
import type { AccountStatus } from "../../model/types";

// API quản trị tài khoản (UC 08–12, nhập CSV). Chỉ ADMIN; backend tự kiểm quyền.

export type AccountSummary = {
  id: string;
  email: string;
  displayName: string;
  role: Role;
  status: AccountStatus;
  createdAt: string;
  updatedAt: string;
  /** MSSV, chỉ tài khoản sinh viên; null khi chưa có (BR-U01-102, 104). */
  studentCode?: string | null;
};

export type AccountPage = { items: AccountSummary[]; total: number; page: number; size: number };

export type AccountDetail = AccountSummary & {
  phoneNumber: string | null;
  version: number;
  /** Sinh viên còn PENDING thì Admin sửa được MSSV gõ sai (BR-U01-105). */
  studentCodeEditable?: boolean;
};

export type AccountFilters = { role?: Role; status?: AccountStatus; q?: string; page?: number };

export type ImportRow = {
  rowNumber: number;
  email: string;
  displayName: string;
  role: string;
  outcome: "VALID" | "CREATED" | "REJECTED";
  reason: string | null;
  studentCode?: string | null;
};

export type ImportResult = {
  fileChecksum: string;
  rows: ImportRow[];
  validCount: number;
  rejectedCount: number;
  committed: boolean;
};

const BASE = "/api/v1/admin/accounts";

export function listAccounts(filters: AccountFilters) {
  const params = new URLSearchParams();
  if (filters.role) params.set("role", filters.role);
  if (filters.status) params.set("status", filters.status);
  if (filters.q) params.set("q", filters.q);
  params.set("page", String(filters.page ?? 0));
  params.set("size", "20");
  return apiFetch<AccountPage>(`${BASE}?${params}`);
}

/** `studentCode` bắt buộc với sinh viên, không gửi với vai trò khác (BR-U01-103). */
export function createAccount(email: string, displayName: string, role: Role, studentCode?: string) {
  return apiFetch<AccountDetail>(BASE, {
    method: "POST",
    body: JSON.stringify({ email, displayName, role, ...(studentCode ? { studentCode } : {}) }),
  });
}

export function getAccount(id: string) {
  return apiFetch<AccountDetail>(`${BASE}/${id}`);
}

/** Không truyền `studentCode` là giữ nguyên MSSV (BR-U01-105). */
export function updateAccount(id: string, displayName: string, phoneNumber: string, version: number, studentCode?: string) {
  return apiFetch<AccountDetail>(`${BASE}/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ displayName, phoneNumber, version, ...(studentCode !== undefined ? { studentCode } : {}) }),
  });
}

export function changeRole(id: string, role: Role, version: number) {
  return apiFetch<AccountDetail>(`${BASE}/${id}/role`, { method: "PUT", body: JSON.stringify({ role, version }) });
}

/** ACTIVE là mở lại: backend trả ACTIVE nếu đã có mật khẩu, PENDING nếu chưa (BR-U01-72). */
export function changeStatus(id: string, status: "ACTIVE" | "DISABLED", version: number) {
  return apiFetch<AccountDetail>(`${BASE}/${id}/status`, { method: "PUT", body: JSON.stringify({ status, version }) });
}

function upload(path: string, file: File) {
  const body = new FormData();
  body.append("file", file);
  return apiFetch<ImportResult>(`${BASE}/${path}`, { method: "POST", body });
}

export function validateImport(file: File) {
  return upload("imports:validate", file);
}

export function commitImport(file: File) {
  return upload("imports:commit", file);
}
