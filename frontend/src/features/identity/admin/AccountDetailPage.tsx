"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert } from "@/shared/ui/Alert";
import { Button, IconButton } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { Field, TextInput } from "@/shared/ui/inputs";
import { LockedInput } from "@/shared/ui/LockedInput";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Pill } from "@/shared/ui/Pill";
import { Section } from "@/shared/ui/Section";
import { showToast, type ToastTone } from "@/shared/ui/Toast";
import { useSession } from "../session";
import { ROLE_LABELS } from "../types";
import { RoleChangeDialog, RoleChangeRejectedDialog, StatusToggleDialog, type RoleChangeBlock } from "./AccountDialogs";
import { getAccount, updateAccount, type AccountDetail } from "./api";
import { ADMIN_ERROR_LABELS, formatDate, STATUS_LABELS, STATUS_TONES } from "./labels";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const PHONE_FORMAT = /^\+?[0-9]{8,15}$/;

type Popup = { kind: "role" } | { kind: "blocked"; block: RoleChangeBlock } | { kind: "status" } | null;

/**
 * Account Detail (UC 10–12, design `a04`): sửa họ tên, số điện thoại kèm version; đổi vai trò (`a04b`, `a04c`) và vô hiệu
 * hóa/mở lại (`a04d`) qua popup. Email chỉ đọc; không có nút gửi OTP hay đặt mật khẩu.
 */
export function AccountDetailPage() {
  const { id } = useParams<{ id: string }>();
  const session = useSession();
  const [account, setAccount] = useState<AccountDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [popup, setPopup] = useState<Popup>(null);

  useEffect(() => {
    let active = true;
    getAccount(id)
      .then((detail) => {
        if (active) setAccount(detail);
      })
      .catch((e: unknown) => {
        if (active) setLoadError(e instanceof ApiError && e.problem.status === 404 ? "Không tìm thấy tài khoản." : "Không tải được tài khoản.");
      });
    return () => {
      active = false;
    };
  }, [id]);

  /** Tải lại sau xung đột version; không ném lỗi để popup/form gọi nó vẫn kết thúc bình thường. */
  async function reload() {
    try {
      setAccount(await getAccount(id));
    } catch {
      setLoadError("Không tải lại được tài khoản. Hãy tải lại trang.");
    }
  }

  if (loadError) {
    return (
      <>
        <PageHeader title="Tài khoản" backHref="/admin/accounts" />
        <PageLoadError onRetry={reload}>{loadError}</PageLoadError>
      </>
    );
  }
  if (!account) return <PageLoading />;

  const isSelf = session.status === "authenticated" && session.profile.accountId === account.id;
  const reopening = account.status === "DISABLED";

  function changed(saved: AccountDetail, message: string, tone: ToastTone = "success") {
    setAccount(saved);
    setPopup(null);
    showToast(message, tone);
  }

  return (
    <>
      <PageHeader
        backHref="/admin/accounts"
        title={
          <>
            {account.displayName}
            <Pill tone={STATUS_TONES[account.status]}>{STATUS_LABELS[account.status]}</Pill>
          </>
        }
        breadcrumb={[{ label: "Tài khoản", href: "/admin/accounts" }, { label: account.email }]}
      />
      <div className="grid items-start gap-4 lg:grid-cols-[1fr_280px]">
        <AccountEditForm
          key={account.version}
          account={account}
          isSelf={isSelf}
          onSaved={(saved) => changed(saved, "Đã lưu thay đổi")}
          onConflict={reload}
          onChangeRole={() => setPopup({ kind: "role" })}
        />
        <div className="flex flex-col gap-4">
          <Section title="Trạng thái">
            <dl className="flex flex-col gap-2.5 text-[13px]">
              <div className="flex items-center justify-between">
                <dt className="text-muted">Trạng thái</dt>
                <dd>
                  <Pill tone={STATUS_TONES[account.status]}>{STATUS_LABELS[account.status]}</Pill>
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted">Ngày tạo</dt>
                <dd className="text-ink">{formatDate(account.createdAt)}</dd>
              </div>
            </dl>
            <Button
              icon={reopening ? "lock_open" : "block"}
              variant={reopening ? "primary" : isSelf ? "secondary" : "danger"}
              disabled={isSelf && !reopening}
              onClick={() => setPopup({ kind: "status" })}
            >
              {reopening ? "Mở lại tài khoản" : "Vô hiệu hóa tài khoản"}
            </Button>
          </Section>
          <Section title="Lịch sử">
            <Link
              href={`/admin/audit?actorEmail=${encodeURIComponent(account.email)}&actorId=${account.id}`}
              className="inline-flex items-center gap-1 text-[13px] font-semibold text-primary-ink hover:underline"
            >
              Mở Nhật ký audit của tài khoản
            </Link>
          </Section>
        </div>
      </div>

      {popup?.kind === "role" && (
        <RoleChangeDialog
          account={account}
          onClose={() => setPopup(null)}
          onChanged={(saved) => changed(saved, "Đã đổi vai trò")}
          onConflict={reload}
          onBlocked={(block) => setPopup({ kind: "blocked", block })}
        />
      )}
      {popup?.kind === "blocked" && <RoleChangeRejectedDialog account={account} block={popup.block} onClose={() => setPopup(null)} />}
      {popup?.kind === "status" && (
        <StatusToggleDialog
          account={account}
          onClose={() => setPopup(null)}
          onChanged={(saved) => saved.status === "DISABLED" ? changed(saved, "Đã vô hiệu hóa tài khoản", "danger") : changed(saved, "Đã mở lại tài khoản")}
          onConflict={reload}
        />
      )}
    </>
  );
}

