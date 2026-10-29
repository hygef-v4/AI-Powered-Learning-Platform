"use client";

import Link from "next/link";
import { useCallback, useId, useState, type FormEvent, type ReactNode } from "react";
import { ApiError } from "@/shared/api/client";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { requestOtp, verifyOtp } from "../api";
import { AuthLayout } from "../components/AuthLayout";
import { AuthToast, type AuthNotice } from "../components/AuthToast";
import { AUTH_BUTTON, AUTH_INPUT, AUTH_LABEL } from "../components/authStyles";
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

export const OTP_INVALID_MESSAGE = "Mã sai hoặc đã hết hạn.";
export const TICKET_EXPIRED_MESSAGE = "Phiên đã hết hạn.";
export const RESENT_MESSAGE = "Đã gửi lại mã.";
const BUSY_MESSAGE = "Hệ thống bận, thử lại sau.";

type Step = 0 | 1 | 2;

/** Chữ và thẻ thông tin khác nhau giữa Kích hoạt (`c02a`, `c02b`, `c02`) và Khôi phục (`c03a`, `c03`, `c04`). */
export type OtpFlowCopy = {
  purpose: OtpPurpose;
  /** Tiêu đề và dòng phụ của từng bước; `email` là email đã nhập ở bước 1. */
  title: (step: Step) => string;
  steps: [string, string, string];
  sendLabel: string;
  codeLabel: string;
  submitLabel: string;
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
  const [notice, setNotice] = useState<AuthNotice | null>(null);
  const notify = (text: string, tone: AuthNotice["tone"]) => setNotice({ id: Date.now(), text, tone });
  const clearNotice = useCallback(() => setNotice(null), []);

  return (
    <AuthLayout
      aside={copy.aside(step)}
      tagline={copy.tagline?.(step)}
      toast={<AuthToast notice={notice} onDone={clearNotice} />}
    >
      <Link href="/login" className="inline-flex items-center gap-1.5 text-xs font-medium text-muted hover:text-ink">
        <Icon name="arrow_back" size={15} />
        Quay lại đăng nhập
      </Link>
      <h1 className="mt-5 font-display text-[32px] leading-[1.05] tracking-[-0.03em] text-ink">{copy.title(step)}</h1>
      <div className="mt-5">
        <Stepper steps={copy.steps} current={step} />
      </div>

      {step === 0 && (
        <EmailStep
          copy={copy}
          initialEmail={email}
          onSent={(sentEmail) => {
            setEmail(sentEmail);
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
          onResent={() => notify(RESENT_MESSAGE, "success")}
        />
      )}
      {step === 2 && ticket && (
        <PasswordStep
          copy={copy}
          onSubmit={(password) => onComplete(ticket, password)}
          onTicketExpired={() => {
            setTicket(null);
            setStep(0);
            notify(TICKET_EXPIRED_MESSAGE, "warning");
          }}
        />
      )}
    </AuthLayout>
  );
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
  onSent,
}: {
  copy: OtpFlowCopy;
  initialEmail: string;
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
      <label htmlFor={id} className={AUTH_LABEL}>
        Email trường
      </label>
      <div className="relative mt-1.5">
        <input
          id={id}
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={cx(AUTH_INPUT, "pr-10")}
        />
        <Icon name="mail" size={18} className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-muted" />
      </div>
      {error && <ErrorText>{error}</ErrorText>}
      <SubmitButton busy={busy}>{copy.sendLabel}</SubmitButton>
    </form>
  );
}

function VerifyStep({
  copy,
  email,
  onVerified,
  onResent,
}: {
  copy: OtpFlowCopy;
  email: string;
  onVerified: (ticket: string) => void;
  onResent: () => void;
}) {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resending, setResending] = useState(false);
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
    setResending(true);
    try {
      await requestOtp(copy.purpose, email);
      setSentAt(Date.now());
      setCode("");
      onResent();
    } catch {
      setError(BUSY_MESSAGE);
    } finally {
      setResending(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-6">
      <div>
        <OtpInput value={code} onChange={setCode} invalid={error === OTP_INVALID_MESSAGE} label={copy.codeLabel} />
      </div>
      <div className="mt-3 flex items-center justify-between text-xs">
        <span
          className={cx("flex items-center gap-1.5 font-semibold tabular-nums", expiresIn > 0 ? "text-ink" : "text-danger")}
          aria-label={`Thời gian hiệu lực của mã: ${formatMmSs(expiresIn)}`}
        >
          <Icon name="schedule" size={14} className="text-muted" />
          {formatMmSs(expiresIn)}
        </span>
        {/* Khóa đến khi được phép gửi lại (60 giây) và trong lúc đang gửi. */}
        <button
          type="button"
          onClick={resend}
          disabled={resendIn > 0 || resending}
          className="font-semibold text-primary-ink hover:underline disabled:cursor-not-allowed disabled:text-muted disabled:no-underline"
        >
          Gửi lại mã
        </button>
      </div>
      {error && <ErrorText>{error}</ErrorText>}
      <SubmitButton busy={busy} disabled={code.length < 6}>
        Xác nhận mã
      </SubmitButton>
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
    </form>
  );
}
