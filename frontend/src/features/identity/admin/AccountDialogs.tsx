"use client";

import Link from "next/link";
import { useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import type { Role } from "@/shared/auth/roles";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import { Field, Select, TextInput } from "@/shared/ui/inputs";
import { TableFrame, Td, Th } from "@/shared/ui/Table";
import { ROLE_LABELS } from "../types";
import { changeRole, changeStatus, type AccountDetail } from "./api";
import { ADMIN_ERROR_LABELS } from "./labels";
import { ROLES } from "./NewAccountDialog";

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
  return (error instanceof ApiError && error.problem.detail) || "Không thực hiện được, vui lòng thử lại.";
}

function Subtitle({ account }: { account: AccountDetail }) {
  return (
    <p className="-mt-1 text-xs text-muted">
      {account.displayName} · {account.email}
    </p>
  );
}

/** Popup Đổi vai trò (UC 11, design `a04b`). Bị chặn vì còn phụ trách thì chuyển sang `RoleChangeRejectedDialog`. */
export function RoleChangeDialog({ account, onClose, onChanged, onConflict, onBlocked }: DialogProps & { onBlocked: (block: RoleChangeBlock) => void }) {
  const currentId = useId();
  const nextId = useId();
  const [role, setRole] = useState<Role>(ROLES.find((r) => r !== account.role)!);
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
      title="Đổi vai trò"
      icon="swap_horiz"
      onClose={onClose}
      footer={
        <>
          <Button variant="primary" onClick={confirm} disabled={busy}>
            Đổi vai trò
          </Button>
        </>
      }
    >
      <Subtitle account={account} />
      <Field label="Vai trò hiện tại" htmlFor={currentId}>
        <TextInput id={currentId} value={ROLE_LABELS[account.role]} readOnly wrapperClassName="bg-shell" className="text-muted" />
      </Field>
      <Field label="Vai trò mới" htmlFor={nextId} required>
        <Select id={nextId} value={role} onChange={(e) => setRole(e.target.value as Role)}>
          {ROLES.filter((r) => r !== account.role).map((r) => (
            <option key={r} value={r}>
              {ROLE_LABELS[r]}
            </option>
          ))}
        </Select>
      </Field>
      <Alert tone="warning">Người dùng sẽ bị đăng xuất khỏi mọi thiết bị và phải đăng nhập lại với quyền mới.</Alert>
      <p className="text-xs text-muted">Không hạ quyền được chính mình.</p>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
    </Dialog>
  );
}

/** Popup Không đổi được vai trò (design `a04c`): liệt kê môn, lớp còn giữ theo mã và tên; môn có link tới trang môn. */
export function RoleChangeRejectedDialog({ account, block, onClose }: { account: AccountDetail; block: RoleChangeBlock; onClose: () => void }) {
  const items = [
    ...block.subjects.map((item) => ({ kind: "Môn", item, href: `/admin/subjects/${item.id}`, held: ROLE_LABELS.SUBJECT_MANAGER })),
    ...block.classes.map((item) => ({ kind: "Lớp", item, href: null, held: ROLE_LABELS.TEACHER })),
  ];
  return (
    <Dialog open title="Không đổi được vai trò" icon="block" tone="danger" onClose={onClose} footer={<Button onClick={onClose}>Đóng</Button>}>
      <Subtitle account={account} />
      <Alert tone="danger" role="alert">
        {account.displayName} đang giữ các môn và lớp dưới đây. Hãy chuyển cho người khác trước khi đổi vai trò{" "}
        {ROLE_LABELS[block.from]} → {ROLE_LABELS[block.to]}.
      </Alert>
      <TableFrame>
        <thead>
          <tr>
            <Th>Loại</Th>
            <Th>Mã</Th>
            <Th>Tên</Th>
            <Th>Vai trò đang giữ</Th>
          </tr>
        </thead>
        <tbody>
          {items.map(({ kind, item, href, held: role }) => (
            <tr key={`${kind}-${item.id}`}>
              <Td>{kind}</Td>
              <Td>
                {item.code ? (
                  href ? <Link href={href} className="font-medium text-primary-ink hover:underline">{item.code}</Link> : <span className="font-medium">{item.code}</span>
                ) : (
                  <span className="font-mono text-xs break-all text-muted" title="Chưa tải được mã">{item.id}</span>
                )}
              </Td>
              <Td>{item.name ?? "—"}</Td>
              <Td>{role}</Td>
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
      title={reopening ? "Mở lại tài khoản?" : "Vô hiệu hóa tài khoản?"}
      icon={reopening ? "lock_open" : "block"}
      tone={reopening ? "primary" : "danger"}
      onClose={onClose}
      footer={
        <>
          <Button variant={reopening ? "primary" : "danger"} onClick={confirm} disabled={busy}>
            {reopening ? "Mở lại" : "Vô hiệu hóa"}
          </Button>
        </>
      }
    >
      {reopening ? (
        <p>
          <b className="text-ink">{account.displayName}</b> đăng nhập lại được. Tài khoản đã từng kích hoạt về Hoạt động; tài
          khoản chưa kích hoạt về Chờ kích hoạt.
        </p>
      ) : (
        <>
          <p>
            <b className="text-ink">{account.displayName}</b> sẽ bị đăng xuất và không đăng nhập được cho tới khi được mở lại.
            Dữ liệu bài làm vẫn được giữ.
          </p>
          <p className="text-xs text-muted">
            Không áp dụng cho chính mình. Nếu đây là Quản trị viên cuối cùng đang hoạt động, hệ thống từ chối và hiện lý do.
          </p>
        </>
      )}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
    </Dialog>
  );
}
