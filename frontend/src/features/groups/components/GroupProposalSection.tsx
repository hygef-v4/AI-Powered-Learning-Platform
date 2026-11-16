"use client";

import { formatStudentLabel } from "@/shared/studentCode";
import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Card, cx, Icon, Pill, showToast, useConfirmDialog } from "@/shared/ui";
import { extractInitials } from "@/shared/layout/navigation";
import { PersonSubline } from "@/shared/ui/PersonSubline";
import {
  answerProposal,
  changeProposal,
  createProposal,
  getMyProposal,
  inviteToProposal,
  renameProposal,
  revokeInvitation,
} from "../api";
import {
  canSubmit,
  describeFormation,
  isVersionConflict,
  PROPOSAL_STATUS_LABELS,
  describeProposalError,
} from "../model/proposals";
import type { FormationSummary, InvitationView, MyProposalView, ProposalMemberView, ProposalView } from "../model/types";
import { InviteMembersDialog, ProposalNameDialog } from "./ProposalDialogs";
import { msg } from "@/i18n/msg";

type Open = { kind: "create" } | { kind: "rename" } | { kind: "invite" } | null;

/** Màn đề xuất của sinh viên chưa có nhóm; `null` khi đang tải hoặc không tải được. */
export function useMyProposal(classId: string) {
  const [view, setView] = useState<MyProposalView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const reload = useCallback(
    () => getMyProposal(classId).then((loaded) => { setView(loaded); setError(null); },
      () => setError(msg("groups.components.GroupProposalSection.khongTaiDuocDeXuatNhom"))),
    [classId],
  );
  useEffect(() => {
    let active = true;
    getMyProposal(classId).then(
      (loaded) => { if (active) setView(loaded); },
      () => { if (active) setError(msg("groups.components.GroupProposalSection.khongTaiDuocDeXuatNhom")); },
    );
    return () => { active = false; };
  }, [classId]);
  return { view, setView, error, reload };
}

/**
 * Sinh viên chưa có nhóm tự lập nhóm (US-GRP-008, 009): trạng thái đợt, đề xuất bị từ chối gần nhất, lời mời đến mình, đề xuất
 * đang tham gia. Mọi thao tác trả lại màn mới nhất; đề xuất vừa đổi ở chỗ khác (409) thì tải lại và báo. Sinh viên vừa được
 * xếp vào nhóm chính thức thì gọi `onGrouped` để trang tải lại nhóm.
 */
