"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { isValidStudentCode, normalizeStudentCode, STUDENT_CODE_FORMAT_MESSAGE } from "@/shared/studentCode";
import { Alert } from "@/shared/ui/Alert";
import { Button, IconButton } from "@/shared/ui/Button";
import { Field, TextInput } from "@/shared/ui/inputs";
import { LockedInput } from "@/shared/ui/LockedInput";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Pill } from "@/shared/ui/Pill";
import { Section } from "@/shared/ui/Section";
import { showToast, type ToastTone } from "@/shared/ui/Toast";
import { useSession } from "../../hooks/session";
import { ROLE_LABELS } from "../../model/types";
import { RoleChangeDialog, RoleChangeRejectedDialog, StatusToggleDialog, type RoleChangeBlock } from "../components/AccountDialogs";
import { getAccount, updateAccount, type AccountDetail } from "../api";
import { ADMIN_ERROR_LABELS, formatDate, STATUS_LABELS, STATUS_TONES } from "../model/labels";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

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
        if (active) setLoadError(e instanceof ApiError && e.problem.status === 404 ? msg("identity.admin.pages.AccountDetailPage.khongTimThayTaiKhoan") : msg("identity.admin.pages.AccountDetailPage.khongTaiDuocTaiKhoan"));
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
      setLoadError(msg("identity.admin.pages.AccountDetailPage.khongTaiLaiDuocTaiKhoan"));
    }
  }

  if (loadError) {
    return (
      <>
        <PageHeader title={msg("identity.admin.pages.AccountDetailPage.taiKhoan")} backHref="/admin/accounts" />
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
        title={account.displayName}
        breadcrumb={[{ label: msg("identity.admin.pages.AccountDetailPage.taiKhoan"), href: "/admin/accounts" }, { label: account.email }]}
      />
      <div className="grid items-start gap-4 lg:grid-cols-[1fr_280px]">
        <AccountEditForm
          key={account.version}
          account={account}
          isSelf={isSelf}
          onSaved={(saved) => changed(saved, msg("identity.admin.pages.AccountDetailPage.daLuuThayDoi"))}
          onConflict={reload}
          onChangeRole={() => setPopup({ kind: "role" })}
        />
        <div className="flex flex-col gap-4">
          <Section title={msg("identity.admin.pages.AccountDetailPage.trangThai")}>
            <dl className="flex flex-col gap-2.5 text-[13px]">
              <div className="flex items-center justify-between">
                <dt className="text-muted">{msg("identity.admin.pages.AccountDetailPage.trangThai")}</dt>
                <dd>
                  <Pill tone={STATUS_TONES[account.status]}>{STATUS_LABELS[account.status]}</Pill>
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted">{msg("identity.admin.pages.AccountDetailPage.ngayTao")}</dt>
                <dd className="text-ink">{formatDate(account.createdAt)}</dd>
              </div>
            </dl>
            <Button
              icon={reopening ? "lock_open" : "block"}
              variant={reopening ? "primary" : isSelf ? "secondary" : "danger"}
              disabled={isSelf && !reopening}
              onClick={() => setPopup({ kind: "status" })}
            >
              {reopening ? msg("identity.admin.pages.AccountDetailPage.moLaiTaiKhoan") : msg("identity.admin.pages.AccountDetailPage.voHieuHoaTaiKhoan")}
            </Button>
          </Section>
          <Section title={msg("identity.admin.pages.AccountDetailPage.lichSu")}>
            <Link
              href={`/admin/audit?actorEmail=${encodeURIComponent(account.email)}&actorId=${account.id}`}
              className="inline-flex items-center gap-1 text-[13px] font-semibold text-primary-ink hover:underline"
            >
              {msg("identity.admin.pages.AccountDetailPage.moNhatKyCuaTaiKhoan")}</Link>
          </Section>
        </div>
      </div>

      {popup?.kind === "role" && (
        <RoleChangeDialog
          account={account}
          onClose={() => setPopup(null)}
          onChanged={(saved) => changed(saved, msg("identity.admin.pages.AccountDetailPage.daDoiVaiTro"))}
          onConflict={reload}
          onBlocked={(block) => setPopup({ kind: "blocked", block })}
        />
      )}
      {popup?.kind === "blocked" && <RoleChangeRejectedDialog account={account} block={popup.block} onClose={() => setPopup(null)} />}
      {popup?.kind === "status" && (
        <StatusToggleDialog
          account={account}
          onClose={() => setPopup(null)}
          onChanged={(saved) => saved.status === "DISABLED" ? changed(saved, msg("identity.admin.pages.AccountDetailPage.daVoHieuHoaTaiKhoan"), "danger") : changed(saved, msg("identity.admin.pages.AccountDetailPage.daMoLaiTaiKhoan"))}
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
  const codeId = useId();
  const student = account.role === "STUDENT";
  const savedCode = account.studentCode ?? "";
  const [displayName, setDisplayName] = useState(account.displayName);
  const [phoneNumber, setPhoneNumber] = useState(account.phoneNumber ?? "");
  const [studentCode, setStudentCode] = useState(savedCode);
  const [busy, setBusy] = useState(false);
  const [fieldError, setFieldError] = useState<{ name?: string; phone?: string; studentCode?: string }>({});
  const [error, setError] = useState<string | null>(null);
  const codeChanged = account.studentCodeEditable && normalizeStudentCode(studentCode) !== savedCode;
  const dirty = displayName !== account.displayName || phoneNumber !== (account.phoneNumber ?? "") || codeChanged;

  function reset() {
    setDisplayName(account.displayName);
    setPhoneNumber(account.phoneNumber ?? "");
    setStudentCode(savedCode);
    setFieldError({});
    setError(null);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const code = normalizeStudentCode(studentCode);
    const errors = {
      name: displayName.trim().length === 0 ? msg("identity.admin.pages.AccountDetailPage.nhapHoVaTen") : undefined,
      phone: phoneNumber.trim() && !PHONE_FORMAT.test(phoneNumber.trim()) ? msg("identity.admin.pages.AccountDetailPage.soDienThoaiGom815") : undefined,
      // Đã có mã thì không được xóa trống (BR-U01-105).
      studentCode: !codeChanged ? undefined
        : !code ? msg("identity.admin.pages.AccountDetailPage.khongXoaTrongDuocMssvDa")
          : isValidStudentCode(code) ? undefined : STUDENT_CODE_FORMAT_MESSAGE,
    };
    setFieldError(errors);
    if (errors.name || errors.phone || errors.studentCode) return;
    setBusy(true);
    setError(null);
    try {
      onSaved(await updateAccount(account.id, displayName.trim(), phoneNumber.trim(), account.version,
        codeChanged ? code : undefined));
    } catch (e) {
      const code = e instanceof ApiError ? e.problem.code : undefined;
      setError((code && ADMIN_ERROR_LABELS[code]) ?? msg("identity.admin.pages.AccountDetailPage.khongLuuDuocVuiLongThu"));
      if (code === "VERSION_CONFLICT") await onConflict();
    } finally {
      setBusy(false);
    }
  }

  return (
    <Section title={msg("identity.admin.pages.AccountDetailPage.thongTinTaiKhoan")}>
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={msg("identity.admin.pages.AccountDetailPage.hoVaTen")} htmlFor={nameId} required error={fieldError.name}>
            <TextInput id={nameId} value={displayName} invalid={!!fieldError.name} onChange={(e) => setDisplayName(e.target.value)} />
          </Field>
          <Field label={msg("identity.admin.pages.AccountDetailPage.soDienThoaiTuyChon")} htmlFor={phoneId} error={fieldError.phone}>
            <TextInput
              id={phoneId}
              inputMode="tel"
              value={phoneNumber}
              invalid={!!fieldError.phone}
              onChange={(e) => setPhoneNumber(e.target.value)}
            />
          </Field>
          <Field label={msg("identity.admin.pages.AccountDetailPage.emailTruong")} htmlFor={emailId}>
            <LockedInput id={emailId} value={account.email} className="min-w-0" />
          </Field>
          <div className="flex min-w-0 flex-col gap-1">
            <Field label={msg("identity.admin.pages.AccountDetailPage.vaiTro")} htmlFor={roleId}>
              <div className="flex min-w-0 gap-2">
                <LockedInput id={roleId} value={ROLE_LABELS[account.role]} className="min-w-0 flex-1" />
                {!student && (
                  <IconButton icon="swap_horiz" label={msg("identity.admin.pages.AccountDetailPage.doiVaiTro")} size={40} onClick={onChangeRole} disabled={isSelf}
                    className="border border-line-strong bg-surface text-ink hover:bg-sunken" />
                )}
              </div>
            </Field>
            {/* BR-U01-107: không đổi vai trò vào hoặc ra khỏi Sinh viên. */}
            {student && <p className="text-xs text-muted">{msg("identity.admin.pages.AccountDetailPage.khongDoiDuocVaiTroCua")}</p>}
          </div>
          {student && (
            <div className="flex min-w-0 flex-col gap-1">
              <Field label={msg("identity.admin.pages.AccountDetailPage.maSoSinhVien")} htmlFor={codeId} error={fieldError.studentCode}>
                {account.studentCodeEditable ? (
                  <TextInput
                    id={codeId}
                    value={studentCode}
                    invalid={!!fieldError.studentCode}
                    placeholder="SE123456"
                    autoComplete="off"
                    data-testid="account-detail-student-code-input"
                    onChange={(e) => setStudentCode(e.target.value)}
                    onBlur={() => setStudentCode(normalizeStudentCode(studentCode))}
                  />
                ) : (
                  <LockedInput id={codeId} value={savedCode || msg("identity.admin.pages.AccountDetailPage.chuaCoMssv")} className="min-w-0" />
                )}
              </Field>
              <p className="text-xs text-muted" data-testid="account-detail-student-code-hint">
                {account.studentCodeEditable ? msg("identity.admin.pages.AccountDetailPage.chiSuaDuocKhiSinhVien")
                  : msg("identity.admin.pages.AccountDetailPage.mssvKhongDoiDuocSauKhi")}
              </p>
            </div>
          )}
        </div>
        {error && (
          <Alert tone="danger" role="alert">
            {error}
          </Alert>
        )}
        <div className="flex justify-end gap-2 border-t border-line pt-4">
          <Button onClick={reset} disabled={!dirty || busy}>
            {msg("identity.admin.pages.AccountDetailPage.huy")}</Button>
          <Button type="submit" variant="primary" icon="save" disabled={!dirty || busy}>
            {msg("identity.admin.pages.AccountDetailPage.luuThayDoi")}</Button>
        </div>
      </form>
    </Section>
  );
}
