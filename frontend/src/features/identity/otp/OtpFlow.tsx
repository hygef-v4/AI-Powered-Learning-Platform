"use client";

import Link from "next/link";
import { useId, useState, type FormEvent, type ReactNode } from "react";
import { ApiError } from "@/shared/api/client";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { requestOtp, verifyOtp } from "../api";
import { AuthLayout } from "../components/AuthLayout";
import { AUTH_BUTTON, AUTH_INPUT, AUTH_LABEL, AUTH_NOTE } from "../components/authStyles";
import { OtpInput } from "../components/OtpInput";
import { PasswordChecklist } from "../components/PasswordChecklist";
import { PasswordField } from "../components/PasswordField";
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

type Step = 0 | 1 | 2;

/** Chữ và thẻ thông tin khác nhau giữa Kích hoạt (`c02a`, `c02b`, `c02`) và Khôi phục (`c03a`, `c03`, `c04`). */
export type OtpFlowCopy = {
  purpose: OtpPurpose;
  /** Tiêu đề và dòng phụ của từng bước; `email` là email đã nhập ở bước 1. */
  title: (step: Step) => string;
  subtitle: (step: Step, email: string) => string;
  steps: [string, string, string];
  sendLabel: string;
  sendNote: string;
  codeLabel: string;
  verifyNote: string;
  submitLabel: string;
  submitNote: string;
  /** Thẻ trắng ở khối bên phải theo bước. */
  aside: (step: Step) => ReactNode;
  /** Dòng phụ dưới khẩu hiệu (chỉ màn Khôi phục có). */
  tagline?: (step: Step) => boolean;
};

/**
 * Ba bước email → OTP → mật khẩu mới (BR-U01-28): ô mật khẩu chỉ mở sau khi OTP đúng; otpTicket chỉ giữ trong state
 * của trang, không lưu ở storage hay URL. `onComplete` gửi mật khẩu kèm ticket; ticket hết hạn thì quay về bước 1.
 */
export function OtpFlow({ copy, onComplete }: { copy: OtpFlowCopy; onComplete: (ticket: string, password: string) => Promise<void> }) {
  const [step, setStep] = useState<Step>(0);
  const [email, setEmail] = useState("");
  const [ticket, setTicket] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  return (
    <AuthLayout aside={copy.aside(step)} tagline={copy.tagline?.(step)}>
      <Link href="/login" className="inline-flex items-center gap-1.5 text-xs font-medium text-[#64748b] hover:text-ink">
        <Icon name="arrow_back" size={15} />
        Quay lại đăng nhập
      </Link>
      <h1 className="mt-5 text-[28px] leading-tight font-bold tracking-[-0.7px] text-ink">{copy.title(step)}</h1>
      <p className="mt-1.5 text-sm text-[#64748b]">{copy.subtitle(step, email)}</p>
      <div className="mt-5">
        <Stepper steps={copy.steps} current={step} />
      </div>

      {step === 0 && (
        <EmailStep
          copy={copy}
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
          copy={copy}
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
          copy={copy}
          onSubmit={(password) => onComplete(ticket, password)}
          onTicketExpired={() => {
            setTicket(null);
            setStep(0);
            setNotice(TICKET_EXPIRED_MESSAGE);
          }}
        />
      )}
    </AuthLayout>
  );
}

/** "taivm@fpt.edu.vn" → "t***m@fpt.edu.vn" như design `c02b`. */
export function maskEmail(email: string) {
  const at = email.indexOf("@");
  if (at < 1) return email;
  const name = email.slice(0, at);
  const masked = name.length <= 2 ? `${name[0]}***` : `${name[0]}***${name[name.length - 1]}`;
  return masked + email.slice(at);
}

function errorProblem(error: unknown) {
  return (error instanceof ApiError ? error.problem : { status: 0 }) as { status: number; code?: string; violations?: string[] };
}

function ErrorText({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="mt-2 flex items-start gap-1.5 text-[13px] text-danger">
      <Icon name="error" size={16} className="mt-px" />
      {children}
    </p>
  );
}

function SubmitButton({ busy, disabled, children }: { busy: boolean; disabled?: boolean; children: string }) {
  return (
    <button type="submit" disabled={busy || disabled} className={cx(AUTH_BUTTON, "mt-6")}>
      {children}
      <Icon name="arrow_forward" size={18} />
    </button>
  );
}

