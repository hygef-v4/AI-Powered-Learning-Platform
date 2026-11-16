"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import type { Role } from "@/shared/auth/roles";
import { AVATAR_TONES, extractInitials } from "@/shared/layout/navigation";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { Segmented, Select, TextInput } from "@/shared/ui/Inputs";
import { PageHeader } from "@/shared/ui/PageHeader";
import { showToast } from "@/shared/ui/Toast";
import { Pill } from "@/shared/ui/Pill";
import { ClickableRow, TableFrame, Td, Th } from "@/shared/ui/Table";
import { SOFT_TONES } from "@/shared/ui/tones";
import { ROLE_LABELS, type AccountStatus } from "../../model/types";
import { listAccounts, type AccountPage } from "../api";
import { ImportAccountsPanel } from "../components/ImportAccountsPanel";
import { STATUS_LABELS, STATUS_TONES } from "../model/labels";
import { NewAccountDialog } from "../components/NewAccountDialog";
import { Card, Pagination } from "@/shared/ui";
import { msg } from "@/i18n/msg";

const ROLE_TABS: { value: Role | ""; label: string }[] = [
  { value: "", label: msg("identity.admin.pages.AccountListPage.tatCa") },
  { value: "STUDENT", label: ROLE_LABELS.STUDENT },
  { value: "TEACHER", label: ROLE_LABELS.TEACHER },
  { value: "SUBJECT_MANAGER", label: ROLE_LABELS.SUBJECT_MANAGER },
  { value: "ADMIN", label: ROLE_LABELS.ADMIN },
];
const STATUSES: AccountStatus[] = ["ACTIVE", "PENDING", "DISABLED"];

