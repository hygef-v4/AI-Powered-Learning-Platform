"use client";

import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import type { Role } from "@/shared/auth/roles";
import { ROLE_LABELS } from "../types";
import { createAccount } from "./api";
import { ADMIN_ERROR_LABELS } from "./labels";

const EMAIL_FORMAT = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const ROLES: Role[] = ["STUDENT", "TEACHER", "SUBJECT_MANAGER", "ADMIN"];

/**
 * Popup New Account (UC 09): email trường, họ tên, vai trò. Tài khoản ở trạng thái chờ kích hoạt, không mật khẩu, không
 * gửi email (BR-U01-10); người dùng tự kích hoạt ở lần đầu.
 */
export function NewAccountDialog({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const emailId = useId();
  const nameId = useId();
  const roleId = useId();
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [role, setRole] = useState<Role>("STUDENT");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!EMAIL_FORMAT.test(email.trim())) {
      setError("Email không hợp lệ.");
      return;
    }
    if (displayName.trim().length === 0) {
      setError("Nhập họ và tên.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await createAccount(email.trim(), displayName.trim(), role);
      onCreated();
    } catch (e) {
      const code = e instanceof ApiError ? e.problem.code : undefined;
      setError((code && ADMIN_ERROR_LABELS[code]) ?? "Không tạo được tài khoản, vui lòng thử lại.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <form
        role="dialog"
        aria-modal="true"
        aria-label="Thêm tài khoản"
        onSubmit={submit}
        noValidate
        className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl"
      >
        <h2 className="text-lg font-semibold text-slate-900">Thêm tài khoản</h2>
        <label htmlFor={emailId} className="mt-4 block text-xs font-semibold text-slate-700">
          Email trường
        </label>
        <input
          id={emailId}
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="masv@fpt.edu.vn"
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
        />
        <label htmlFor={nameId} className="mt-3 block text-xs font-semibold text-slate-700">
          Họ và tên
        </label>
        <input
          id={nameId}
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
        />
        <label htmlFor={roleId} className="mt-3 block text-xs font-semibold text-slate-700">
          Vai trò
        </label>
        <select
          id={roleId}
          value={role}
          onChange={(e) => setRole(e.target.value as Role)}
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
        >
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {ROLE_LABELS[r]}
            </option>
          ))}
        </select>
        <p className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
          Tài khoản ở trạng thái chờ kích hoạt. Người dùng tự kích hoạt ở lần đăng nhập đầu. Không có email nào được gửi.
        </p>
        {error && (
          <p role="alert" className="mt-3 text-sm text-red-600">
            {error}
          </p>
        )}
        <div className="mt-6 flex justify-end gap-2">
          <button type="button" onClick={onClose} className="rounded-lg px-4 py-2 text-sm text-slate-700 hover:bg-slate-100">
            Hủy
          </button>
          <button
            type="submit"
            disabled={busy}
            className="rounded-lg bg-[#1f2a6b] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            Thêm tài khoản
          </button>
        </div>
      </form>
    </div>
  );
}