function EmailStep({
  copy,
  initialEmail,
  notice,
  onSent,
}: {
  copy: OtpFlowCopy;
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
      await requestOtp(copy.purpose, value);
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
        <p role="status" className="mb-4 flex items-start gap-2 rounded-[10px] bg-warning-soft px-3.5 py-2.5 text-[13px] text-warning">
          <Icon name="warning" size={18} />
          {notice}
        </p>
      )}
      <label htmlFor={id} className={AUTH_LABEL}>
        Email trường
      </label>
      <div className="relative mt-1.5">
        <input
          id={id}
          type="email"
          autoComplete="email"
          placeholder={copy.purpose === "ACTIVATION" ? "masv@fpt.edu.vn" : "ten@fpt.edu.vn"}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={cx(AUTH_INPUT, "pr-10")}
        />
        <Icon name="mail" size={18} className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[#94a3b8]" />
      </div>
      {error && <ErrorText>{error}</ErrorText>}
      <SubmitButton busy={busy}>{copy.sendLabel}</SubmitButton>
      <p className={cx(AUTH_NOTE, "mt-4")}>{copy.sendNote}</p>
    </form>
  );
}

function VerifyStep({
  copy,
  email,
  onVerified,
  onChangeEmail,
}: {
  copy: OtpFlowCopy;
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
      onVerified((await verifyOtp(copy.purpose, email, code)).otpTicket);
    } catch (e) {
      setError(errorProblem(e).status === 400 ? OTP_INVALID_MESSAGE : BUSY_MESSAGE);
      setCode("");
    } finally {
      setBusy(false);
    }
  }

  async function resend() {
    setError(null);
    try {
      await requestOtp(copy.purpose, email);
      setSentAt(Date.now());
      setCode("");
    } catch {
      setError(BUSY_MESSAGE);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-6">
      <p className="rounded-[10px] bg-[#f1f5f9] px-3.5 py-2.5 text-[13px] text-[#475569]">
        Đã gửi mã tới <b>{maskEmail(email)}</b>.{" "}
        <button type="button" onClick={onChangeEmail} className="font-semibold text-primary-strong hover:underline">
          Đổi email
        </button>
      </p>

      <div className="mt-5">
        <OtpInput value={code} onChange={setCode} invalid={error === OTP_INVALID_MESSAGE} label={copy.codeLabel} />
      </div>
      <div className="mt-3 flex items-center justify-between text-xs">
        <span className="flex items-center gap-1.5 text-[#64748b]">
          <Icon name="schedule" size={14} />
          {expiresIn > 0 ? (
            <>
              Mã hết hạn sau <b className="text-ink">{formatMmSs(expiresIn)}</b>
            </>
          ) : (
            "Mã đã hết hạn, hãy gửi lại mã."
          )}
        </span>
        {resendIn > 0 ? (
          <span className="text-[#94a3b8]">Gửi lại mã sau {formatMmSs(resendIn)}</span>
        ) : (
          <button type="button" onClick={resend} className="font-semibold text-primary-strong hover:underline">
            Gửi lại mã
          </button>
        )}
      </div>
      {error && <ErrorText>{error}</ErrorText>}
      <SubmitButton busy={busy} disabled={code.length < 6}>
        Xác nhận mã
      </SubmitButton>
      <p className={cx(AUTH_NOTE, "mt-4")}>{copy.verifyNote}</p>
    </form>
  );
}

function PasswordStep({
  copy,
  onSubmit,
  onTicketExpired,
}: {
  copy: OtpFlowCopy;
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
      const problem = errorProblem(e);
      if (problem.code === "TICKET_EXPIRED") {
        onTicketExpired();
        return;
      }
      setError(
        problem.code === "PASSWORD_POLICY"
          ? (problem.violations ?? []).map((v) => PASSWORD_VIOLATION_MESSAGES[v] ?? v).join(" ") || "Mật khẩu chưa đạt yêu cầu."
          : BUSY_MESSAGE,
      );
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
      {error && <ErrorText>{error}</ErrorText>}
      <SubmitButton busy={busy}>{copy.submitLabel}</SubmitButton>
      <p className={cx(AUTH_NOTE, "mt-4")}>{copy.submitNote}</p>
    </form>
  );
}
