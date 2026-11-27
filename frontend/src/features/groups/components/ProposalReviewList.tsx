"use client";

import { formatStudentLabel } from "@/shared/studentCode";
import { useCallback, useEffect, useId, useState } from "react";
import { Alert, Button, cx, Dialog, Field, Pill, Section, showToast, Textarea } from "@/shared/ui";
import { approveProposals, getClassProposals, rejectProposal } from "../api";
import {
  describeFormation,
  MAX_REJECT_REASON,
  PROPOSAL_STATUS_LABELS,
  describeProposalError,
  WARNING_LABELS,
} from "../model/proposals";
import type { StudentRow } from "../model/groupDrafts";
import type { ClassProposalsView, GroupSetView, ReviewProposalView } from "../model/types";
import { GroupImportDialog } from "./GroupImportDialog";
import { SelfGroupingSettingsDialog } from "./SelfGroupingSettingsDialog";
import { msg } from "@/i18n/msg";

/**
 * Sinh viên tự chia nhóm ở tab Sinh viên của giảng viên (US-GRP-007, 010): trạng thái đợt và nút cài đặt; đề xuất đang hoạt
 * động, chọn nhiều đề xuất chờ duyệt để duyệt một lần, từ chối có lý do tùy chọn; sinh viên chưa có nhóm kèm đề xuất họ đang ở.
 * Tải lại khi `setVersion` của bộ nhóm đổi (giảng viên vừa lưu nhóm). Duyệt xong gọi `onGroupsChanged` để tải lại bộ nhóm.
 * Có `groupSet` và `people` thì hiện nút Nhập từ Excel cạnh Cài đặt (BR-U12-99); lưu xong gọi `onGroupsSaved`.
 */
