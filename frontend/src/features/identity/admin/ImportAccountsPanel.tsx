"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { commitImport, validateImport, type ImportResult } from "./api";
import { IMPORT_REASON_LABELS } from "./labels";

const TEMPLATE = "email,display_name,role\nmasv@fpt.edu.vn,Nguyễn Văn A,STUDENT\n";

/**
 * Nhập CSV (luồng phụ UC 09, BR-U01-80–85): chọn tệp → kiểm từng dòng (không ghi) → xác nhận gửi lại cùng tệp, backend
 * kiểm lại và chỉ tạo dòng hợp lệ. Không có email nào được gửi.
 */
export function ImportAccountsPanel({ onImported, onClose }: { onImported: () => void; onClose: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportResult | null>(null);
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

  async function run(action: (file: File) => Promise<ImportResult>) {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const result = await action(file);
      setPreview(result);
      if (result.committed) onImported();
    } catch (e) {
      setError(e instanceof ApiError && e.problem.status === 400 ? (e.problem.detail ?? "Tệp không hợp lệ.") : "Không xử lý được tệp, vui lòng thử lại.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section aria-label="Nhập CSV" className="mt-4 rounded-xl border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-slate-900">Nhập tài khoản từ CSV</h2>
        <button type="button" onClick={onClose} aria-label="Đóng" className="text-slate-400 hover:text-slate-600">
          ✕
        </button>
      </div>
      <p className="mt-1 text-xs text-slate-500">
        Tối đa 1000 dòng, cột bắt buộc email, display_name, role. Không nhập được Quản trị viên.{" "}
        <button type="button" onClick={downloadTemplate} className="text-[#1f2a6b] underline">
          Tải tệp mẫu
        </button>
      </p>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        <input
          type="file"
          accept=".csv,text/csv"
          aria-label="Chọn tệp CSV"
          onChange={(e) => {
            setFile(e.target.files?.[0] ?? null);
            setPreview(null);
          }}
          className="text-sm"
        />
        <button
          type="button"
          disabled={!file || busy}
          onClick={() => run(validateImport)}
          className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-50"
        >
          Kiểm tra tệp
        </button>
      </div>
      {error && (
        <p role="alert" className="mt-3 text-sm text-red-600">
          {error}
        </p>
      )}
      {preview && (
        <div className="mt-4">
          <p className="text-sm text-slate-700" role="status">
            {preview.committed
              ? `Đã tạo ${preview.validCount} tài khoản chờ kích hoạt, bỏ qua ${preview.rejectedCount} dòng lỗi. Không có email nào được gửi.`
              : `${preview.validCount} dòng hợp lệ, ${preview.rejectedCount} dòng lỗi.`}
          </p>
          <div className="mt-2 max-h-72 overflow-auto rounded-lg border border-slate-200">
            <table className="w-full text-left text-xs">
              <thead className="sticky top-0 bg-slate-50 text-slate-500">
                <tr>
                  <th className="px-3 py-2">Dòng</th>
                  <th className="px-3 py-2">Email</th>
                  <th className="px-3 py-2">Họ tên</th>
                  <th className="px-3 py-2">Vai trò</th>
                  <th className="px-3 py-2">Kết quả</th>
                </tr>
              </thead>
              <tbody>
                {preview.rows.map((row) => (
                  <tr key={row.rowNumber} className="border-t border-slate-100">
                    <td className="px-3 py-1.5">{row.rowNumber}</td>
                    <td className="px-3 py-1.5">{row.email}</td>
                    <td className="px-3 py-1.5">{row.displayName}</td>
                    <td className="px-3 py-1.5">{row.role}</td>
                    <td className={`px-3 py-1.5 ${row.reason ? "text-red-600" : "text-emerald-700"}`}>
                      {row.reason ? (IMPORT_REASON_LABELS[row.reason] ?? row.reason) : row.outcome === "CREATED" ? "Đã tạo" : "Hợp lệ"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!preview.committed && preview.validCount > 0 && (
            <button
              type="button"
              disabled={busy}
              onClick={() => run(commitImport)}
              className="mt-3 rounded-lg bg-[#1f2a6b] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
            >
              {`Xác nhận tạo ${preview.validCount} tài khoản`}
            </button>
          )}
        </div>
      )}
    </section>
  );
}
