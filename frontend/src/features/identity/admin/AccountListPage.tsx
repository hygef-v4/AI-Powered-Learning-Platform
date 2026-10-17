"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import type { Role } from "@/shared/auth/roles";
import { ROLE_LABELS, type AccountStatus } from "../types";
import { listAccounts, type AccountPage } from "./api";
import { ImportAccountsPanel } from "./ImportAccountsPanel";
import { formatDateTime, STATUS_LABELS, STATUS_TONES } from "./labels";
import { NewAccountDialog } from "./NewAccountDialog";

const ROLE_TABS: { value: Role | ""; label: string }[] = [
  { value: "", label: "Tất cả" },
  { value: "STUDENT", label: ROLE_LABELS.STUDENT },
  { value: "TEACHER", label: ROLE_LABELS.TEACHER },
  { value: "SUBJECT_MANAGER", label: ROLE_LABELS.SUBJECT_MANAGER },
  { value: "ADMIN", label: ROLE_LABELS.ADMIN },
];
const STATUSES: AccountStatus[] = ["PENDING", "ACTIVE", "DISABLED"];

/**
 * Account List (UC 08): tab vai trò, lọc trạng thái, tìm email/tên, phân trang; bộ lọc nằm trên URL để Back giữ trạng
 * thái. Mở New Account (UC 09), Nhập CSV (luồng phụ UC 09) và Account Detail (UC 10).
 */
export function AccountListPage() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const role = (params.get("role") ?? "") as Role | "";
  const status = (params.get("status") ?? "") as AccountStatus | "";
  const q = params.get("q") ?? "";
  const page = Number(params.get("page") ?? "0") || 0;

  const [data, setData] = useState<AccountPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [search, setSearch] = useState(q);
  const [creating, setCreating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    listAccounts({ role: role || undefined, status: status || undefined, q: q || undefined, page })
      .then((result) => {
        if (!active) return;
        setData(result);
        setError(null);
      })
      .catch(() => {
        if (active) setError("Không tải được danh sách tài khoản.");
      });
    return () => {
      active = false;
    };
  }, [role, status, q, page, reloadKey]);

  function navigate(next: Record<string, string | number | undefined>) {
    const query = new URLSearchParams(params.toString());
    for (const [key, value] of Object.entries(next)) {
      if (value === undefined || value === "" || value === 0) query.delete(key);
      else query.set(key, String(value));
    }
    router.replace(`${pathname}?${query}`);
  }

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    navigate({ q: search.trim(), page: 0 });
  }

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;

  return (
    <main className="mx-auto max-w-6xl p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-900">Quản lý tài khoản</h1>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setImporting((v) => !v)}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            Nhập CSV
          </button>
          <button
            type="button"
            onClick={() => setCreating(true)}
            className="rounded-lg bg-[#1f2a6b] px-4 py-2 text-sm font-semibold text-white"
          >
            Thêm tài khoản
          </button>
        </div>
      </div>

      {notice && (
        <p role="status" className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {notice}
        </p>
      )}
      {importing && (
        <ImportAccountsPanel onImported={() => setReloadKey((k) => k + 1)} onClose={() => setImporting(false)} />
      )}

      <div className="mt-6 flex flex-wrap items-center gap-2 border-b border-slate-200" role="tablist" aria-label="Vai trò">
        {ROLE_TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            role="tab"
            aria-selected={role === tab.value}
            onClick={() => navigate({ role: tab.value, page: 0 })}
            className={`-mb-px border-b-2 px-3 py-2 text-sm ${
              role === tab.value ? "border-[#1f2a6b] font-semibold text-[#1f2a6b]" : "border-transparent text-slate-500"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3">
        <select
          aria-label="Trạng thái"
          value={status}
          onChange={(e) => navigate({ status: e.target.value, page: 0 })}
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
        >
          <option value="">Mọi trạng thái</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
        <form onSubmit={submitSearch} className="flex gap-2">
          <input
            type="search"
            aria-label="Tìm theo email hoặc tên"
            placeholder="Tìm theo email hoặc tên"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-72 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
          />
          <button type="submit" className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm">
            Tìm
          </button>
        </form>
      </div>

      {error && (
        <p role="alert" className="mt-4 text-sm text-red-600">
          {error}
        </p>
      )}

      <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs text-slate-500">
            <tr>
              <th className="px-4 py-3">Họ và tên</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Vai trò</th>
              <th className="px-4 py-3">Trạng thái</th>
              <th className="px-4 py-3">Cập nhật</th>
            </tr>
          </thead>
          <tbody>
            {data?.items.map((account) => (
              <tr key={account.id} className="border-t border-slate-100 hover:bg-slate-50">
                <td className="px-4 py-3">
                  <Link href={`/admin/accounts/${account.id}`} className="font-medium text-[#1f2a6b] hover:underline">
                    {account.displayName}
                  </Link>
                </td>
                <td className="px-4 py-3 text-slate-600">{account.email}</td>
                <td className="px-4 py-3">{ROLE_LABELS[account.role]}</td>
                <td className="px-4 py-3">
                  <span className={`rounded-md px-2 py-0.5 text-xs font-medium ${STATUS_TONES[account.status]}`}>
                    {STATUS_LABELS[account.status]}
                  </span>
                </td>
                <td className="px-4 py-3 text-slate-500">{formatDateTime(account.updatedAt)}</td>
              </tr>
            ))}
            {data && data.items.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-slate-500">
                  Không có tài khoản phù hợp.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && (
        <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
          <span>{data.total} tài khoản</span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => navigate({ page: page - 1 })}
              className="rounded-lg border border-slate-300 px-3 py-1 disabled:opacity-40"
            >
              Trước
            </button>
            <span>
              Trang {page + 1}/{totalPages}
            </span>
            <button
              type="button"
              disabled={page + 1 >= totalPages}
              onClick={() => navigate({ page: page + 1 })}
              className="rounded-lg border border-slate-300 px-3 py-1 disabled:opacity-40"
            >
              Sau
            </button>
          </div>
        </div>
      )}

      {creating && (
        <NewAccountDialog
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
            setNotice("Tài khoản đã tạo ở trạng thái chờ kích hoạt. Người dùng tự kích hoạt ở lần đăng nhập đầu.");
            setReloadKey((k) => k + 1);
          }}
        />
      )}
    </main>
  );
}
