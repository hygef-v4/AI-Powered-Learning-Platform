"use client";

import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import { Field } from "@/shared/ui/inputs";
import { changePassword } from "../api";
import { PasswordChecklist } from "../components/PasswordChecklist";
import { SecretInput } from "../components/SecretInput";
import { PASSWORD_VIOLATION_MESSAGES, passwordAcceptable } from "../passwordRules";

export const PASSWORD_CHANGED_MESSAGE = "Đã đổi mật khẩu. Các thiết bị khác đã bị đăng xuất.";

/** Popup Password Change (UC 05, design `c06`): mật khẩu hiện tại, mật khẩu mới và nhập lại (BR-U01-33). */
export function PasswordChangeDialog({ onClose, onChanged }: { onClose: () => void; onChanged: () => void }) {
  const formId = useId();
  const currentId = useId();
  const newId = useId();
  const confirmId = useId();
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
      } else if (problem.code === "TOO_MANY_ATTEMPTS") {
        setError("Nhập sai mật khẩu hiện tại quá nhiều lần, hãy thử lại sau ít phút.");
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
    <Dialog
      open
      title="Đổi mật khẩu"
      icon="lock_reset"
      onClose={onClose}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            Hủy
          </Button>
          <Button type="submit" form={formId} variant="primary" icon="lock_reset" disabled={busy}>
            Đổi mật khẩu
          </Button>
        </>
      }
    >
      <form id={formId} onSubmit={submit} noValidate className="flex flex-col gap-3.5">
        <Field label="Mật khẩu hiện tại" htmlFor={currentId}>
          <SecretInput id={currentId} autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
        </Field>
        <Field label="Mật khẩu mới" htmlFor={newId}>
          <SecretInput id={newId} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <Field label="Nhập lại mật khẩu mới" htmlFor={confirmId}>
          <SecretInput id={confirmId} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />
        </Field>
        <PasswordChecklist password={password} chips />
        <p className="text-xs text-muted">Các thiết bị khác sẽ bị đăng xuất. Phiên hiện tại được giữ lại.</p>
        {error && (
          <p role="alert" className="text-[13px] text-danger">
            {error}
          </p>
        )}
      </form>
    </Dialog>
  );
}
