"use client";

import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import type { Role } from "@/shared/auth/roles";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import { Field, Select, TextInput } from "@/shared/ui/inputs";
import { ROLE_LABELS } from "../types";
import { createAccount } from "./api";
import { ADMIN_ERROR_LABELS } from "./labels";

const EMAIL_FORMAT = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const ROLES: Role[] = ["STUDENT", "TEACHER", "SUBJECT_MANAGER", "ADMIN"];

/**
 * Popup New Account (UC 09, design `a03`): email trường, họ tên, vai trò. Tài khoản ở trạng thái chờ kích hoạt, không mật
 * khẩu, không gửi email (BR-U01-10); người dùng tự kích hoạt.
 */
export function NewAccountDialog({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const formId = useId();
  const emailId = useId();
  const nameId = useId();
  const roleId = useId();
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [role, setRole] = useState<Role>("STUDENT");
  const [busy, setBusy] = useState(false);
  const [fieldError, setFieldError] = useState<{ email?: string; name?: string }>({});
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const errors = {
      email: EMAIL_FORMAT.test(email.trim()) ? undefined : "Email không hợp lệ.",
      name: displayName.trim().length === 0 ? "Nhập họ và tên." : undefined,
    };
    setFieldError(errors);
    if (errors.email || errors.name) return;
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
    <Dialog
      open
      title="Thêm tài khoản"
      icon="person_add"
      onClose={onClose}
      footer={
        <>
          <Button type="submit" form={formId} variant="primary" icon="person_add" disabled={busy}>
            Thêm tài khoản
          </Button>
        </>
      }
    >
      <form id={formId} onSubmit={submit} noValidate className="flex flex-col gap-3.5">
        <Field
          label="Email trường"
          htmlFor={emailId}
          required
          error={fieldError.email}
        >
          <TextInput
            id={emailId}
            type="email"
            value={email}
            invalid={!!fieldError.email}
            placeholder="masv@fpt.edu.vn"
            onChange={(e) => setEmail(e.target.value)}
          />
        </Field>
        <Field label="Họ và tên" htmlFor={nameId} required error={fieldError.name}>
          <TextInput id={nameId} value={displayName} invalid={!!fieldError.name} onChange={(e) => setDisplayName(e.target.value)} />
        </Field>
        <Field label="Vai trò" htmlFor={roleId} required>
          <Select id={roleId} value={role} onChange={(e) => setRole(e.target.value as Role)}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {ROLE_LABELS[r]}
              </option>
            ))}
          </Select>
        </Field>
        {error && (
          <Alert tone="danger" role="alert">
            {error}
          </Alert>
        )}
      </form>
    </Dialog>
  );
}
