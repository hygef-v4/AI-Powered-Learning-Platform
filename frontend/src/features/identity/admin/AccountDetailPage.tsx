"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useId, useState, type FormEvent, type ReactNode } from "react";
import { ApiError } from "@/shared/api/client";
import type { Role } from "@/shared/auth/roles";
import { useSession } from "../session";
import { ROLE_LABELS } from "../types";
import { changeRole, changeStatus, getAccount, updateAccount, type AccountDetail } from "./api";
import { ConfirmDialog } from "./ConfirmDialog";
import { ADMIN_ERROR_LABELS, formatDateTime, STATUS_LABELS, STATUS_TONES } from "./labels";

const ROLES: Role[] = ["STUDENT", "TEACHER", "SUBJECT_MANAGER", "ADMIN"];
const PHONE_FORMAT = /^\+?[0-9]{8,15}$/;

/** Thông báo lỗi nghiệp vụ; ROLE_CHANGE_BLOCKED kèm danh sách môn/lớp còn phụ trách (MSG29). */
function describeError(error: unknown): ReactNode {
  if (!(error instanceof ApiError)) return "Không thực hiện được, vui lòng thử lại.";
  const problem = error.problem as { code?: string; detail?: string; managedSubjects?: string[]; taughtClasses?: string[] };
  const message = (problem.code && ADMIN_ERROR_LABELS[problem.code]) ?? problem.detail ?? "Không thực hiện được.";
  if (problem.code !== "ROLE_CHANGE_BLOCKED") return message;
  return (
    <>
      {message}
      <ul className="mt-1 list-disc pl-5 text-xs">
        {(problem.managedSubjects ?? []).map((id) => (
          <li key={id}>Chủ nhiệm môn: {id}</li>
        ))}
        {(problem.taughtClasses ?? []).map((id) => (
          <li key={id}>Giảng viên lớp: {id}</li>
        ))}
      </ul>
    </>
  );
}

/**
 * Account Detail (UC 10–12): xem thông tin; sửa họ tên, số điện thoại kèm version; đổi vai trò và vô hiệu hóa/mở lại
 * qua popup riêng (Q10). Email chỉ đọc; không có nút gửi OTP hay đặt mật khẩu.
 */
export function AccountDetailPage() {
  const { id } = useParams<{ id: string }>();
  const session = useSession();
  const [account, setAccount] = useState<AccountDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

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

  async function reload() {
    setAccount(await getAccount(id));
  }

  if (loadError) {
    return (
      <main className="mx-auto max-w-4xl p-6">
        <p role="alert" className="text-sm text-red-600">
          {loadError}
        </p>
      </main>
    );
  }
  if (!account) return <p className="p-8 text-center text-sm text-slate-500">Đang tải...</p>;

  const isSelf = session.status === "authenticated" && session.profile.accountId === account.id;

  return (
    <main className="mx-auto max-w-4xl p-6">
      <Link href="/admin/accounts" className="text-sm text-slate-500 hover:text-slate-700">
        ← Danh sách tài khoản
      </Link>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-semibold text-slate-900">{account.displayName}</h1>
        <span className={`rounded-md px-2 py-0.5 text-xs font-medium ${STATUS_TONES[account.status]}`}>
          {STATUS_LABELS[account.status]}
        </span>
      </div>
      <p className="mt-1 text-sm text-slate-500">
        Tạo {formatDateTime(account.createdAt)} · Cập nhật {formatDateTime(account.updatedAt)}
      </p>
      {notice && (
        <p role="status" className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {notice}
        </p>
      )}

      <AccountEditForm
        key={account.version}
        account={account}
        onSaved={(saved) => {
          setAccount(saved);
          setNotice("Đã lưu thay đổi.");
        }}
        onConflict={reload}
      />

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <RoleSection
          account={account}
          isSelf={isSelf}
          onChanged={(saved) => {
            setAccount(saved);
            setNotice("Đã đổi vai trò. Người dùng sẽ phải đăng nhập lại.");
          }}
          onConflict={reload}
        />
        <StatusSection
          account={account}
          isSelf={isSelf}
          onChanged={(saved) => {
            setAccount(saved);
            setNotice(saved.status === "DISABLED" ? "Đã vô hiệu hóa tài khoản." : "Đã mở lại tài khoản.");
          }}
          onConflict={reload}
        />
      </div>
    </main>
  );
}

