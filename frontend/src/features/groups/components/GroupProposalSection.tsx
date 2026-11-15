"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Icon, Pill, cx, showToast, useConfirmDialog } from "@/shared/ui";
import { initials } from "@/shared/layout/navigation";
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
  proposalErrorText,
} from "../model/proposals";
import type { FormationSummary, InvitationView, MyProposalView, ProposalMemberView, ProposalView } from "../model/types";
import { InviteMembersDialog, ProposalNameDialog } from "./ProposalDialogs";

type Open = { kind: "create" } | { kind: "rename" } | { kind: "invite" } | null;

/** Màn đề xuất của sinh viên chưa có nhóm; `null` khi đang tải hoặc không tải được. */
export function useMyProposal(classId: string) {
  const [view, setView] = useState<MyProposalView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const reload = useCallback(
    () => getMyProposal(classId).then((loaded) => { setView(loaded); setError(null); },
      () => setError("Không tải được đề xuất nhóm, thử lại sau.")),
    [classId],
  );
  useEffect(() => {
    let active = true;
    getMyProposal(classId).then(
      (loaded) => { if (active) setView(loaded); },
      () => { if (active) setError("Không tải được đề xuất nhóm, thử lại sau."); },
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
    return <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>{error}</Alert>;
  }
  if (!view) return <p role="status" className="text-[13px] text-muted">Đang tải đề xuất nhóm…</p>;
  if (view.grouped) return null;

  /** Chạy một thao tác; trả lời lỗi để popup hiện tại chỗ, hoặc null khi xong. */
  const run = async (action: () => Promise<MyProposalView>, done?: string): Promise<string | null> => {
    setBusy(true);
    setActionError(null);
    try {
      setView(await action());
      setOpen(null);
      if (done) showToast(done);
      return null;
    } catch (e) {
      if (isVersionConflict(e)) void reload();
      return proposalErrorText(e);
    } finally {
      setBusy(false);
    }
  };
  const runInline = async (action: () => Promise<MyProposalView>, done?: string) => {
    const failed = await run(action, done);
    if (failed) setActionError(failed);
  };

  const { formation, proposal } = view;
  return (
    <div className="flex flex-col gap-4" data-testid="group-proposal-section">
      <FormationBanner formation={formation} />
      {view.lastRejected && !proposal && (
        <Alert tone="warning" title={`Đề xuất "${view.lastRejected.name}" bị từ chối`}>
          {view.lastRejected.reason ? `Lý do: ${view.lastRejected.reason}` : "Giảng viên không ghi lý do."} Bạn có thể lập hoặc vào đề xuất khác.
        </Alert>
      )}
      {actionError && <Alert tone="danger" role="alert">{actionError}</Alert>}
      {proposal ? (
        <ProposalCard proposal={proposal} formation={formation} busy={busy} candidatesCount={view.candidates.length}
          onRename={() => setOpen({ kind: "rename" })}
          onInvite={() => setOpen({ kind: "invite" })}
          onRevoke={(accountId) => void runInline(() => revokeInvitation(proposal.id, accountId, proposal.version))}
          onSubmit={() => void runInline(() => changeProposal(proposal.id, "submit", proposal.version), "Đã gửi đề xuất cho giảng viên")}
          onWithdraw={() => void runInline(() => changeProposal(proposal.id, "withdraw", proposal.version), "Đã rút đề xuất về nháp")}
          onCancel={() => confirm(`Hủy đề xuất ${proposal.name}?`,
            () => void runInline(() => changeProposal(proposal.id, "cancel", proposal.version), "Đã hủy đề xuất"), "Hủy đề xuất")}
          onLeave={() => confirm(`Rời đề xuất ${proposal.name}?`,
            () => void runInline(() => answerProposal(proposal.id, "leave"), "Đã rời đề xuất"), "Rời đề xuất")} />
      ) : formation.open && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-dashed border-line-strong bg-surface p-5">
          <p className="text-[13.5px] text-ink">Lập nhóm rồi mời bạn cùng lớp. Đủ người thì gửi giảng viên duyệt.</p>
          <Button variant="primary" icon="add" disabled={busy} onClick={() => setOpen({ kind: "create" })} data-testid="proposal-create">
            Tạo đề xuất nhóm
          </Button>
        </div>
      )}
      {view.invitations.length > 0 && (
        <InvitationList invitations={view.invitations} busy={busy} inProposal={!!proposal}
          onAccept={(id) => void runInline(() => answerProposal(id, "accept"), "Đã vào nhóm")}
          onDecline={(id) => void runInline(() => answerProposal(id, "decline"), "Đã từ chối lời mời")} />
      )}
      {!proposal && !formation.enabled && view.invitations.length === 0 && (
        <Alert tone="info">Bạn chưa có nhóm trong lớp này. Giảng viên sẽ xếp nhóm.</Alert>
      )}

      {open?.kind === "create" && (
        <ProposalNameDialog title="Tạo đề xuất nhóm" confirmLabel="Tạo" onClose={() => setOpen(null)}
          onSave={(name) => run(() => createProposal(classId, name), "Đã tạo đề xuất nhóm")} />
      )}
      {open?.kind === "rename" && proposal && (
        <ProposalNameDialog title="Đổi tên nhóm" initial={proposal.name} confirmLabel="Lưu" onClose={() => setOpen(null)}
          onSave={(name) => run(() => renameProposal(proposal.id, name, proposal.version), "Đã đổi tên nhóm")} />
      )}
      {open?.kind === "invite" && proposal && (
        <InviteMembersDialog candidates={view.candidates} freeSeats={proposal.freeSeats} onClose={() => setOpen(null)}
          onInvite={(ids) => run(() => inviteToProposal(proposal.id, ids, proposal.version), "Đã gửi lời mời")} />
      )}
      {confirmDialog}
    </div>
  );
}

/** Trạng thái đợt tự chia nhóm: sĩ số, hạn chót hoặc lý do đóng. */
export function FormationBanner({ formation }: { formation: FormationSummary }) {
  if (!formation.enabled) return null;
  return (
    <Alert tone={formation.open ? "info" : "warning"} title="Tự chia nhóm" role="status">
      {describeFormation(formation)}
      {!formation.open && " Giảng viên sẽ xếp nhóm cho bạn."}
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
    <section aria-label="Lời mời vào nhóm" className="flex flex-col gap-3 rounded-xl border border-line bg-surface p-5">
      <h2 className="text-[15px] font-bold text-ink">Lời mời vào nhóm ({invitations.length})</h2>
      <ul className="flex flex-col gap-2">
        {invitations.map((i) => (
          <li key={i.proposalId} data-testid={`invitation-${i.proposalId}`}
            className="flex flex-wrap items-center gap-3 rounded-lg border border-line px-4 py-3">
            <div className="min-w-0 flex-1">
              <p className="truncate text-[13.5px] font-semibold text-ink">{i.proposalName}</p>
              <p className="text-xs text-muted">
                Trưởng nhóm {i.leaderName ?? "chưa rõ tên"} · {i.memberCount} thành viên
                {i.expired && " · Lời mời đã hết hạn"}
                {!i.expired && inProposal && " · Rời đề xuất hiện tại để chấp nhận"}
              </p>
            </div>
            <Button size="sm" disabled={busy} onClick={() => onDecline(i.proposalId)}>Từ chối</Button>
            {i.canAccept && (
              <Button size="sm" variant="primary" disabled={busy} onClick={() => onAccept(i.proposalId)}
                data-testid={`invitation-accept-${i.proposalId}`}>
                Chấp nhận
              </Button>
            )}
          </li>
        ))}
      </ul>
    </section>
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
    <section aria-label="Đề xuất nhóm của bạn" className="flex flex-col gap-4 rounded-xl border border-line bg-surface p-6"
      data-testid="proposal-card">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="min-w-0 text-xl font-bold text-ink [overflow-wrap:anywhere]">{proposal.name}</h2>
            <Pill tone={draft ? "neutral" : "warning"}>{PROPOSAL_STATUS_LABELS[proposal.status]}</Pill>
          </div>
          <p className="mt-0.5 text-[13px] text-muted">
            {proposal.members.length}/{formation.maxSize} thành viên
            {draft && missing > 0 && ` · cần thêm ${missing} người để gửi duyệt`}
            {!draft && " · chờ giảng viên duyệt, rút về nếu cần sửa"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {editable && <Button size="sm" icon="edit" disabled={busy} onClick={onRename}>Đổi tên</Button>}
          {editable && (
            <Button size="sm" icon="person_add" disabled={busy || proposal.freeSeats === 0 || candidatesCount === 0} onClick={onInvite}
              data-testid="proposal-invite">
              Mời bạn
            </Button>
          )}
          {proposal.leader && draft && (
            <Button size="sm" variant="primary" icon="check" disabled={busy || !submittable} onClick={onSubmit}
              data-testid="proposal-submit">
              Gửi duyệt
            </Button>
          )}
          {proposal.leader && !draft && (
            <Button size="sm" icon="arrow_back" disabled={busy} onClick={onWithdraw} data-testid="proposal-withdraw">Rút về để sửa</Button>
          )}
          {proposal.leader && draft && (
            <Button size="sm" variant="danger" disabled={busy} onClick={onCancel} data-testid="proposal-cancel">Hủy đề xuất</Button>
          )}
          {!proposal.leader && draft && (
            <Button size="sm" disabled={busy} onClick={onLeave} data-testid="proposal-leave">Rời đề xuất</Button>
          )}
        </div>
      </div>
      <ul aria-label="Thành viên đề xuất" className="grid gap-2 sm:grid-cols-2">
        {proposal.members.map((m) => <PersonRow key={m.accountId} person={m} note={m.leader ? "Trưởng nhóm" : undefined} />)}
        {proposal.invitations.map((m) => (
          <PersonRow key={m.accountId} person={m} invited note={m.expired ? "Lời mời hết hạn" : "Đang chờ trả lời"}
            action={editable && !m.expired ? (
              <Button size="sm" disabled={busy} onClick={() => onRevoke(m.accountId)} data-testid={`proposal-revoke-${m.accountId}`}>
                Thu hồi
              </Button>
            ) : undefined} />
        ))}
      </ul>
    </section>
  );
}

function PersonRow({ person, note, invited, action }: { person: ProposalMemberView; note?: string; invited?: boolean; action?: React.ReactNode }) {
  return (
    <li className={cx("flex min-w-0 items-center gap-3 rounded-lg border px-3 py-2.5", invited ? "border-dashed border-line-strong" : "border-line")}>
      <span aria-hidden="true" className={cx("flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold",
        person.leader ? "bg-warning-soft text-warning" : "bg-primary-soft text-primary-ink")}>
        {person.leader ? <Icon name="crown" size={16} filled /> : initials(person.name ?? person.email ?? "?")}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-[13.5px] font-semibold text-ink">{person.name ?? person.email ?? "Chưa rõ tên"}</span>
        {/* Ghi chú (trưởng nhóm, đang chờ trả lời…) thay chỗ email như trước; MSSV vẫn hiện ở đầu (BR-U12-97). */}
        <PersonSubline className="truncate" studentCode={person.studentCode} email={note ?? person.email} />
      </span>
      {action}
    </li>
  );
}