export function ProposalReviewList({ classId, setVersion, readOnly, onGroupsChanged, groupSet, people, onGroupsSaved }: {
  classId: string;
  setVersion: number;
  readOnly: boolean;
  onGroupsChanged: () => void;
  groupSet?: GroupSetView;
  people?: StudentRow[];
  onGroupsSaved?: (next: GroupSetView) => void;
}) {
  const [view, setView] = useState<ClassProposalsView | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [picked, setPicked] = useState<string[]>([]);
  const [failures, setFailures] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [rejecting, setRejecting] = useState<ReviewProposalView | null>(null);

  const load = useCallback(() => getClassProposals(classId).then((loaded) => {
    setView(loaded);
    setLoadError(null);
    setPicked((current) => current.filter((id) => loaded.proposals.some((p) => p.id === id && p.status === "SUBMITTED")));
  }, () => setLoadError(msg("groups.components.ProposalReviewList.couldNotLoadGroupProposals"))), [classId]);

  useEffect(() => {
    void load();
  }, [load, setVersion]);

  if (loadError && !view) {
    return <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void load()}>{msg("groups.components.ProposalReviewList.reload")}</Button>}>{loadError}</Alert>;
  }
  if (!view) return null;

  const { formation } = view;
  const submitted = view.proposals.filter((p) => p.status === "SUBMITTED");
  const drafts = view.proposals.filter((p) => p.status === "DRAFT");
  // Lớp chưa từng bật và không có đề xuất: chỉ hiện một dòng mời bật.
  const idle = !formation.enabled && view.proposals.length === 0;

  const approve = async (ids: string[]) => {
    setBusy(true);
    setError(null);
    try {
      const result = await approveProposals(classId, ids);
      setFailures(Object.fromEntries(result.failed.map((f) => [f.proposalId, f.message])));
      if (result.approved.length > 0) {
        showToast(msg("groups.components.ProposalReviewList.approvedProposalsAsGroups", { length: String(result.approved.length) }));
        onGroupsChanged();
      }
      if (result.failed.length > 0) setError(msg("groups.components.ProposalReviewList.proposalsCouldNotBeApproved", { length: String(result.failed.length) }));
      await load();
    } catch (e) {
      setError(describeProposalError(e, msg("groups.components.ProposalReviewList.couldNotApproveTheProposal")));
      await load();
    } finally {
      setBusy(false);
    }
  };

  const toggle = (id: string) => setPicked((current) => (current.includes(id) ? current.filter((x) => x !== id) : [...current, id]));

  return (
    <Section title={msg("groups.components.ProposalReviewList.studentSelfGrouping")} icon="groups" description={msg("groups.components.ProposalReviewList.selfGrouping", { value: String(describeFormation(formation)) })}
      actions={!readOnly && <div className="flex flex-wrap gap-2">
        {groupSet && people && (
          <Button size="sm" icon="upload_file" onClick={() => setImportOpen(true)} data-testid="group-import-open">{msg("groups.components.ProposalReviewList.importFromExcel")}</Button>
        )}
        <Button size="sm" icon="settings" onClick={() => setSettingsOpen(true)} data-testid="formation-open">{msg("groups.components.ProposalReviewList.settings")}</Button>
      </div>}>
      {idle ? (
        <p className="text-[13px] text-muted">{msg("groups.components.ProposalReviewList.turnOnToLetStudents")}</p>
      ) : (
        <div className="flex flex-col gap-4">
          {error && <Alert tone="danger" role="alert">{error}</Alert>}
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-[13.5px] font-semibold text-ink">{msg("groups.components.ProposalReviewList.awaitingApproval")}{submitted.length})</h3>
            {!readOnly && submitted.length > 0 && (
              <Button size="sm" variant="primary" icon="check" disabled={busy || picked.length === 0}
                onClick={() => void approve(picked)} data-testid="proposals-approve-selected">
                {msg("groups.components.ProposalReviewList.approveSelected")}{picked.length})
              </Button>
            )}
          </div>
          {submitted.length === 0 && <p className="text-[13px] text-muted">{msg("groups.components.ProposalReviewList.noProposalsAwaitingApproval")}</p>}
          <ul className="flex flex-col gap-2">
            {submitted.map((p) => (
              <ProposalRow key={p.id} proposal={p} failure={failures[p.id]} busy={busy} readOnly={readOnly}
                checked={picked.includes(p.id)} onToggle={() => toggle(p.id)}
                onApprove={() => void approve([p.id])} onReject={() => setRejecting(p)} />
            ))}
          </ul>
          {drafts.length > 0 && (
            <details className="rounded-lg border border-line px-4 py-2">
              <summary className="cursor-pointer py-1 text-[13.5px] font-semibold text-ink">{msg("groups.components.ProposalReviewList.forming")}{drafts.length})</summary>
              <ul className="mt-2 flex flex-col gap-2">
                {drafts.map((p) => <ProposalRow key={p.id} proposal={p} busy={busy} readOnly />)}
              </ul>
            </details>
          )}
          <UngroupedStudentsList view={view} />
        </div>
      )}

      {settingsOpen && (
        <SelfGroupingSettingsDialog classId={classId} onClose={() => setSettingsOpen(false)}
          onSaved={() => { setSettingsOpen(false); showToast(msg("groups.components.ProposalReviewList.selfGroupingSettingsSaved")); void load(); }} />
      )}
      {importOpen && groupSet && people && (
        <GroupImportDialog classId={classId} set={groupSet} people={people}
          sizes={{ minSize: formation.minSize, maxSize: formation.maxSize }}
          proposalOf={new Map(view.ungrouped.flatMap((u) => (u.proposalName ? [[u.person.accountId, u.proposalName] as const] : [])))}
          onClose={() => setImportOpen(false)}
          onSaved={(next) => { setImportOpen(false); onGroupsSaved?.(next); }} />
      )}
      {rejecting && (
        <RejectProposalDialog proposal={rejecting} onClose={() => setRejecting(null)}
          onRejected={(next) => { setRejecting(null); setView(next); showToast(msg("groups.components.ProposalReviewList.rejected", { name: String(rejecting.name) }), "danger"); }} />
      )}
    </Section>
  );
}

function ProposalRow({ proposal, failure, busy, readOnly, checked, onToggle, onApprove, onReject }: {
  proposal: ReviewProposalView;
  failure?: string;
  busy: boolean;
  readOnly: boolean;
  checked?: boolean;
  onToggle?: () => void;
  onApprove?: () => void;
  onReject?: () => void;
}) {
  const reviewable = !readOnly && proposal.status === "SUBMITTED";
  return (
    <li className={cx("flex flex-col gap-2 rounded-xl border bg-surface px-4 py-3", failure ? "border-danger/50" : "border-line")}
      data-testid={`proposal-review-${proposal.id}`}>
      <div className="flex flex-wrap items-start gap-3">
        {reviewable && (
          <input type="checkbox" className="mt-1 size-4 accent-[var(--c-primary)]" checked={!!checked} disabled={busy}
            onChange={onToggle} aria-label={msg("groups.components.ProposalReviewList.select", { name: String(proposal.name) })} />
        )}
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="truncate text-[13.5px] font-semibold text-ink">{proposal.name}</span>
            <Pill tone={proposal.status === "SUBMITTED" ? "warning" : "neutral"}>{PROPOSAL_STATUS_LABELS[proposal.status]}</Pill>
          </div>
          <p className="text-xs text-muted">
            {msg("groups.components.ProposalReviewList.leader")}{formatStudentLabel({ ...proposal.leader, name: proposal.leader.name ?? proposal.leader.email ?? msg("groups.components.ProposalReviewList.unknownName") })} · {proposal.members.length} {msg("groups.components.ProposalReviewList.members")}{proposal.pendingInvitations > 0 && msg("groups.components.ProposalReviewList.pendingInvitations", { pendingInvitations: String(proposal.pendingInvitations) })}
          </p>
          <p className="mt-1 text-[13px] text-ink">{proposal.members.map(formatStudentLabel).join(", ")}</p>
          {proposal.warnings.map((w) => <p key={w} className="mt-1 text-xs text-warning">{WARNING_LABELS[w]}</p>)}
        </div>
        {reviewable && (
          <div className="flex gap-2">
            <Button size="sm" variant="danger" disabled={busy} onClick={onReject} data-testid={`proposal-reject-${proposal.id}`}>{msg("groups.components.ProposalReviewList.reject")}</Button>
            <Button size="sm" variant="primary" disabled={busy} onClick={onApprove} data-testid={`proposal-approve-${proposal.id}`}>{msg("groups.components.ProposalReviewList.approve")}</Button>
          </div>
        )}
      </div>
      {failure && <p role="alert" className="text-xs text-danger">{failure}</p>}
    </li>
  );
}

