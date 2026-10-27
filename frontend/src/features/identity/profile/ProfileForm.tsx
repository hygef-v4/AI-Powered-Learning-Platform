"use client";

import { useId, useState, type FormEvent, type ReactNode } from "react";
import { ApiError } from "@/shared/api/client";
import { initials } from "@/shared/layout/navigation";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { Field, TextInput } from "@/shared/ui/inputs";
import { LockedInput } from "@/shared/ui/LockedInput";
import { Pill } from "@/shared/ui/Pill";
import { Section } from "@/shared/ui/Section";
import { showToast } from "@/shared/ui/Toast";
import { STATUS_LABELS, STATUS_TONES } from "../admin/labels";
import { updateProfile } from "../api";
import { ROLE_LABELS, type Profile } from "../types";

const PHONE_FORMAT = /^\+?[0-9]{8,15}$/;

/**
 * Hồ sơ của chính mình (UC 06, 07; design `c05`): sửa họ và tên, số điện thoại; email trường, vai trò, trạng thái chỉ
 * đọc (BR-U01-51). Không có ảnh đại diện, chỉ hiện chữ viết tắt (BR-U01-50).
 */
export function ProfileForm({
  profile,
  onSaved,
  onChangePassword,
}: {
  profile: Profile;
  onSaved: (profile: Profile) => void;
  onChangePassword: () => void;
}) {
  const nameId = useId();
  const phoneId = useId();
  const emailId = useId();
  const roleId = useId();
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [phoneNumber, setPhoneNumber] = useState(profile.phoneNumber ?? "");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);
  const [fieldError, setFieldError] = useState<{ name?: string; phone?: string }>({});
  const dirty = displayName !== profile.displayName || phoneNumber !== (profile.phoneNumber ?? "");

  async function submit(event: FormEvent) {
    event.preventDefault();
    const name = displayName.trim();
    const phone = phoneNumber.trim();
    const errors = {
      name: name.length === 0 || [...name].length > 150 ? "Họ và tên cần từ 1 đến 150 ký tự." : undefined,
      phone: phone && !PHONE_FORMAT.test(phone) ? "Số điện thoại gồm 8–15 chữ số, có thể có dấu + ở đầu." : undefined,
    };
    setFieldError(errors);
    if (errors.name || errors.phone) return;
    setSaving(true);
    setMessage(null);
    try {
      const saved = await updateProfile(name, phone);
      setDisplayName(saved.displayName);
      setPhoneNumber(saved.phoneNumber ?? "");
      showToast("Đã lưu thay đổi");
      onSaved(saved);
    } catch (e) {
      const detail = e instanceof ApiError && e.problem.status === 400 ? e.problem.detail : undefined;
      setMessage({ kind: "error", text: detail ?? "Không lưu được, vui lòng thử lại sau." });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Section className="p-7">
      <form onSubmit={submit} noValidate>
        <div className="grid gap-8 sm:grid-cols-[180px_1fr]">
          <div className="flex justify-center pt-2">
            <span className="flex size-24 items-center justify-center rounded-full bg-primary text-[28px] font-bold text-on-primary">
              {initials(profile.displayName)}
            </span>
          </div>
          <div className="flex flex-col gap-4">
            <Field label="Họ và tên" htmlFor={nameId} error={fieldError.name}>
              <TextInput id={nameId} value={displayName} invalid={!!fieldError.name} onChange={(e) => setDisplayName(e.target.value)} />
            </Field>
            <Field label="Số điện thoại" htmlFor={phoneId} error={fieldError.phone}>
              <TextInput
                id={phoneId}
                inputMode="tel"
                value={phoneNumber}
                invalid={!!fieldError.phone}
                onChange={(e) => setPhoneNumber(e.target.value)}
              />
            </Field>
            <Field label="Email trường" htmlFor={emailId}>
              <LockedInput id={emailId} value={profile.email} />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Vai trò" htmlFor={roleId}>
                <LockedInput id={roleId} value={ROLE_LABELS[profile.role]} />
              </Field>
              <div className="flex min-w-0 flex-col gap-1.5">
                <p className="text-xs font-semibold text-body">Trạng thái tài khoản</p>
                <LockedBox>
                  <Pill tone={STATUS_TONES[profile.status]}>{STATUS_LABELS[profile.status]}</Pill>
                </LockedBox>
              </div>
            </div>
          </div>
        </div>

        {message && (
          <Alert tone={message.kind === "ok" ? "success" : "danger"} role={message.kind === "ok" ? "status" : "alert"} className="mt-5">
            {message.text}
          </Alert>
        )}

        <div className="mt-6 flex items-center justify-between gap-2 border-t border-line pt-5">
          <Button icon="key" onClick={onChangePassword}>
            Đổi mật khẩu
          </Button>
          <Button type="submit" variant="primary" disabled={!dirty || saving}>
            Lưu thay đổi
          </Button>
        </div>
      </form>
    </Section>
  );
}

function LockedBox({ children }: { children: ReactNode }) {
  return (
    <div aria-disabled="true" className="flex h-10 cursor-not-allowed items-center justify-between rounded-lg border border-line-strong bg-shell px-3">
      {children}
      <Icon name="lock" size={16} className="text-faint" />
    </div>
  );
}