export function GroupProposalSection({ classId, onGrouped }: { classId: string; onGrouped: () => void }) {
  const { view, setView, error, reload } = useMyProposal(classId);
  const [open, setOpen] = useState<Open>(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [confirmDialog, confirm] = useConfirmDialog();

  useEffect(() => {
    if (view?.grouped) onGrouped();
  }, [view?.grouped, onGrouped]);

  if (error && !view) {
    return <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>{msg("groups.components.GroupProposalSection.taiLai")}</Button>}>{error}</Alert>;
  }
  if (!view) return <p role="status" className="text-[13px] text-muted">{msg("groups.components.GroupProposalSection.dangTaiDeXuatNhom")}</p>;
  if (view.grouped) return null;

  /** Chạy một thao tác; trả lời lỗi để popup hiện tại chỗ, hoặc null khi xong. */
  const runAction = async (action: () => Promise<MyProposalView>, done?: string): Promise<string | null> => {
    setBusy(true);
    setActionError(null);
    try {
      setView(await action());
      setOpen(null);
      if (done) showToast(done);
      return null;
    } catch (e) {
      if (isVersionConflict(e)) void reload();
      return describeProposalError(e);
    } finally {
      setBusy(false);
    }
  };
  const runInline = async (action: () => Promise<MyProposalView>, done?: string) => {
    const failed = await runAction(action, done);
    if (failed) setActionError(failed);
  };

  const { formation, proposal } = view;
  return (
    <div className="flex flex-col gap-4" data-testid="group-proposal-section">
      <FormationBanner formation={formation} />
      {view.lastRejected && !proposal && (
        <Alert tone="warning" title={msg("groups.components.GroupProposalSection.deXuatBiTuChoi", { name: String(view.lastRejected.name) })}>
          {view.lastRejected.reason ? msg("groups.components.GroupProposalSection.lyDo", { reason: String(view.lastRejected.reason) }) : msg("groups.components.GroupProposalSection.giangVienKhongGhiLyDo")} {msg("groups.components.GroupProposalSection.banCoTheLapHoacVao")}</Alert>
      )}
      {actionError && <Alert tone="danger" role="alert">{actionError}</Alert>}
      {proposal ? (
        <ProposalCard proposal={proposal} formation={formation} busy={busy} candidatesCount={view.candidates.length}
          onRename={() => setOpen({ kind: "rename" })}
          onInvite={() => setOpen({ kind: "invite" })}
          onRevoke={(accountId) => void runInline(() => revokeInvitation(proposal.id, accountId, proposal.version))}
          onSubmit={() => void runInline(() => changeProposal(proposal.id, "submit", proposal.version), msg("groups.components.GroupProposalSection.daGuiDeXuatChoGiang"))}
          onWithdraw={() => void runInline(() => changeProposal(proposal.id, "withdraw", proposal.version), msg("groups.components.GroupProposalSection.daRutDeXuatVeNhap"))}
          onCancel={() => confirm(msg("groups.components.GroupProposalSection.huyDeXuat", { name: String(proposal.name) }),
            () => void runInline(() => changeProposal(proposal.id, "cancel", proposal.version), msg("groups.components.GroupProposalSection.daHuyDeXuat")), msg("groups.components.GroupProposalSection.huyDeXuat2"))}
          onLeave={() => confirm(msg("groups.components.GroupProposalSection.roiDeXuat", { name: String(proposal.name) }),
            () => void runInline(() => answerProposal(proposal.id, "leave"), msg("groups.components.GroupProposalSection.daRoiDeXuat")), msg("groups.components.GroupProposalSection.roiDeXuat2"))} />
      ) : formation.open && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-dashed border-line-strong bg-surface p-5">
          <p className="text-[13.5px] text-ink">{msg("groups.components.GroupProposalSection.lapNhomRoiMoiBanCung")}</p>
          <Button variant="primary" icon="add" disabled={busy} onClick={() => setOpen({ kind: "create" })} data-testid="proposal-create">
            {msg("groups.components.GroupProposalSection.taoDeXuatNhom")}</Button>
        </div>
      )}
      {view.invitations.length > 0 && (
        <InvitationList invitations={view.invitations} busy={busy} inProposal={!!proposal}
          onAccept={(id) => void runInline(() => answerProposal(id, "accept"), msg("groups.components.GroupProposalSection.daVaoNhom"))}
          onDecline={(id) => void runInline(() => answerProposal(id, "decline"), msg("groups.components.GroupProposalSection.daTuChoiLoiMoi"))} />
      )}
      {!proposal && !formation.enabled && view.invitations.length === 0 && (
        <Alert tone="info">{msg("groups.components.GroupProposalSection.banChuaCoNhomTrongLop")}</Alert>
      )}

      {open?.kind === "create" && (
        <ProposalNameDialog title={msg("groups.components.GroupProposalSection.taoDeXuatNhom")} confirmLabel={msg("groups.components.GroupProposalSection.tao")} onClose={() => setOpen(null)}
          onSave={(name) => runAction(() => createProposal(classId, name), msg("groups.components.GroupProposalSection.daTaoDeXuatNhom"))} />
      )}
      {open?.kind === "rename" && proposal && (
        <ProposalNameDialog title={msg("groups.components.GroupProposalSection.doiTenNhom")} initial={proposal.name} confirmLabel={msg("groups.components.GroupProposalSection.luu")} onClose={() => setOpen(null)}
          onSave={(name) => runAction(() => renameProposal(proposal.id, name, proposal.version), msg("groups.components.GroupProposalSection.daDoiTenNhom"))} />
      )}
      {open?.kind === "invite" && proposal && (
        <InviteMembersDialog candidates={view.candidates} freeSeats={proposal.freeSeats} onClose={() => setOpen(null)}
          onInvite={(ids) => runAction(() => inviteToProposal(proposal.id, ids, proposal.version), msg("groups.components.GroupProposalSection.daGuiLoiMoi"))} />
      )}
      {confirmDialog}
    </div>
  );
}

