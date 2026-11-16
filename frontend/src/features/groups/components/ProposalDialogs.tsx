"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Field, TextInput, cx } from "@/shared/ui";
import { MAX_PROPOSAL_NAME, validateProposalName } from "../model/proposals";
import type { CandidateView } from "../model/types";
import { matchesPersonQuery } from "@/shared/studentCode";
import { PersonSubline } from "@/shared/ui/PersonSubline";
import { msg } from "@/i18n/msg";

/** Popup đặt tên nhóm khi tạo hoặc đổi tên đề xuất (BR-U12-75, 76): bắt buộc, 1–100 ký tự sau khi cắt khoảng trắng. */
export function ProposalNameDialog({ title, initial = "", confirmLabel, onClose, onSave }: {
  title: string;
  initial?: string;
  confirmLabel: string;
  onClose: () => void;
  onSave: (name: string) => Promise<string | null>;
}) {
  const fieldId = useId();
  const [value, setValue] = useState(initial);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const saveProposal = async () => {
    const checked = validateProposalName(value);
    if (checked.error) {
      setError(checked.error);
      return;
    }
    setBusy(true);
    const failed = await onSave(checked.name);
    setBusy(false);
    if (failed) setError(failed);
  };
  return (
    <Dialog open title={title} onClose={onClose} dismissible={!busy}
      footer={<Button variant="primary" disabled={busy} onClick={() => void saveProposal()} data-testid="proposal-name-save">{confirmLabel}</Button>}>
      <form onSubmit={(event) => { event.preventDefault(); void saveProposal(); }}>
        <Field label={msg("groups.components.ProposalDialogs.tenNhom")} htmlFor={fieldId} required error={error}>
          <TextInput id={fieldId} value={value} maxLength={MAX_PROPOSAL_NAME} invalid={!!error} autoFocus disabled={busy}
            onChange={(event) => { setValue(event.target.value); setError(null); }} data-testid="proposal-name-input" />
        </Field>
        <p className="mt-1 text-right text-xs text-muted">{value.trim().length}/{MAX_PROPOSAL_NAME}</p>
      </form>
    </Dialog>
  );
}

/**
 * Popup mời bạn cùng lớp (BR-U12-77, 92): danh sách sinh viên chưa có nhóm, tìm theo tên, email hoặc MSSV, chọn nhiều tới khi hết
 * chỗ. Người đang ở đề xuất khác vẫn mời được, kèm ghi chú họ phải rời đề xuất đó trước khi chấp nhận.
 */
export function InviteMembersDialog({ candidates, freeSeats, onClose, onInvite }: {
  candidates: CandidateView[];
  freeSeats: number;
  onClose: () => void;
  onInvite: (accountIds: string[]) => Promise<string | null>;
}) {
  const [query, setQuery] = useState("");
  const [picked, setPicked] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const q = query.toLocaleLowerCase("vi").trim();
  const shown = candidates.filter((c) => matchesPersonQuery(c, q));
  const full = picked.length >= freeSeats;
  const toggle = (accountId: string) => {
    setError(null);
    setPicked((current) => (current.includes(accountId)
      ? current.filter((id) => id !== accountId)
      : current.length >= freeSeats ? current : [...current, accountId]));
  };
  const invite = async () => {
    setBusy(true);
    const failed = await onInvite(picked);
    setBusy(false);
    if (failed) setError(failed);
  };
  return (
    <Dialog open title={msg("groups.components.ProposalDialogs.moiBanVaoNhom")} size="lg" onClose={onClose} dismissible={!busy}
      footer={
        <Button variant="primary" icon="person_add" disabled={busy || picked.length === 0} onClick={() => void invite()}
          data-testid="invite-members-submit">
          {picked.length > 0 ? msg("groups.components.ProposalDialogs.moiNguoi", { length: String(picked.length) }) : msg("groups.components.ProposalDialogs.moi")}
        </Button>
      }>
      <p className="text-[13px] text-muted">{msg("groups.components.ProposalDialogs.con")}{freeSeats} {msg("groups.components.ProposalDialogs.choNguoiDuocMoiCanChap")}</p>
      <TextInput type="search" icon="search" aria-label={msg("groups.components.ProposalDialogs.timTheoTenEmailHoacMssv")} placeholder={msg("groups.components.ProposalDialogs.timTheoTenEmailHoacMssv")}
        value={query} onChange={(event) => setQuery(event.target.value)} />
      {shown.length === 0 ? (
        <p className="py-6 text-center text-[13px] text-muted">{msg("groups.components.ProposalDialogs.khongCoSinhVienPhuHop")}</p>
      ) : (
        <ul aria-label={msg("groups.components.ProposalDialogs.sinhVienChuaCoNhom")} className="flex max-h-[50vh] flex-col gap-1 overflow-y-auto">
          {shown.map((c) => {
            const checked = picked.includes(c.accountId);
            const disabled = busy || (!checked && full);
            return (
              <li key={c.accountId}>
                <label className={cx("flex min-h-11 cursor-pointer items-center gap-3 rounded-lg px-3 py-2",
                  checked ? "bg-primary-soft" : "hover:bg-shell", disabled && "cursor-not-allowed opacity-60")}>
                  <input type="checkbox" className="size-4 accent-[var(--c-primary)]" checked={checked} disabled={disabled}
                    onChange={() => toggle(c.accountId)} data-testid={`invite-candidate-${c.accountId}`} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[13.5px] font-semibold text-ink">{c.name ?? c.email ?? msg("groups.components.ProposalDialogs.chuaRoTen")}</span>
                    <PersonSubline className="truncate" studentCode={c.studentCode} email={c.email} />
                  </span>
                  {c.inOtherProposal && <span className="shrink-0 text-xs text-muted">{msg("groups.components.ProposalDialogs.dangODeXuatKhac")}</span>}
                </label>
              </li>
            );
          })}
        </ul>
      )}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
    </Dialog>
  );
}