function AccountEditForm({
  account,
  isSelf,
  onSaved,
  onConflict,
  onChangeRole,
}: {
  account: AccountDetail;
  isSelf: boolean;
  onSaved: (account: AccountDetail) => void;
  onConflict: () => Promise<void>;
  onChangeRole: () => void;
}) {
  const nameId = useId();
  const phoneId = useId();
  const emailId = useId();
  const roleId = useId();
  const [displayName, setDisplayName] = useState(account.displayName);
  const [phoneNumber, setPhoneNumber] = useState(account.phoneNumber ?? "");
  const [busy, setBusy] = useState(false);
  const [fieldError, setFieldError] = useState<{ name?: string; phone?: string }>({});
  const [error, setError] = useState<string | null>(null);
  const dirty = displayName !== account.displayName || phoneNumber !== (account.phoneNumber ?? "");

  function reset() {
    setDisplayName(account.displayName);
    setPhoneNumber(account.phoneNumber ?? "");
    setFieldError({});
    setError(null);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const errors = {
      name: displayName.trim().length === 0 ? "Nhập họ và tên." : undefined,
      phone: phoneNumber.trim() && !PHONE_FORMAT.test(phoneNumber.trim()) ? "Số điện thoại gồm 8–15 chữ số, có thể có dấu + ở đầu." : undefined,
    };
    setFieldError(errors);
    if (errors.name || errors.phone) return;
    setBusy(true);
    setError(null);
    try {
      onSaved(await updateAccount(account.id, displayName.trim(), phoneNumber.trim(), account.version));
    } catch (e) {
      const code = e instanceof ApiError ? e.problem.code : undefined;
      setError((code && ADMIN_ERROR_LABELS[code]) ?? "Không lưu được, vui lòng thử lại.");
      if (code === "VERSION_CONFLICT") await onConflict();
    } finally {
      setBusy(false);
    }
  }

  return (
    <Section title="Thông tin tài khoản">
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Họ và tên" htmlFor={nameId} required error={fieldError.name}>
            <TextInput id={nameId} value={displayName} invalid={!!fieldError.name} onChange={(e) => setDisplayName(e.target.value)} />
          </Field>
          <Field label="Số điện thoại (tùy chọn)" htmlFor={phoneId} error={fieldError.phone}>
            <TextInput
              id={phoneId}
              inputMode="tel"
              value={phoneNumber}
              invalid={!!fieldError.phone}
              onChange={(e) => setPhoneNumber(e.target.value)}
            />
          </Field>
          <Field label="Email trường" htmlFor={emailId}>
            <LockedInput id={emailId} value={account.email} className="min-w-0" />
          </Field>
          <Field label="Vai trò" htmlFor={roleId}>
            <div className="flex min-w-0 gap-2">
              <LockedInput id={roleId} value={ROLE_LABELS[account.role]} className="min-w-0 flex-1" />
              <IconButton icon="swap_horiz" label="Đổi vai trò" size={40} onClick={onChangeRole} disabled={isSelf}
                className="border border-line-strong bg-surface text-ink hover:bg-sunken" />
            </div>
          </Field>
        </div>
        {error && (
          <Alert tone="danger" role="alert">
            {error}
          </Alert>
        )}
        <div className="flex justify-end gap-2 border-t border-line pt-4">
          <Button onClick={reset} disabled={!dirty || busy}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" icon="save" disabled={!dirty || busy}>
            Lưu thay đổi
          </Button>
        </div>
      </form>
    </Section>
  );
}
