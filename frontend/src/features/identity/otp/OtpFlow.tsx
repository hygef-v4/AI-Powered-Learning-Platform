"use client";

import Link from "next/link";
import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { requestOtp, verifyOtp } from "../api";
import { PasswordChecklist } from "../components/PasswordChecklist";
import { PasswordField } from "../components/PasswordField";
import { OtpInput } from "../components/OtpInput";
import { Stepper } from "../components/Stepper";
import { PASSWORD_VIOLATION_MESSAGES, passwordAcceptable } from "../passwordRules";
import type { OtpPurpose } from "../types";
import { formatMmSs, useCountdown } from "../useCountdown";

const OTP_TTL_SECONDS = 10 * 60;
const RESEND_COOLDOWN_SECONDS = 60;
const EMAIL_FORMAT = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const OTP_INVALID_MESSAGE = "Mã không hợp lệ hoặc đã hết hạn.";
export const TICKET_EXPIRED_MESSAGE = "Phiên xác minh đã hết hạn, hãy yêu cầu mã mới.";
const BUSY_MESSAGE = "Hệ thống tạm bận, vui lòng thử lại sau.";

/** Nội dung khác nhau giữa Kích hoạt (UC 01) và Đặt lại mật khẩu (UC 04). */
export type OtpFlowCopy = {
  purpose: OtpPurpose;
  title: string;
  subtitle: string;
  passwordTitle: string;
  passwordSubtitle: (email: string) => string;
  steps: [string, string, string];
  submitLabel: string;
  submitNote: string;
};

/**
 * Ba bước email → OTP → mật khẩu mới (BR-U01-28): ô mật khẩu chỉ mở sau khi OTP đúng; otpTicket chỉ giữ trong state
 * của trang, không lưu ở storage hay URL. `onComplete` gửi mật khẩu kèm ticket; lỗi ticket hết hạn thì quay về bước 1.
 */
export function OtpFlow({ copy, onComplete }: { copy: OtpFlowCopy; onComplete: (ticket: string, password: string) => Promise<void> }) {
  const [step, setStep] = useState<0 | 1 | 2>(0);
  const [email, setEmail] = useState("");
  const [ticket, setTicket] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  function restart(message: string) {
    setTicket(null);
    setStep(0);
    setNotice(message);
  }

  return (
    <div>
      <Link href="/login" className="text-xs text-slate-500 hover:text-slate-700">
        ← Quay lại đăng nhập
      </Link>
      <h1 className="mt-4 text-2xl font-bold text-slate-900">{step === 2 ? copy.passwordTitle : copy.title}</h1>
      <p className="mt-1 text-sm text-slate-500">{step === 2 ? copy.passwordSubtitle(email) : copy.subtitle}</p>
      <div className="mt-4">
        <Stepper steps={copy.steps} current={step} />
      </div>

      {step === 0 && (
        <EmailStep
          purpose={copy.purpose}
          initialEmail={email}
          notice={notice}
          onSent={(sentEmail) => {
            setEmail(sentEmail);
            setNotice(null);
            setStep(1);
          }}
        />
      )}
      {step === 1 && (
        <VerifyStep
          purpose={copy.purpose}
          email={email}
          onVerified={(otpTicket) => {
            setTicket(otpTicket);
            setStep(2);
          }}
          onChangeEmail={() => setStep(0)}
        />
      )}
      {step === 2 && ticket && (
        <PasswordStep
          submitLabel={copy.submitLabel}
          submitNote={copy.submitNote}
          onSubmit={(password) => onComplete(ticket, password)}
          onTicketExpired={() => restart(TICKET_EXPIRED_MESSAGE)}
        />
      )}
    </div>
  );
}

function errorStatus(error: unknown) {
  return error instanceof ApiError ? error.problem : { status: 0 };
}

function PrimaryButton({ busy, children }: { busy: boolean; children: string }) {
  return (
    <button
      type="submit"
      disabled={busy}
      className="mt-5 w-full rounded-lg bg-[#1f2a6b] py-2.5 text-sm font-semibold text-white hover:bg-[#18215a] disabled:opacity-60"
    >
      {children}
    </button>
  );
}

function EmailStep({
  purpose,
  initialEmail,
  notice,
  onSent,
}: {
  purpose: OtpPurpose;
  initialEmail: string;
  notice: string | null;
  onSent: (email: string) => void;
}) {
  const id = useId();
  const [email, setEmail] = useState(initialEmail);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const value = email.trim();
    if (!EMAIL_FORMAT.test(value)) {
      setError("Nhập email trường hợp lệ.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await requestOtp(purpose, value);
      onSent(value);
    } catch {
      setError(BUSY_MESSAGE);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-6">
      {notice && (
        <p role="status" className="mb-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {notice}
        </p>
      )}
      <label htmlFor={id} className="text-[11px] font-semibold tracking-wide text-slate-700 uppercase">
        Email
      </label>
      <input
        id={id}
        type="email"
        autoComplete="email"
        placeholder="masv@fpt.edu.vn"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
      />
      {error && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {error}
        </p>
      )}
      <PrimaryButton busy={busy}>Gửi mã</PrimaryButton>
    </form>
  );
}