/** Trạng thái đợt tự chia nhóm: sĩ số, hạn chót hoặc lý do đóng. */
export function FormationBanner({ formation }: { formation: FormationSummary }) {
  if (!formation.enabled) return null;
  return (
    <Alert tone={formation.open ? "info" : "warning"} title={msg("groups.components.GroupProposalSection.tuChiaNhom")} role="status">
      {describeFormation(formation)}
      {!formation.open && msg("groups.components.GroupProposalSection.giangVienSeXepNhomCho")}
    </Alert>
  );
}

function InvitationList({ invitations, busy, inProposal, onAccept, onDecline }: {
  invitations: InvitationView[];
  busy: boolean;
  inProposal: boolean;
  onAccept: (proposalId: string) => void;
  onDecline: (proposalId: string) => void;
}) {
  return (
    <Card as="section" padding="lg" className="flex flex-col gap-3" aria-label={msg("groups.components.GroupProposalSection.loiMoiVaoNhom")}>
      <h2 className="text-[15px] font-bold text-ink">{msg("groups.components.GroupProposalSection.loiMoiVaoNhom2")}{invitations.length})</h2>
      <ul className="flex flex-col gap-2">
        {invitations.map((i) => (
          <li key={i.proposalId} data-testid={`invitation-${i.proposalId}`}
            className="flex flex-wrap items-center gap-3 rounded-lg border border-line px-4 py-3">
            <div className="min-w-0 flex-1">
              <p className="truncate text-[13.5px] font-semibold text-ink">{i.proposalName}</p>
              <p className="text-xs text-muted">
                {msg("groups.components.GroupProposalSection.truongNhom")}{formatStudentLabel({ name: i.leaderName ?? msg("groups.components.GroupProposalSection.chuaRoTen"), studentCode: i.leaderStudentCode })} · {i.memberCount} {msg("groups.components.GroupProposalSection.thanhVien")}{i.expired && msg("groups.components.GroupProposalSection.loiMoiDaHetHan")}
                {!i.expired && inProposal && msg("groups.components.GroupProposalSection.roiDeXuatHienTaiDe")}
              </p>
            </div>
            <Button size="sm" disabled={busy} onClick={() => onDecline(i.proposalId)}>{msg("groups.components.GroupProposalSection.tuChoi")}</Button>
            {i.canAccept && (
              <Button size="sm" variant="primary" disabled={busy} onClick={() => onAccept(i.proposalId)}
                data-testid={`invitation-accept-${i.proposalId}`}>
                {msg("groups.components.GroupProposalSection.chapNhan")}</Button>
            )}
          </li>
        ))}
      </ul>
    </Card>
  );
}

