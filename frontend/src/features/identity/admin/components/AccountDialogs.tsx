"use client";

import Link from "next/link";
import { useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import type { Role } from "@/shared/auth/roles";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import { Field, Select } from "@/shared/ui/inputs";
import { TableFrame, Td, Th } from "@/shared/ui/Table";
import { ROLE_LABELS } from "../../model/types";
import { changeRole, changeStatus, type AccountDetail } from "../api";
import { ADMIN_ERROR_LABELS } from "../model/labels";
import { ROLES } from "./NewAccountDialog";
import { msg } from "@/i18n/msg";

/** Môn hoặc lớp còn giữ; `code`, `name` là null khi unit sở hữu chưa trả được (khi đó chỉ có ID). */
export type HeldAssignment = { id: string; code: string | null; name: string | null };

/** Phụ trách còn giữ khi backend chặn đổi vai trò (ROLE_CHANGE_BLOCKED, MSG29). */
export type RoleChangeBlock = { from: Role; to: Role; subjects: HeldAssignment[]; classes: HeldAssignment[] };

type BlockedProblem = {
  code?: string;
  managedSubjects?: string[];
  taughtClasses?: string[];
  subjects?: HeldAssignment[];
  classes?: HeldAssignment[];
};

/** Bản backend cũ chỉ trả ID: vẫn hiện được, không có mã và tên. */
function held(details: HeldAssignment[] | undefined, ids: string[] | undefined): HeldAssignment[] {
  return details ?? (ids ?? []).map((id) => ({ id, code: null, name: null }));
}

type DialogProps = {
  account: AccountDetail;
  onClose: () => void;
  onChanged: (account: AccountDetail) => void;
  /** VERSION_CONFLICT: trang tải lại tài khoản. */
  onConflict: () => Promise<void>;
};

function errorMessage(error: unknown) {
  const code = error instanceof ApiError ? error.problem.code : undefined;
  if (code && ADMIN_ERROR_LABELS[code]) return ADMIN_ERROR_LABELS[code];
  return (error instanceof ApiError && error.problem.detail) || msg("identity.admin.components.AccountDialogs.khongThucHienDuocVuiLong");
}

/** Không đổi vai trò vào hoặc ra khỏi Sinh viên (BR-U01-107): popup chỉ mở cho tài khoản khác sinh viên. */
const STAFF_ROLES = ROLES.filter((r) => r !== "STUDENT");

/**
 * Popup Đổi vai trò (UC 11, design `a04b`): một ô Vai trò, mặc định là vai trò hiện tại; chọn vai trò khác mới đổi được.
 * Không có lựa chọn Sinh viên. Bị chặn vì còn phụ trách thì chuyển sang `RoleChangeRejectedDialog`.
 */
export function RoleChangeDialog({ account, onClose, onChanged, onConflict, onBlocked }: DialogProps & { onBlocked: (block: RoleChangeBlock) => void }) {
  const roleId = useId();
  const [role, setRole] = useState<Role>(account.role);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      onChanged(await changeRole(account.id, role, account.version));
    } catch (e) {
      const problem = (e instanceof ApiError ? e.problem : {}) as BlockedProblem;
      if (problem.code === "ROLE_CHANGE_BLOCKED") {
        onBlocked({
          from: account.role,
          to: role,
          subjects: held(problem.subjects, problem.managedSubjects),
          classes: held(problem.classes, problem.taughtClasses),
        });
        return;
      }
      setError(errorMessage(e));
      if (problem.code === "VERSION_CONFLICT") await onConflict();
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog
      open
      title={msg("identity.admin.components.AccountDialogs.doiVaiTro")}
      icon="swap_horiz"
      onClose={onClose}
      footer={
        <>
          <Button variant="primary" onClick={confirm} disabled={busy || role === account.role}>
            {msg("identity.admin.components.AccountDialogs.doiVaiTro")}</Button>
        </>
      }
    >
      <Field label={msg("identity.admin.components.AccountDialogs.vaiTro")} htmlFor={roleId}>
        <Select id={roleId} value={role} onChange={(e) => setRole(e.target.value as Role)}>
          {STAFF_ROLES.map((r) => (
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
    </Dialog>
  );
}

/** Popup Không đổi được vai trò (design `a04c`): một câu ngắn và bảng Loại, Mã, Tên; môn có link tới trang môn. */
export function RoleChangeRejectedDialog({ account, block, onClose }: { account: AccountDetail; block: RoleChangeBlock; onClose: () => void }) {
  const items = [
    ...block.subjects.map((item) => ({ kind: msg("identity.admin.components.AccountDialogs.mon"), item, href: `/admin/subjects/${item.id}` })),
    ...block.classes.map((item) => ({ kind: msg("identity.admin.components.AccountDialogs.lop"), item, href: null })),
  ];
  return (
    <Dialog open title={msg("identity.admin.components.AccountDialogs.khongDoiDuocVaiTro")} icon="block" tone="danger" onClose={onClose}>
      <Alert tone="danger" role="alert">
        {account.displayName} {msg("identity.admin.components.AccountDialogs.dangGiuCacMonHoacLop")}</Alert>
      <TableFrame filledHead className="[&_table]:table-fixed">
        <colgroup><col className="w-[110px]" /><col className="w-[140px]" /><col /></colgroup><thead>
          <tr>
            <Th>{msg("identity.admin.components.AccountDialogs.loai")}</Th>
            <Th>{msg("identity.admin.components.AccountDialogs.ma")}</Th>
            <Th>{msg("identity.admin.components.AccountDialogs.ten")}</Th>
          </tr>
        </thead>
        <tbody>
          {items.map(({ kind, item, href }) => (
            <tr key={`${kind}-${item.id}`}>
              <Td>{kind}</Td>
              <Td>
                {item.code ? (
                  href ? <Link href={href} className="font-medium text-ink hover:underline">{item.code}</Link> : <span className="font-medium">{item.code}</span>
                ) : (
                  <span className="font-mono text-xs break-all text-muted" title={msg("identity.admin.components.AccountDialogs.chuaTaiDuocMa")}>{item.id}</span>
                )}
              </Td>
              <Td>{item.name ?? "—"}</Td>
            </tr>
          ))}
        </tbody>
      </TableFrame>
    </Dialog>
  );
}

/** Popup Vô hiệu hóa / Mở lại tài khoản (UC 12, design `a04d`). Không có ô lý do (F12). */
export function StatusToggleDialog({ account, onClose, onChanged, onConflict }: DialogProps) {
  const reopening = account.status === "DISABLED";
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      onChanged(await changeStatus(account.id, reopening ? "ACTIVE" : "DISABLED", account.version));
    } catch (e) {
      setError(errorMessage(e));
      if (e instanceof ApiError && e.problem.code === "VERSION_CONFLICT") await onConflict();
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog
      open
      title={reopening ? msg("identity.admin.components.AccountDialogs.moLaiTaiKhoan") : msg("identity.admin.components.AccountDialogs.voHieuHoaTaiKhoan")}
      icon={reopening ? "lock_open" : "block"}
      tone={reopening ? "primary" : "danger"}
      onClose={onClose}
      footer={
        <>
          <Button variant={reopening ? "primary" : "danger"} className="w-full" onClick={confirm} disabled={busy}>
            {reopening ? msg("identity.admin.components.AccountDialogs.moLai") : msg("identity.admin.components.AccountDialogs.voHieuHoa")}
          </Button>
        </>
      }
    >
      <p>{msg("identity.admin.components.AccountDialogs.banCoChacMuon")}{reopening ? msg("identity.admin.components.AccountDialogs.moLai2") : msg("identity.admin.components.AccountDialogs.voHieuHoa2")} {msg("identity.admin.components.AccountDialogs.taiKhoan")}{account.displayName} {msg("identity.admin.components.AccountDialogs.khong")}</p>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
    </Dialog>
  );
}
