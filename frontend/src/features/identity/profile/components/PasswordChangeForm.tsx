"use client";

import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { Button } from "@/shared/ui/Button";
import { Field } from "@/shared/ui/Inputs";
import { Section } from "@/shared/ui/Section";
import { changePassword } from "../../api";
import { PasswordChecklist } from "../../components/PasswordChecklist";
import { SecretInput } from "../../components/SecretInput";
import { PASSWORD_VIOLATION_MESSAGES, isPasswordAcceptable } from "../../model/passwordRules";
import { msg } from "@/i18n/msg";

export const PASSWORD_CHANGED_MESSAGE = msg("identity.profile.components.PasswordChangeForm.daDoiMatKhau");

/**
 * Khối Đổi mật khẩu nằm ngay trên trang Hồ sơ (UC 05, design `c06`): mật khẩu hiện tại, mật khẩu mới và nhập lại
 * (BR-U01-33). Đổi xong thì xóa các ô và báo cho trang.
 */
export function PasswordChangeForm({ onChanged }: { onChanged: () => void }) {
  const currentId = useId();
  const newId = useId();
  const confirmId = useId();
  const [current, setCurrent] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const acceptable = isPasswordAcceptable(password);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (current.length === 0) {
      setError(msg("identity.profile.components.PasswordChangeForm.nhapMatKhauHienTai"));
      return;
    }
    if (!acceptable) {
      setError(msg("identity.profile.components.PasswordChangeForm.matKhauMoiCanItNhat"));
      return;
    }
    if (password !== confirm) {
      setError(msg("identity.profile.components.PasswordChangeForm.haiMatKhauMoiChuaKhop"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await changePassword(current, password);
      setCurrent("");
      setPassword("");
      setConfirm("");
      onChanged();
    } catch (e) {
      const problem = (e instanceof ApiError ? e.problem : { status: 0 }) as {
        status: number;
        code?: string;
        violations?: string[];
      };
      if (problem.code === "CURRENT_PASSWORD_INVALID") {
        setError(msg("identity.profile.components.PasswordChangeForm.matKhauHienTaiKhongDung"));
        setCurrent("");
      } else if (problem.code === "TOO_MANY_ATTEMPTS") {
        setError(msg("identity.profile.components.PasswordChangeForm.nhapSaiMatKhauHienTai"));
        setCurrent("");
      } else if (problem.code === "PASSWORD_POLICY") {
        setError((problem.violations ?? []).map((v) => PASSWORD_VIOLATION_MESSAGES[v] ?? v).join(" "));
      } else {
        setError(msg("identity.profile.components.PasswordChangeForm.heThongTamBanVuiLong"));
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <Section
      title={msg("identity.profile.components.PasswordChangeForm.doiMatKhau")}
      icon="lock_reset"
      description={msg("identity.profile.components.PasswordChangeForm.cacThietBiKhacSeBi")}
    >
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={msg("identity.profile.components.PasswordChangeForm.matKhauHienTai")} htmlFor={currentId}>
            <SecretInput
              id={currentId}
              autoComplete="current-password"
              value={current}
              onChange={(e) => setCurrent(e.target.value)}
            />
          </Field>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={msg("identity.profile.components.PasswordChangeForm.matKhauMoi")} htmlFor={newId}>
            <SecretInput
              id={newId}
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>
          <Field label={msg("identity.profile.components.PasswordChangeForm.nhapLaiMatKhauMoi")} htmlFor={confirmId}>
            <SecretInput
              id={confirmId}
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
          </Field>
        </div>
        <PasswordChecklist password={password} chips />
        {error && (
          <p role="alert" className="text-[13px] text-danger">
            {error}
          </p>
        )}
        <div className="flex justify-end border-t border-line pt-5">
          <Button type="submit" icon="key" disabled={busy}>
            {msg("identity.profile.components.PasswordChangeForm.doiMatKhau")}</Button>
        </div>
      </form>
    </Section>
  );
}