function ProposalCard({ proposal, formation, busy, candidatesCount, onRename, onInvite, onRevoke, onSubmit, onWithdraw, onCancel, onLeave }: {
  proposal: ProposalView;
  formation: FormationSummary;
  busy: boolean;
  candidatesCount: number;
  onRename: () => void;
  onInvite: () => void;
  onRevoke: (accountId: string) => void;
  onSubmit: () => void;
  onWithdraw: () => void;
  onCancel: () => void;
  onLeave: () => void;
}) {
  const draft = proposal.status === "DRAFT";
  const editable = proposal.leader && draft && formation.open;
  const submittable = canSubmit(proposal, formation);
  const missing = Math.max(0, formation.minSize - proposal.members.length);
  return (
    <Card as="section" padding="none" className="flex flex-col gap-4 p-6" aria-label={msg("groups.components.GroupProposalSection.deXuatNhomCuaBan")} data-testid="proposal-card">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="min-w-0 text-xl font-bold text-ink [overflow-wrap:anywhere]">{proposal.name}</h2>
            <Pill tone={draft ? "neutral" : "warning"}>{PROPOSAL_STATUS_LABELS[proposal.status]}</Pill>
          </div>
          <p className="mt-0.5 text-[13px] text-muted">
            {proposal.members.length}/{formation.maxSize} {msg("groups.components.GroupProposalSection.thanhVien")}{draft && missing > 0 && msg("groups.components.GroupProposalSection.canThemNguoiDeGuiDuyet", { missing: String(missing) })}
            {!draft && msg("groups.components.GroupProposalSection.choGiangVienDuyetRutVe")}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {editable && <Button size="sm" icon="edit" disabled={busy} onClick={onRename}>{msg("groups.components.GroupProposalSection.doiTen")}</Button>}
          {editable && (
            <Button size="sm" icon="person_add" disabled={busy || proposal.freeSeats === 0 || candidatesCount === 0} onClick={onInvite}
              data-testid="proposal-invite">
              {msg("groups.components.GroupProposalSection.moiBan")}</Button>
          )}
          {proposal.leader && draft && (
            <Button size="sm" variant="primary" icon="check" disabled={busy || !submittable} onClick={onSubmit}
              data-testid="proposal-submit">
              {msg("groups.components.GroupProposalSection.guiDuyet")}</Button>
          )}
          {proposal.leader && !draft && (
            <Button size="sm" icon="arrow_back" disabled={busy} onClick={onWithdraw} data-testid="proposal-withdraw">{msg("groups.components.GroupProposalSection.rutVeDeSua")}</Button>
          )}
          {proposal.leader && draft && (
            <Button size="sm" variant="danger" disabled={busy} onClick={onCancel} data-testid="proposal-cancel">{msg("groups.components.GroupProposalSection.huyDeXuat2")}</Button>
          )}
          {!proposal.leader && draft && (
            <Button size="sm" disabled={busy} onClick={onLeave} data-testid="proposal-leave">{msg("groups.components.GroupProposalSection.roiDeXuat2")}</Button>
          )}
        </div>
      </div>
      <ul aria-label={msg("groups.components.GroupProposalSection.thanhVienDeXuat")} className="grid gap-2 sm:grid-cols-2">
        {proposal.members.map((m) => <PersonRow key={m.accountId} person={m} note={m.leader ? msg("groups.components.GroupProposalSection.truongNhom2") : undefined} />)}
        {proposal.invitations.map((m) => (
          <PersonRow key={m.accountId} person={m} invited note={m.expired ? msg("groups.components.GroupProposalSection.loiMoiHetHan") : msg("groups.components.GroupProposalSection.dangChoTraLoi")}
            action={editable && !m.expired ? (
              <Button size="sm" disabled={busy} onClick={() => onRevoke(m.accountId)} data-testid={`proposal-revoke-${m.accountId}`}>
                {msg("groups.components.GroupProposalSection.thuHoi")}</Button>
            ) : undefined} />
        ))}
      </ul>
    </Card>
  );
}

function PersonRow({ person, note, invited, action }: { person: ProposalMemberView; note?: string; invited?: boolean; action?: React.ReactNode }) {
  return (
    <li className={cx("flex min-w-0 items-center gap-3 rounded-lg border px-3 py-2.5", invited ? "border-dashed border-line-strong" : "border-line")}>
      <span aria-hidden="true" className={cx("flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold",
        person.leader ? "bg-warning-soft text-warning" : "bg-primary-soft text-primary-ink")}>
        {person.leader ? <Icon name="crown" size={16} filled /> : extractInitials(person.name ?? person.email ?? "?")}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-[13.5px] font-semibold text-ink">{person.name ?? person.email ?? msg("groups.components.GroupProposalSection.chuaRoTen2")}</span>
        {/* Ghi chú (trưởng nhóm, đang chờ trả lời…) thay chỗ email như trước; MSSV vẫn hiện ở đầu (BR-U12-97). */}
        <PersonSubline className="truncate" studentCode={person.studentCode} email={note ?? person.email} />
      </span>
      {action}
    </li>
  );
}
