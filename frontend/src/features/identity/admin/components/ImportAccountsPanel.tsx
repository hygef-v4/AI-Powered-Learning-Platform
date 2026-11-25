"use client";

import { useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import type { Role } from "@/shared/auth/roles";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { Pill } from "@/shared/ui/Pill";
import { Section } from "@/shared/ui/Section";
import { TableFrame, Td, Th } from "@/shared/ui/Table";
import { ROLE_LABELS } from "../../model/types";
import { commitImport, validateImport, type ImportResult } from "../api";
import { IMPORT_REASON_LABELS } from "../model/labels";
import { Pagination } from "@/shared/ui";
import { msg } from "@/i18n/msg";

const ROWS_PER_PAGE = 10;

/** Lý do lỗi viết hoa chữ cái đầu. */
function formatSentence(text: string) {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/**
 * Nhập Excel (luồng phụ UC 09): chọn tệp → kiểm từng dòng (không ghi) → xác nhận gửi lại cùng
 * tệp, backend kiểm lại và chỉ tạo dòng hợp lệ. Không có email nào được gửi. Nhập xong thì xóa tệp, bảng và đóng thẻ.
 * Cột Dòng đánh số từ 1 cho dòng dữ liệu đầu tiên (dòng tiêu đề của tệp không tính).
 */
export function ImportAccountsPanel({ onImported, onClose }: { onImported: (created: number) => void; onClose: () => void }) {
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportResult | null>(null);
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function runImport(action: (file: File) => Promise<ImportResult>, target: File) {
    setBusy(true);
    setError(null);
    try {
      const result = await action(target);
      if (result.committed) {
        setFile(null);
        setPreview(null);
        if (input.current) input.current.value = "";
        onImported(result.validCount);
        onClose();
        return;
      }
      setPreview(result);
      setPage(0);
    } catch (e) {
      setError(
        e instanceof ApiError && e.problem.status === 400
          ? (e.problem.detail ?? msg("identity.admin.components.ImportAccountsPanel.invalidFile"))
          : msg("identity.admin.components.ImportAccountsPanel.couldNotProcessTheFile"),
      );
    } finally {
      setBusy(false);
    }
  }

  function choose(next: File | null) {
    setFile(next);
    setPreview(null);
    if (next) void runImport(validateImport, next);
  }

  const rows = preview?.rows.slice(page * ROWS_PER_PAGE, (page + 1) * ROWS_PER_PAGE) ?? [];

  return (
    <Section
      title={msg("identity.admin.components.ImportAccountsPanel.importAccountsFromExcel")}
      icon="upload_file"
      actions={
        <button type="button" onClick={onClose} aria-label={msg("identity.admin.components.ImportAccountsPanel.close")} className="text-muted hover:text-ink">
          <Icon name="close" />
        </button>
      }
    >
      <p className="text-xs text-muted">{msg("identity.admin.components.ImportAccountsPanel.columnsEmailDisplayNameRole")}</p>
      <input
        ref={input}
        type="file"
        accept=".xlsx"
        aria-label={msg("identity.admin.components.ImportAccountsPanel.chooseAnExcelFile")}
        disabled={busy}
        className="sr-only"
        onChange={(e) => choose(e.target.files?.[0] ?? null)}
      />
      <div className="flex flex-wrap items-center gap-3">
        {file && (
          <div className="flex min-w-0 items-center gap-2.5 rounded-xl border border-line bg-shell px-3 py-2">
            <Icon name="description" size={20} className="text-primary-ink" />
            <span className="min-w-0 truncate text-[13px] font-semibold text-ink">{file.name}</span>
          </div>
        )}
        <Button icon="folder_open" onClick={() => input.current?.click()} disabled={busy}>
          {file ? msg("identity.admin.components.ImportAccountsPanel.chooseAnotherFile") : msg("identity.admin.components.ImportAccountsPanel.chooseFile")}
        </Button>
        <a
          href="/templates/account-import.xlsx"
          download="mau-tai-khoan.xlsx"
          className="inline-flex items-center gap-1 text-xs font-semibold text-primary-ink hover:underline"
        >
          <Icon name="download" size={16} />
          {msg("identity.admin.components.ImportAccountsPanel.downloadTemplate")}</a>
      </div>

      {busy && !preview && <p className="text-xs text-muted">{msg("identity.admin.components.ImportAccountsPanel.checkingFile")}</p>}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}

      {preview && (
        <>
          <div className="flex flex-wrap items-center gap-2" role="status">
            <Pill>{preview.rows.length} {msg("identity.admin.components.ImportAccountsPanel.rows")}</Pill>
            <Pill tone="success">
              {preview.validCount} {preview.committed ? msg("identity.admin.components.ImportAccountsPanel.created") : msg("identity.admin.components.ImportAccountsPanel.valid")}
            </Pill>
            {preview.rejectedCount > 0 && <Pill tone="danger">{preview.rejectedCount} {msg("identity.admin.components.ImportAccountsPanel.errors")}</Pill>}
          </div>
          <TableFrame filledHead className="[&_table]:min-w-[820px] [&_table]:table-fixed">
            <colgroup><col className="w-[72px]" /><col className="w-[28%]" /><col /><col className="w-[120px]" /><col className="w-[110px]" /><col className="w-[200px]" /></colgroup><thead>
              <tr>
                <Th className="w-16">{msg("identity.admin.components.ImportAccountsPanel.row")}</Th>
                <Th>Email</Th>
                <Th>{msg("identity.admin.components.ImportAccountsPanel.fullName")}</Th>
                <Th>{msg("identity.admin.components.ImportAccountsPanel.role")}</Th>
                <Th>{msg("identity.admin.components.ImportAccountsPanel.studentCode")}</Th>
                <Th>{msg("identity.admin.components.ImportAccountsPanel.checkResults")}</Th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.rowNumber}>
                  <Td className="text-muted">{row.rowNumber - 1}</Td>
                  <Td>{row.email}</Td>
                  <Td>{row.displayName}</Td>
                  <Td>{ROLE_LABELS[row.role as Role] ?? row.role}</Td>
                  <Td>{row.studentCode}</Td>
                  <Td>
                    {row.reason ? (
                      <Pill tone="danger">{formatSentence(IMPORT_REASON_LABELS[row.reason] ?? row.reason)}</Pill>
                    ) : (
                      <Pill tone="success">{row.outcome === "CREATED" ? msg("identity.admin.components.ImportAccountsPanel.created2") : msg("identity.admin.components.ImportAccountsPanel.valid2")}</Pill>
                    )}
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
          {preview.rows.length > ROWS_PER_PAGE && (
            <Pagination page={page} pageSize={ROWS_PER_PAGE} total={preview.rows.length} itemLabel={msg("identity.admin.components.ImportAccountsPanel.rows2")} onPageChange={setPage} summary={false} />
          )}
          {!preview.committed && (
            <>
              <div className="flex justify-end gap-2 border-t border-line pt-3.5">
                <Button onClick={onClose} disabled={busy}>
                  {msg("identity.admin.components.ImportAccountsPanel.cancel")}</Button>
                <Button
                  variant="primary"
                  icon="check"
                  disabled={busy || preview.validCount === 0 || !file}
                  onClick={() => file && runImport(commitImport, file)}
                >
                  {msg("identity.admin.components.ImportAccountsPanel.importAccounts", { validCount: String(preview.validCount) })}
                </Button>
              </div>
            </>
          )}
        </>
      )}
    </Section>
  );
}