function VerifyStep({
  purpose,
  email,
  onVerified,
  onChangeEmail,
}: {
  purpose: OtpPurpose;
  email: string;
  onVerified: (ticket: string) => void;
  onChangeEmail: () => void;
}) {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sentAt, setSentAt] = useState(() => Date.now());
  const expiresIn = useCountdown(sentAt + OTP_TTL_SECONDS * 1000);
  const resendIn = useCountdown(sentAt + RESEND_COOLDOWN_SECONDS * 1000);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!/^\d{6}$/.test(code)) {
      setError("Nhập đủ 6 chữ số.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      onVerified((await verifyOtp(purpose, email, code)).otpTicket);
    } catch (e) {
      const status = errorStatus(e).status;
      setError(status === 400 ? OTP_INVALID_MESSAGE : BUSY_MESSAGE);
      setCode("");
    } finally {
      setBusy(false);
    }
  }

  async function resend() {
    setError(null);
    try {
      await requestOtp(purpose, email);
      setSentAt(Date.now());
      setCode("");
    } catch {
      setError(BUSY_MESSAGE);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-6">
      <p className="text-[11px] font-semibold tracking-wide text-slate-700 uppercase">Email</p>
      <div className="mt-1 flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm">
        <span className="text-slate-700">{email}</span>
        <span className="flex items-center gap-2">
          <span className="text-emerald-600" aria-hidden="true">
            ✓
          </span>
          <button type="button" onClick={onChangeEmail} className="text-xs text-[#1f2a6b] hover:underline">
            Đổi email
          </button>
        </span>
      </div>

      <div className="mt-4">
        <OtpInput value={code} onChange={setCode} invalid={error === OTP_INVALID_MESSAGE} />
      </div>
      <div className="mt-2 flex items-center justify-between text-xs">
        <span className="text-slate-500">
          {expiresIn > 0 ? `Mã hết hạn sau ${formatMmSs(expiresIn)}` : "Mã đã hết hạn, hãy gửi lại mã."}
        </span>
        <button
          type="button"
          onClick={resend}
          disabled={resendIn > 0}
          className="font-medium text-[#1f2a6b] hover:underline disabled:text-slate-400 disabled:no-underline"
        >
          {resendIn > 0 ? `Gửi lại mã (${resendIn}s)` : "Gửi lại mã"}
        </button>
      </div>
      {error && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {error}
        </p>
      )}
      <PrimaryButton busy={busy}>Xác nhận mã</PrimaryButton>
      <p className="mt-3 text-center text-xs text-slate-500">Nếu email tồn tại, bạn sẽ nhận được mã trong vài phút.</p>
    </form>
  );
}

function PasswordStep({
  submitLabel,
  submitNote,
  onSubmit,
  onTicketExpired,
}: {
  submitLabel: string;
  submitNote: string;
  onSubmit: (password: string) => Promise<void>;
  onTicketExpired: () => void;
}) {
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const acceptable = passwordAcceptable(password);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!acceptable) {
      setError("Mật khẩu cần ít nhất 8 ký tự, có cả chữ và số.");
      return;
    }
    if (password !== confirm) {
      setError("Hai mật khẩu chưa khớp.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSubmit(password);
    } catch (e) {
      const problem = errorStatus(e) as { status: number; code?: string; violations?: string[] };
      if (problem.code === "TICKET_EXPIRED") {
        onTicketExpired();
        return;
      }
      if (problem.code === "PASSWORD_POLICY") {
        setError(
          (problem.violations ?? []).map((v) => PASSWORD_VIOLATION_MESSAGES[v] ?? v).join(" ") ||
            "Mật khẩu chưa đạt yêu cầu.",
        );
      } else {
        setError(BUSY_MESSAGE);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-6">
      <PasswordField
        label="Mật khẩu mới"
        autoComplete="new-password"
        value={password}
        valid={acceptable}
        onChange={(e) => setPassword(e.target.value)}
      />
      <PasswordField
        className="mt-4"
        label="Nhập lại mật khẩu"
        autoComplete="new-password"
        value={confirm}
        valid={confirm.length > 0 && confirm === password}
        onChange={(e) => setConfirm(e.target.value)}
      />
      <PasswordChecklist password={password} />
      {error && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {error}
        </p>
      )}
      <PrimaryButton busy={busy}>{`${submitLabel} →`}</PrimaryButton>
      <p className="mt-3 text-center text-xs text-slate-500">{submitNote}</p>
    </form>
  );
}
