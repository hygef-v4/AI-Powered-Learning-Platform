"use client";

import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { updateProfile } from "../api";
import { ROLE_LABELS, type AccountStatus, type Profile } from "../types";

const STATUS_LABELS: Record<AccountStatus, string> = {
  PENDING: "Chờ kích hoạt",
  ACTIVE: "Đang hoạt động",
  DISABLED: "Đã vô hiệu hóa",
};

const PHONE_FORMAT = /^\+?[0-9]{8,15}$/;

/**
 * Hồ sơ của chính mình (UC 06, 07): sửa họ và tên, số điện thoại; email trường, vai trò, trạng thái chỉ đọc
 * (BR-U01-51). Không có ảnh đại diện, chỉ hiện chữ cái đầu (BR-U01-50).
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
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [phoneNumber, setPhoneNumber] = useState(profile.phoneNumber ?? "");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);
  const dirty = displayName !== profile.displayName || phoneNumber !== (profile.phoneNumber ?? "");
  const initials = profile.displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((word) => word[0]?.toUpperCase())
    .join("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    const name = displayName.trim();
    const phone = phoneNumber.trim();
    if (name.length === 0 || [...name].length > 150) {
      setMessage({ kind: "error", text: "Họ và tên cần từ 1 đến 150 ký tự." });
      return;
    }
    if (phone && !PHONE_FORMAT.test(phone)) {
      setMessage({ kind: "error", text: "Số điện thoại gồm 8–15 chữ số, có thể có dấu + ở đầu." });
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const saved = await updateProfile(name, phone);
      setDisplayName(saved.displayName);
      setPhoneNumber(saved.phoneNumber ?? "");
      setMessage({ kind: "ok", text: "Đã lưu thay đổi." });
      onSaved(saved);
    } catch (e) {
      const detail = e instanceof ApiError && e.problem.status === 400 ? e.problem.detail : undefined;
      setMessage({ kind: "error", text: detail ?? "Không lưu được, vui lòng thử lại sau." });
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="rounded-xl border border-slate-200 bg-white p-6">
      <div className="flex items-center gap-4">
        <span className="flex h-14 w-14 items-center justify-center rounded-full bg-indigo-100 text-lg font-semibold text-indigo-700">
          {initials}
        </span>
        <div>
          <p className="text-lg font-semibold text-slate-900">{profile.displayName}</p>
          <p className="text-sm text-slate-500">{ROLE_LABELS[profile.role]}</p>
        </div>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor={nameId} className="text-xs font-semibold text-slate-700">
            Họ và tên
          </label>
          <input
            id={nameId}
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          />
        </div>
        <div>
          <label htmlFor={phoneId} className="text-xs font-semibold text-slate-700">
            Số điện thoại
          </label>
          <input
            id={phoneId}
            inputMode="tel"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
          />
        </div>
        <ReadOnly label="Email trường" value={profile.email} />
        <ReadOnly label="Vai trò" value={ROLE_LABELS[profile.role]} />
        <ReadOnly label="Trạng thái" value={STATUS_LABELS[profile.status]} />
      </div>

      {message && (
        <p role={message.kind === "error" ? "alert" : "status"} className={`mt-4 text-sm ${message.kind === "error" ? "text-red-600" : "text-emerald-700"}`}>
          {message.text}
        </p>
      )}

      <div className="mt-6 flex justify-between gap-2">
        <button
          type="button"
          onClick={onChangePassword}
          className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
        >
          Đổi mật khẩu
        </button>
        <button
          type="submit"
          disabled={!dirty || saving}
          className="rounded-lg bg-[#1f2a6b] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
        >
          Lưu thay đổi
        </button>
      </div>
    </form>
  );
}

function ReadOnly({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold text-slate-700">{label}</p>
      <p className="mt-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600" aria-readonly="true">
        <span className="mr-1" aria-hidden="true">
          🔒
        </span>
        {value}
      </p>
    </div>
  );
}
