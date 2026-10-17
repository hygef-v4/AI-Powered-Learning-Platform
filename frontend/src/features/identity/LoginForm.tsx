"use client";

import Link from "next/link";
import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { login } from "./api";
import { PasswordField } from "./components/PasswordField";
import type { SessionInfo } from "./types";

/** Một câu cho mọi thất bại: không phân biệt sai mật khẩu, khóa tạm, chưa kích hoạt hay không tồn tại (BR-U01-40, Q05). */
export const LOGIN_FAILED_MESSAGE = "Email hoặc mật khẩu không đúng, hoặc tài khoản chưa sẵn sàng.";
export const SYSTEM_BUSY_MESSAGE = "Hệ thống tạm bận, vui lòng thử lại sau.";

const EMAIL_FORMAT = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Form User Login (UC 02). */
export function LoginForm({ onSuccess }: { onSuccess: (session: SessionInfo) => void }) {
  const emailId = useId();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!EMAIL_FORMAT.test(email.trim())) {
      setError("Nhập email trường hợp lệ.");
      return;
    }
    if (password.length === 0) {
      setError("Nhập mật khẩu.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      onSuccess(await login(email.trim(), password));
    } catch (e) {
      const status = e instanceof ApiError ? e.problem.status : 0;
      setError(status === 503 ? SYSTEM_BUSY_MESSAGE : status >= 400 && status < 500 ? LOGIN_FAILED_MESSAGE : SYSTEM_BUSY_MESSAGE);
      setPassword("");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      <h1 className="text-2xl font-bold text-slate-900">Đăng nhập</h1>
      <p className="mt-1 text-sm text-slate-500">Đăng nhập bằng email trường.</p>

      <div className="mt-6">
        <label htmlFor={emailId} className="text-[11px] font-semibold tracking-wide text-slate-700 uppercase">
          Email
        </label>
        <input
          id={emailId}
          type="email"
          autoComplete="username"
          placeholder="masv@fpt.edu.vn"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
        />
      </div>
      <PasswordField
        className="mt-4"
        label="Mật khẩu"
        autoComplete="current-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      {error && (
        <p role="alert" className="mt-3 text-sm text-red-600">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={submitting}
        className="mt-5 w-full rounded-lg bg-[#1f2a6b] py-2.5 text-sm font-semibold text-white hover:bg-[#18215a] disabled:opacity-60"
      >
        {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
      </button>

      <div className="mt-4 text-right">
        <Link href="/reset-password" className="text-xs font-medium text-[#1f2a6b] hover:underline">
          Quên mật khẩu?
        </Link>
      </div>
      <p className="mt-3 text-center text-xs text-slate-500">
        Lần đầu sử dụng?{" "}
        <Link href="/activate" className="font-semibold text-[#1f2a6b] hover:underline">
          Kích hoạt tài khoản
        </Link>
      </p>
    </form>
  );
}
