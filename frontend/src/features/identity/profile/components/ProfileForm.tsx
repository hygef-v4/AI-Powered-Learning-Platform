"use client";

import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { initials } from "@/shared/layout/navigation";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { Field, TextInput } from "@/shared/ui/inputs";
import { LockedInput } from "@/shared/ui/LockedInput";
import { Pill } from "@/shared/ui/Pill";
import { Section } from "@/shared/ui/Section";
import { showToast } from "@/shared/ui/Toast";
import { STATUS_LABELS, STATUS_TONES } from "../../admin/model/labels";
import { updateProfile } from "../../api";
import { ROLE_LABELS, type Profile } from "../../model/types";

const PHONE_FORMAT = /^\+?[0-9]{8,15}$/;

/**
 * Hồ sơ của chính mình (UC 06, 07; design `c05`): sửa họ và tên, số điện thoại; email trường, vai trò, trạng thái chỉ
 * đọc (BR-U01-51). Không có ảnh đại diện, chỉ hiện chữ viết tắt (BR-U01-50).
 */
export function ProfileForm({
  profile,
  onSaved,
}: {
  profile: Profile;
  onSaved: (profile: Profile) => void;
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
    <Section
      title="Thông tin cá nhân"
      icon="person"
      description="Email trường, vai trò và trạng thái do quản trị viên quản lý."
    >
      <div className="flex items-center gap-4 rounded-lg bg-shell p-4">
        <span
          aria-hidden="true"
          className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary text-xl font-bold text-on-primary"
        >
          {initials(profile.displayName)}
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-base font-bold text-ink">{profile.displayName}</p>
          <p className="truncate text-[13px] text-muted">{profile.email}</p>
        </div>
        <Pill tone={STATUS_TONES[profile.status]}>{STATUS_LABELS[profile.status]}</Pill>
      </div>

      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Họ và tên" htmlFor={nameId} error={fieldError.name}>
            <TextInput
              id={nameId}
              value={displayName}
              invalid={!!fieldError.name}
              onChange={(e) => setDisplayName(e.target.value)}
            />
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
          <Field label="Vai trò" htmlFor={roleId}>
            <LockedInput id={roleId} value={ROLE_LABELS[profile.role]} />
          </Field>
        </div>

        {message && (
          <Alert
            tone={message.kind === "ok" ? "success" : "danger"}
            role={message.kind === "ok" ? "status" : "alert"}
          >
            {message.text}
          </Alert>
        )}

        <div className="flex justify-end border-t border-line pt-5">
          <Button type="submit" variant="primary" disabled={!dirty || saving}>
            Lưu thay đổi
          </Button>
        </div>
      </form>
    </Section>
  );
}
