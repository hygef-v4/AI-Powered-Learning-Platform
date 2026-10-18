"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import type { Role } from "@/shared/auth/roles";
import { AVATAR_TONES, initials } from "@/shared/layout/navigation";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { Select, TextInput } from "@/shared/ui/inputs";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Pill } from "@/shared/ui/Pill";
import { ClickableRow, TableFrame, Td, Th } from "@/shared/ui/Table";
import { SOFT_TONES } from "@/shared/ui/tones";
import { ROLE_LABELS, type AccountStatus } from "../types";
import { listAccounts, type AccountPage } from "./api";
import { ImportAccountsPanel } from "./ImportAccountsPanel";
import { formatDate, STATUS_LABELS, STATUS_TONES } from "./labels";
import { NewAccountDialog } from "./NewAccountDialog";
import { Pagination } from "./Pagination";

const ROLE_TABS: { value: Role | ""; label: string }[] = [
  { value: "", label: "Tất cả" },
  { value: "STUDENT", label: ROLE_LABELS.STUDENT },
  { value: "TEACHER", label: ROLE_LABELS.TEACHER },
  { value: "SUBJECT_MANAGER", label: ROLE_LABELS.SUBJECT_MANAGER },
  { value: "ADMIN", label: ROLE_LABELS.ADMIN },
];
const STATUSES: AccountStatus[] = ["ACTIVE", "PENDING", "DISABLED"];

/**
 * Account List (UC 08, design `a02`): tab vai trò, lọc trạng thái, tìm email/tên, phân trang; bộ lọc nằm trên URL để Back
 * giữ trạng thái. Mở New Account (UC 09, `a03`), Nhập CSV (`a02b`) và Account Detail (UC 10, `a04`).
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

  return (
    <>
      <PageHeader
        title="Tài khoản"
        subtitle={data ? `${data.total} tài khoản` : undefined}
        actions={
          <>
            <Button icon="upload_file" onClick={() => setImporting((v) => !v)} aria-expanded={importing}>
              Nhập CSV
            </Button>
            <Button variant="primary" icon="add" onClick={() => setCreating(true)}>
              Thêm tài khoản
            </Button>
          </>
        }
      />

      {notice && (
        <Alert tone="success" role="status">
          {notice}
        </Alert>
      )}
      {importing && (
        <ImportAccountsPanel
          onImported={(count) => {
            setNotice(`Đã tạo ${count} tài khoản ở trạng thái Chờ kích hoạt. Không có email nào được gửi.`);
            setReloadKey((k) => k + 1);
          }}
          onClose={() => setImporting(false)}
        />
      )}

      <section className="flex flex-col gap-3.5 rounded-2xl border border-line bg-white p-5">
        <div className="flex flex-wrap items-center gap-3">
          <div role="tablist" aria-label="Vai trò" className="flex flex-wrap gap-1 rounded-[10px] bg-shell p-1">
            {ROLE_TABS.map((tab) => (
              <button
                key={tab.label}
                type="button"
                role="tab"
                aria-selected={role === tab.value}
                onClick={() => navigate({ role: tab.value, page: 0 })}
                className={cx(
                  "rounded-[7px] px-2.5 py-1.5 text-xs font-semibold transition-colors",
                  role === tab.value ? "bg-white text-ink shadow-sm" : "text-muted hover:text-ink",
                )}
              >
                {tab.label}
              </button>
            ))}
          </div>
          <Select
            aria-label="Trạng thái"
            value={status}
            onChange={(e) => navigate({ status: e.target.value, page: 0 })}
            wrapperClassName="w-44"
          >
            <option value="">Trạng thái: Tất cả</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                Trạng thái: {STATUS_LABELS[s]}
              </option>
            ))}
          </Select>
          <form onSubmit={submitSearch} role="search" className="ml-auto w-full sm:w-64">
            <TextInput
              type="search"
              icon="search"
              aria-label="Lọc theo email hoặc tên"
              placeholder="Lọc theo email hoặc tên"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </form>
        </div>

        {error ? (
          <Alert
            tone="danger"
            role="alert"
            action={
              <Button size="sm" icon="refresh" onClick={() => setReloadKey((k) => k + 1)}>
                Tải lại
              </Button>
            }
          >
            {error}
          </Alert>
        ) : (
          <TableFrame>
            <thead>
              <tr>
                <Th>Họ tên</Th>
                <Th>Email</Th>
                <Th>Vai trò</Th>
                <Th>Trạng thái</Th>
                <Th>Ngày tạo</Th>
                <Th className="w-10">
                  <span className="sr-only">Mở</span>
                </Th>
              </tr>
            </thead>
            <tbody>
              {!data && (
                <tr>
                  <Td colSpan={6} className="py-8 text-center text-muted">
                    Đang tải...
                  </Td>
                </tr>
              )}
              {data?.items.map((account) => (
                <ClickableRow key={account.id} onClick={() => router.push(`/admin/accounts/${account.id}`)}>
                  <Td>
                    <span className="flex items-center gap-2.5">
                      <span
                        aria-hidden
                        className={cx(
                          "flex size-8 shrink-0 items-center justify-center rounded-full text-[11px] font-bold",
                          SOFT_TONES[AVATAR_TONES[account.role]],
                        )}
                      >
                        {initials(account.displayName)}
                      </span>
                      <Link
                        href={`/admin/accounts/${account.id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="font-semibold text-ink hover:text-primary"
                      >
                        {account.displayName}
                      </Link>
                    </span>
                  </Td>
                  <Td className="text-body">{account.email}</Td>
                  <Td>{ROLE_LABELS[account.role]}</Td>
                  <Td>
                    <Pill tone={STATUS_TONES[account.status]}>{STATUS_LABELS[account.status]}</Pill>
                  </Td>
                  <Td className="text-muted">{formatDate(account.createdAt)}</Td>
                  <Td className="text-faint">
                    <Icon name="chevron_right" size={18} />
                  </Td>
                </ClickableRow>
              ))}
              {data && data.items.length === 0 && (
                <tr>
                  <Td colSpan={6} className="py-10 text-center text-muted">
                    Không có tài khoản phù hợp với bộ lọc.
                  </Td>
                </tr>
              )}
            </tbody>
          </TableFrame>
        )}

        {data && data.total > 0 && (
          <Pagination page={page} size={data.size} total={data.total} unit="tài khoản" onPage={(p) => navigate({ page: p })} />
        )}
      </section>

      {creating && (
        <NewAccountDialog
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
            setNotice("Đã thêm tài khoản ở trạng thái Chờ kích hoạt. Người dùng tự kích hoạt bằng email trường.");
            setReloadKey((k) => k + 1);
          }}
        />
      )}
    </>
  );
}