/**
 * Account List (UC 08, design `a02`): ô tìm email/tên/MSSV ở hàng trên (gõ tới đâu lọc tới đó), tab vai trò, lọc trạng thái bên
 * phải, phân trang; bộ lọc nằm trên URL để Back
 * giữ trạng thái. Mở New Account (UC 09, `a03`), Nhập Excel (`a02b`) và Account Detail (UC 10, `a04`).
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

  useEffect(() => {
    let active = true;
    listAccounts({ role: role || undefined, status: status || undefined, q: q || undefined, page })
      .then((result) => {
        if (!active) return;
        setData(result);
        setError(null);
      })
      .catch(() => {
        if (active) setError(msg("identity.admin.pages.AccountListPage.khongTaiDuocDanhSachTai"));
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

  // Gõ tới đâu lọc tới đó: chờ một nhịp ngắn sau lần gõ cuối rồi mới đổi URL.
  useEffect(() => {
    const next = search.trim();
    if (next === q) return;
    const timer = setTimeout(() => navigate({ q: next, page: 0 }), 300);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  return (
    <>
      <PageHeader
        title={msg("identity.admin.pages.AccountListPage.taiKhoan")}
        actions={
          <>
            <Button icon="upload_file" onClick={() => setImporting((v) => !v)} aria-expanded={importing}>
              {msg("identity.admin.pages.AccountListPage.nhapExcel")}</Button>
            <Button variant="primary" icon="add" onClick={() => setCreating(true)}>
              {msg("identity.admin.pages.AccountListPage.themTaiKhoan")}</Button>
          </>
        }
      />

      {importing && (
        <ImportAccountsPanel
          onImported={(count) => {
            showToast(msg("identity.admin.pages.AccountListPage.daTaoTaiKhoan", { count: String(count) }));
            setReloadKey((k) => k + 1);
          }}
          onClose={() => setImporting(false)}
        />
      )}

      <Card as="section" padding="lg" className="flex flex-col gap-3.5">
        <div role="search">
          <TextInput
            type="search"
            icon="search"
            aria-label={msg("identity.admin.pages.AccountListPage.timTheoEmailTenHoacMssv")}
            placeholder={msg("identity.admin.pages.AccountListPage.timTheoEmailTenHoacMssv")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <Segmented
            as="tab"
            ariaLabel={msg("identity.admin.pages.AccountListPage.vaiTro")}
            value={role}
            onChange={(value) => navigate({ role: value, page: 0 })}
            options={ROLE_TABS}
          />
          <Select
            aria-label={msg("identity.admin.pages.AccountListPage.trangThai")}
            value={status}
            onChange={(e) => navigate({ status: e.target.value, page: 0 })}
            wrapperClassName="ml-auto w-48"
          >
            <option value="">{msg("identity.admin.pages.AccountListPage.trangThaiTatCa")}</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {msg("identity.admin.pages.AccountListPage.trangThai2")}{STATUS_LABELS[s]}
              </option>
            ))}
          </Select>
        </div>

        {error ? (
          <Alert
            tone="danger"
            role="alert"
            action={
              <Button size="sm" icon="refresh" onClick={() => setReloadKey((k) => k + 1)}>
                {msg("identity.admin.pages.AccountListPage.taiLai")}</Button>
            }
          >
            {error}
          </Alert>
        ) : (
          <TableFrame filledHead className="[&_table]:min-w-[720px] [&_table]:table-fixed">
            <colgroup><col className="w-[110px]" /><col /><col className="w-[30%]" /><col className="w-[140px]" /><col className="w-[120px]" /><col className="w-[48px]" /></colgroup>
            <thead>
              <tr>
                <Th>MSSV</Th>
                <Th>{msg("identity.admin.pages.AccountListPage.hoTen")}</Th>
                <Th>Email</Th>
                <Th>{msg("identity.admin.pages.AccountListPage.vaiTro")}</Th>
                <Th>{msg("identity.admin.pages.AccountListPage.trangThai")}</Th>
                <Th className="w-10">
                  <span className="sr-only">{msg("identity.admin.pages.AccountListPage.mo")}</span>
                </Th>
              </tr>
            </thead>
            <tbody>
              {!data && (
                <tr>
                  <Td colSpan={6} className="py-8 text-center text-muted">
                    {msg("identity.admin.pages.AccountListPage.dangTai")}</Td>
                </tr>
              )}
              {data?.items.map((account) => (
                <ClickableRow key={account.id} onClick={() => router.push(`/admin/accounts/${account.id}`)}>
                  <Td className="font-medium text-body" data-testid="account-list-student-code">{account.studentCode}</Td>
                  <Td>
                    <span className="flex min-w-0 items-center gap-2.5">
                      <span
                        aria-hidden
                        className={cx(
                          "flex size-8 shrink-0 items-center justify-center rounded-full text-xs font-bold",
                          SOFT_TONES[AVATAR_TONES[account.role]],
                        )}
                      >
                        {extractInitials(account.displayName)}
                      </span>
                      <Link
                        href={`/admin/accounts/${account.id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="truncate font-semibold text-ink hover:text-primary-ink"
                        title={account.displayName}
                      >
                        {account.displayName}
                      </Link>
                    </span>
                  </Td>
                  <Td className="truncate text-body" title={account.email}>{account.email}</Td>
                  <Td>{ROLE_LABELS[account.role]}</Td>
                  <Td>
                    <Pill tone={STATUS_TONES[account.status]}>{STATUS_LABELS[account.status]}</Pill>
                  </Td>
                  <Td className="text-faint">
                    <Icon name="chevron_right" size={18} />
                  </Td>
                </ClickableRow>
              ))}
              {data && data.items.length === 0 && (
                <tr>
                  <Td colSpan={6} className="py-10 text-center text-muted">
                    {msg("identity.admin.pages.AccountListPage.khongCoTaiKhoanPhuHop")}</Td>
                </tr>
              )}
            </tbody>
          </TableFrame>
        )}

        {data && data.total > 0 && <Pagination page={page} pageSize={data.size} total={data.total} onPageChange={(p) => navigate({ page: p })} summary={false} />}
      </Card>

      {creating && (
        <NewAccountDialog
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
            showToast(msg("identity.admin.pages.AccountListPage.daThemTaiKhoan"));
            setReloadKey((k) => k + 1);
          }}
        />
      )}
    </>
  );
}
