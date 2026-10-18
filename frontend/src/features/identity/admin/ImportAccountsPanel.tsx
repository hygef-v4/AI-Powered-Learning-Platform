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
import { ROLE_LABELS } from "../types";
import { commitImport, validateImport, type ImportResult } from "./api";
import { IMPORT_REASON_LABELS } from "./labels";
import { Pagination } from "./Pagination";

const TEMPLATE = "email,display_name,role\nmasv@fpt.edu.vn,Nguyễn Văn A,STUDENT\n";
const ROWS_PER_PAGE = 10;

function formatSize(bytes: number) {
  return bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1)} KB`;
}

/**
 * Nhập CSV (luồng phụ UC 09, BR-U01-80–85; design `a02b`): chọn tệp → kiểm từng dòng (không ghi) → xác nhận gửi lại cùng
 * tệp, backend kiểm lại và chỉ tạo dòng hợp lệ. Không có email nào được gửi.
 */
export function ImportAccountsPanel({ onImported, onClose }: { onImported: (created: number) => void; onClose: () => void }) {
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportResult | null>(null);
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function downloadTemplate() {
    const url = URL.createObjectURL(new Blob([TEMPLATE], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "mau-tai-khoan.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  async function run(action: (file: File) => Promise<ImportResult>, target: File) {
    setBusy(true);
    setError(null);
    try {
      const result = await action(target);
      setPreview(result);
      setPage(0);
      if (result.committed) onImported(result.validCount);
    } catch (e) {
      setError(
        e instanceof ApiError && e.problem.status === 400
          ? (e.problem.detail ?? "Tệp không hợp lệ.")
          : "Không xử lý được tệp, vui lòng thử lại.",
      );
    } finally {
      setBusy(false);
    }
  }

  function choose(next: File | null) {
    setFile(next);
    setPreview(null);
    if (next) void run(validateImport, next);
  }

  const rows = preview?.rows.slice(page * ROWS_PER_PAGE, (page + 1) * ROWS_PER_PAGE) ?? [];

  return (
    <Section
      title="Nhập tài khoản từ CSV"
      description="Tệp .csv, mã UTF-8, dòng đầu là tiêu đề cột"
      icon="upload_file"
      actions={
        <button type="button" onClick={onClose} aria-label="Đóng" className="text-muted hover:text-ink">
          <Icon name="close" />
        </button>
      }
    >
      <input
        ref={input}
        type="file"
        accept=".csv,text/csv"
        aria-label="Chọn tệp CSV"
        className="sr-only"
        onChange={(e) => choose(e.target.files?.[0] ?? null)}
      />
      <div className="flex flex-wrap items-center gap-3">
        {file && (
          <div className="flex min-w-0 items-center gap-2.5 rounded-[11px] border border-line bg-shell px-3 py-2">
            <Icon name="description" size={20} className="text-primary" />
            <span className="min-w-0">
              <span className="block truncate text-[13px] font-semibold text-ink">{file.name}</span>
              <span className="block text-[11px] text-muted">{formatSize(file.size)}</span>
            </span>
          </div>
        )}
        <Button icon="folder_open" onClick={() => input.current?.click()} disabled={busy}>
          {file ? "Chọn tệp khác" : "Chọn tệp"}
        </Button>
        <button
          type="button"
          onClick={downloadTemplate}
          className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
        >
          <Icon name="download" size={16} />
          Tải tệp mẫu (email, họ tên, vai trò)
        </button>
      </div>

      {busy && !preview && <p className="text-xs text-muted">Đang kiểm tra tệp...</p>}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}

      {preview && (
        <>
          <div className="flex flex-wrap items-center gap-2" role="status">
            <Pill>{preview.rows.length} dòng</Pill>
            <Pill tone="success">
              {preview.validCount} {preview.committed ? "đã tạo" : "hợp lệ"}
            </Pill>
            {preview.rejectedCount > 0 && <Pill tone="danger">{preview.rejectedCount} lỗi</Pill>}
            <span className="text-xs text-muted">
              {preview.committed
                ? `Đã tạo ${preview.validCount} tài khoản, bỏ qua ${preview.rejectedCount} dòng lỗi.`
                : "Chỉ nhập các dòng hợp lệ; dòng lỗi được bỏ qua."}
            </span>
          </div>
          <TableFrame>
            <thead>
              <tr>
                <Th className="w-16">Dòng</Th>
                <Th>Email</Th>
                <Th>Họ tên</Th>
                <Th>Vai trò</Th>
                <Th>Kết quả kiểm tra</Th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.rowNumber}>
                  <Td className="text-muted">{row.rowNumber}</Td>
                  <Td>{row.email}</Td>
                  <Td>{row.displayName}</Td>
                  <Td>{ROLE_LABELS[row.role as Role] ?? row.role}</Td>
                  <Td>
                    {row.reason ? (
                      <Pill tone="danger">Lỗi: {IMPORT_REASON_LABELS[row.reason] ?? row.reason}</Pill>
                    ) : (
                      <Pill tone="success">{row.outcome === "CREATED" ? "Đã tạo" : "Hợp lệ"}</Pill>
                    )}
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
          {preview.rows.length > ROWS_PER_PAGE && (
            <Pagination page={page} size={ROWS_PER_PAGE} total={preview.rows.length} unit="dòng" onPage={setPage} />
          )}
          {!preview.committed && (
            <>
              <p className="text-xs text-muted">
                Tài khoản mới ở trạng thái Chờ kích hoạt. Hệ thống <b className="text-body">không gửi email</b>; người dùng tự
                kích hoạt bằng email trường.
              </p>
              <div className="flex justify-end gap-2 border-t border-line pt-3.5">
                <Button onClick={onClose} disabled={busy}>
                  Hủy
                </Button>
                <Button
                  variant="primary"
                  icon="check"
                  disabled={busy || preview.validCount === 0 || !file}
                  onClick={() => file && run(commitImport, file)}
                >
                  {`Xác nhận nhập ${preview.validCount} tài khoản`}
                </Button>
              </div>
            </>
          )}
        </>
      )}
    </Section>
  );
}
