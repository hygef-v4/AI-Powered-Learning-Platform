"use client";

import Link from "next/link";
import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { login } from "./api";
import { AUTH_INPUT, AUTH_LABEL, LOGIN_BUTTON } from "./components/authStyles";
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
      <h1 className="font-display text-[34px] leading-none tracking-[-0.03em] text-ink">Đăng nhập</h1>

      <div className="mt-7">
        <label htmlFor={emailId} className={AUTH_LABEL}>
          Email
        </label>
        <input
          id={emailId}
          type="email"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={cx(AUTH_INPUT, "mt-1.5")}
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
        <p role="alert" className="mt-3 flex items-start gap-1.5 text-[13px] text-danger">
          <Icon name="error" size={16} className="mt-px" />
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={submitting}
        className={cx(LOGIN_BUTTON, "mt-6")}
      >
        {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
      </button>

      <div className="mt-4 text-right">
        <Link href="/reset-password" className="text-xs font-medium text-primary-ink hover:underline">
          Quên mật khẩu?
        </Link>
      </div>
      <p className="mt-3 text-center text-xs text-muted">
        Lần đầu sử dụng?{" "}
        <Link href="/activate" className="font-semibold text-primary-ink hover:underline">
          Kích hoạt tài khoản
        </Link>
      </p>
    </form>
  );
}