function AccountEditForm({
  account,
  onSaved,
  onConflict,
}: {
  account: AccountDetail;
  onSaved: (account: AccountDetail) => void;
  onConflict: () => Promise<void>;
}) {
  const nameId = useId();
  const phoneId = useId();
  const [displayName, setDisplayName] = useState(account.displayName);
  const [phoneNumber, setPhoneNumber] = useState(account.phoneNumber ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ReactNode>(null);
  const dirty = displayName !== account.displayName || phoneNumber !== (account.phoneNumber ?? "");

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (displayName.trim().length === 0) {
      setError("Nhập họ và tên.");
      return;
    }
    if (phoneNumber.trim() && !PHONE_FORMAT.test(phoneNumber.trim())) {
      setError("Số điện thoại gồm 8–15 chữ số, có thể có dấu + ở đầu.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      onSaved(await updateAccount(account.id, displayName.trim(), phoneNumber.trim(), account.version));
    } catch (e) {
      setError(describeError(e));
      if (e instanceof ApiError && e.problem.code === "VERSION_CONFLICT") await onConflict();
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
      <h2 className="font-semibold text-slate-900">Thông tin tài khoản</h2>
      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor={nameId} className="text-xs font-semibold text-slate-700">
            Họ và tên
          </label>
          <input
            id={nameId}
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
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
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
        <div className="sm:col-span-2">
          <p className="text-xs font-semibold text-slate-700">Email trường</p>
          <p className="mt-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600" aria-readonly="true">
            🔒 {account.email}
          </p>
        </div>
      </div>
      {error && (
        <div role="alert" className="mt-3 text-sm text-red-600">
          {error}
        </div>
      )}
      <div className="mt-4 flex justify-end">
        <button
          type="submit"
          disabled={!dirty || busy}
          className="rounded-lg bg-[#1f2a6b] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
        >
          Lưu thay đổi
        </button>
      </div>
    </form>
  );
}

function RoleSection({
  account,
  isSelf,
  onChanged,
  onConflict,
}: {
  account: AccountDetail;
  isSelf: boolean;
  onChanged: (account: AccountDetail) => void;
  onConflict: () => Promise<void>;
}) {
  const selectId = useId();
  const [role, setRole] = useState<Role>(account.role);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ReactNode>(null);

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      onChanged(await changeRole(account.id, role, account.version));
      setConfirming(false);
    } catch (e) {
      setError(describeError(e));
      if (e instanceof ApiError && e.problem.code === "VERSION_CONFLICT") await onConflict();
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <h2 className="font-semibold text-slate-900">Vai trò</h2>
      <label htmlFor={selectId} className="mt-3 block text-xs font-semibold text-slate-700">
        Vai trò cao nhất
      </label>
      <select
        id={selectId}
        value={role}
        disabled={isSelf}
        onChange={(e) => setRole(e.target.value as Role)}
        className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50"
      >
        {ROLES.map((r) => (
          <option key={r} value={r}>
            {ROLE_LABELS[r]}
          </option>
        ))}
      </select>
      {isSelf && <p className="mt-2 text-xs text-slate-500">Không thể tự đổi vai trò của chính mình.</p>}
      <button
        type="button"
        disabled={isSelf || role === account.role}
        onClick={() => {
          setError(null);
          setConfirming(true);
        }}
        className="mt-4 rounded-lg border border-slate-300 px-4 py-2 text-sm disabled:opacity-50"
      >
        Đổi vai trò
      </button>
      {confirming && (
        <ConfirmDialog
          title="Đổi vai trò?"
          confirmLabel="Đổi vai trò"
          busy={busy}
          error={error}
          onConfirm={confirm}
          onCancel={() => setConfirming(false)}
        >
          Đổi {account.displayName} từ {ROLE_LABELS[account.role]} sang {ROLE_LABELS[role]}. Người dùng sẽ bị đăng xuất
          khỏi mọi thiết bị và phải đăng nhập lại.
        </ConfirmDialog>
      )}
    </section>
  );
}

function StatusSection({
  account,
  isSelf,
  onChanged,
  onConflict,
}: {
  account: AccountDetail;
  isSelf: boolean;
  onChanged: (account: AccountDetail) => void;
  onConflict: () => Promise<void>;
}) {
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ReactNode>(null);
  const disabled = account.status === "DISABLED";

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      onChanged(await changeStatus(account.id, disabled ? "ACTIVE" : "DISABLED", account.version));
      setConfirming(false);
    } catch (e) {
      setError(describeError(e));
      if (e instanceof ApiError && e.problem.code === "VERSION_CONFLICT") await onConflict();
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <h2 className="font-semibold text-slate-900">Trạng thái</h2>
      <p className="mt-3 text-sm text-slate-600">{STATUS_LABELS[account.status]}</p>
      {isSelf && <p className="mt-2 text-xs text-slate-500">Không thể tự vô hiệu hóa chính mình.</p>}
      <button
        type="button"
        disabled={isSelf && !disabled}
        onClick={() => {
          setError(null);
          setConfirming(true);
        }}
        className={`mt-4 rounded-lg px-4 py-2 text-sm font-semibold disabled:opacity-50 ${
          disabled ? "border border-slate-300 text-slate-700" : "bg-red-600 text-white"
        }`}
      >
        {disabled ? "Mở lại" : "Vô hiệu hóa"}
      </button>
      {confirming && (
        <ConfirmDialog
          title={disabled ? "Mở lại tài khoản?" : "Vô hiệu hóa tài khoản?"}
          confirmLabel={disabled ? "Mở lại" : "Vô hiệu hóa"}
          danger={!disabled}
          busy={busy}
          error={error}
          onConfirm={confirm}
          onCancel={() => setConfirming(false)}
        >
          {disabled
            ? "Tài khoản đã từng kích hoạt sẽ hoạt động lại; tài khoản chưa kích hoạt sẽ về trạng thái chờ kích hoạt."
            : `${account.displayName} sẽ bị đăng xuất khỏi mọi thiết bị và không đăng nhập được. Dữ liệu và lịch sử được giữ nguyên.`}
        </ConfirmDialog>
      )}
    </section>
  );
}
