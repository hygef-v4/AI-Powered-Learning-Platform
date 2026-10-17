"use client";

import { useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { changePassword } from "../api";
import { PasswordChecklist } from "../components/PasswordChecklist";
import { PasswordField } from "../components/PasswordField";
import { PASSWORD_VIOLATION_MESSAGES, passwordAcceptable } from "../passwordRules";

export const PASSWORD_CHANGED_MESSAGE = "Đã đổi mật khẩu. Các thiết bị khác đã bị đăng xuất.";

/** Popup Password Change (UC 05): mật khẩu hiện tại, mật khẩu mới và nhập lại (BR-U01-33). */
export function PasswordChangeDialog({ onClose, onChanged }: { onClose: () => void; onChanged: () => void }) {
  const [current, setCurrent] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const acceptable = passwordAcceptable(password);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (current.length === 0) {
      setError("Nhập mật khẩu hiện tại.");
      return;
    }
    if (!acceptable) {
      setError("Mật khẩu mới cần ít nhất 8 ký tự, có cả chữ và số.");
      return;
    }
    if (password !== confirm) {
      setError("Hai mật khẩu mới chưa khớp.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await changePassword(current, password);
      onChanged();
    } catch (e) {
      const problem = (e instanceof ApiError ? e.problem : { status: 0 }) as {
        status: number;
        code?: string;
        violations?: string[];
      };
      if (problem.code === "CURRENT_PASSWORD_INVALID") {
        setError("Mật khẩu hiện tại không đúng.");
        setCurrent("");
      } else if (problem.code === "PASSWORD_POLICY") {
        setError((problem.violations ?? []).map((v) => PASSWORD_VIOLATION_MESSAGES[v] ?? v).join(" "));
      } else {
        setError("Hệ thống tạm bận, vui lòng thử lại sau.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <form
        role="dialog"
        aria-modal="true"
        aria-labelledby="password-change-title"
        onSubmit={submit}
        noValidate
        className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl"
      >
        <div className="flex items-start justify-between">
          <h2 id="password-change-title" className="text-lg font-semibold text-slate-900">
            Đổi mật khẩu
          </h2>
          <button type="button" onClick={onClose} aria-label="Đóng" className="text-slate-400 hover:text-slate-600">
            ✕
          </button>
        </div>
        <PasswordField
          className="mt-4"
          label="Mật khẩu hiện tại"
          autoComplete="current-password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
        />
        <PasswordField
          className="mt-4"
          label="Mật khẩu mới"
          autoComplete="new-password"
          value={password}
          valid={acceptable}
          onChange={(e) => setPassword(e.target.value)}
        />
        <PasswordField
          className="mt-4"
          label="Nhập lại mật khẩu mới"
          autoComplete="new-password"
          value={confirm}
          valid={confirm.length > 0 && confirm === password}
          onChange={(e) => setConfirm(e.target.value)}
        />
        <PasswordChecklist password={password} />
        <p className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
          Sau khi đổi, các thiết bị khác sẽ bị đăng xuất; thiết bị này vẫn giữ đăng nhập.
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
            Đổi mật khẩu
          </button>
        </div>
      </form>
    </div>
  );
}