/** Sinh viên đang học chưa có nhóm, kèm đề xuất họ đang ở; dùng cùng dữ liệu với Chia ngẫu nhiên. */
function UngroupedStudentsList({ view }: { view: ClassProposalsView }) {
  if (view.ungrouped.length === 0) return <p className="text-[13px] text-muted">{msg("groups.components.ProposalReviewList.allStudentsAlreadyHaveA")}</p>;
  return (
    <details className="rounded-lg border border-line px-4 py-2" data-testid="ungrouped-students">
      <summary className="cursor-pointer py-1 text-[13.5px] font-semibold text-ink">{msg("groups.components.ProposalReviewList.noGroup")}{view.ungrouped.length})</summary>
      <ul className="mt-2 flex flex-col divide-y divide-line">
        {view.ungrouped.map((u) => (
          <li key={u.person.accountId} className="flex flex-wrap items-center justify-between gap-2 py-2 text-[13px]">
            <span className="min-w-0">
              <span className="font-semibold text-ink">{u.person.name ?? u.person.email ?? msg("groups.components.ProposalReviewList.unknownName2")}</span>
              {(u.person.studentCode || u.person.email) && <span className="ml-2 text-muted">
                {[u.person.studentCode, u.person.email].filter(Boolean).join(" · ")}</span>}
            </span>
            <span className="text-muted">
              {u.proposalName ? `${u.proposalName} · ${PROPOSAL_STATUS_LABELS[u.proposalStatus ?? "DRAFT"]}` : msg("groups.components.ProposalReviewList.noProposalYet")}
            </span>
          </li>
        ))}
      </ul>
    </details>
  );
}

function RejectProposalDialog({ proposal, onClose, onRejected }: {
  proposal: ReviewProposalView;
  onClose: () => void;
  onRejected: (next: ClassProposalsView) => void;
}) {
  const fieldId = useId();
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const tooLong = reason.trim().length > MAX_REJECT_REASON;
  const reject = async () => {
    if (tooLong) return;
    setBusy(true);
    setError(null);
    try {
      onRejected(await rejectProposal(proposal.id, reason.trim() || null));
    } catch (e) {
      setError(describeProposalError(e, msg("groups.components.ProposalReviewList.couldNotRejectTheProposal")));
      setBusy(false);
    }
  };
  return (
    <Dialog open title={msg("groups.components.ProposalReviewList.rejectProposal", { name: String(proposal.name) })} tone="danger" onClose={onClose} dismissible={!busy}
      footer={<Button variant="danger" disabled={busy || tooLong} onClick={() => void reject()} data-testid="proposal-reject-confirm">{msg("groups.components.ProposalReviewList.reject")}</Button>}>
      <Field label={msg("groups.components.ProposalReviewList.reasonOptional")} htmlFor={fieldId} error={tooLong ? msg("groups.components.ProposalReviewList.theReasonCanBeAt") : error}>
        <Textarea id={fieldId} rows={3} value={reason} disabled={busy} onChange={(event) => setReason(event.target.value)}
          data-testid="proposal-reject-reason" />
      </Field>
      <p className="text-right text-xs text-muted">{reason.trim().length}/{MAX_REJECT_REASON}</p>
      <p className="text-[13px] text-muted">{msg("groups.components.ProposalReviewList.membersAreNotifiedAndCan")}</p>
    </Dialog>
  );
}
