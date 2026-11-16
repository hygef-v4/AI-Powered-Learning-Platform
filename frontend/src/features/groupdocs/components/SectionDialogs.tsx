"use client";

import { formatStudentLabel } from "@/shared/studentCode";
import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { SearchPicker } from "@/features/assignments/components/SearchPicker";
import { Alert, Button, Dialog, Field, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { SECTION_STATUS } from "../model/status";
import { assignSection, submitDocument } from "../api";
import type { Receipt, SectionView, WorkspaceView } from "../model/types";
import { msg } from "@/i18n/msg";

function describeProblem(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.problem.status === 409) return e.problem.detail ?? msg("groupdocs.components.SectionDialogs.theDataJustChangedPlease");
    return e.problem.detail ?? fallback;
  }
  return fallback;
}

/**
 * Trưởng nhóm giao mục (s18f, BR-U12-33): ô tìm chọn thành viên như các ô chọn khác; mục đang có người giữ thì ô ghi sẵn
 * người đó và nhắc giao cho người khác sẽ nhả mục trước đó.
 */
export function AssignSectionDialog({
  view,
  section,
  onClose,
  onChanged,
}: {
  view: WorkspaceView;
  section: SectionView;
  onClose: () => void;
  onChanged: () => void;
}) {
  const heldByOther = section.status === "CLAIMED";
  const [assignee, setAssignee] = useState(heldByOther ? (section.holderId ?? "") : "");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const chosen = view.members.find((m) => m.accountId === assignee);
  const changed = !!assignee && assignee !== section.holderId;
  const assign = async () => {
    setBusy(true);
    try {
      await assignSection(view.documentId, section.id, assignee, heldByOther);
      onChanged();
      onClose();
    } catch (e) {
      setError(describeProblem(e, msg("groupdocs.components.SectionDialogs.couldNotAssignTheSection")));
      setBusy(false);
    }
  };
  return (
    <Dialog
      open
      title={msg("groupdocs.components.SectionDialogs.assignSection")}
      icon="person_add"
      onClose={onClose}
      footer={
        <Button variant="primary" onClick={() => void assign()} disabled={!changed || busy}>
          {msg("groupdocs.components.SectionDialogs.assignSection")}</Button>
      }
    >
      <Field label={msg("groupdocs.components.SectionDialogs.memberTakingTheSection")} htmlFor="assign-member">
        <SearchPicker id="assign-member" label={msg("groupdocs.components.SectionDialogs.memberTakingTheSection")} placeholder={msg("groupdocs.components.SectionDialogs.findMembers")} optionIcon="person"
          emptyText={msg("groupdocs.components.SectionDialogs.noMatchingMembers")}
          options={view.members.filter((m) => m.accountId !== assignee).map((m) => ({ id: m.accountId, label: formatStudentLabel({ ...m, name: m.name ?? m.accountId }) }))}
          onPick={setAssignee}
          selected={chosen ? { label: formatStudentLabel({ ...chosen, name: chosen.name ?? chosen.accountId }), clearLabel: msg("groupdocs.components.SectionDialogs.deselectMember"), onClear: () => setAssignee("") } : undefined} />
      </Field>
      {heldByOther && <Alert tone="warning">{msg("groupdocs.components.SectionDialogs.assigningItToSomeoneElse")}</Alert>}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
    </Dialog>
  );
}

/**
 * Nộp tài liệu nhóm (s18c, BR-U12-40): chỉ trưởng nhóm; mục chưa Xong không vào bản nộp. Nộp lỗi thì đóng popup và báo
 * lên trang ({@code onFailed}: một câu báo lỗi và các block bị báo lỗi) để trang tô đỏ các mục chưa xong.
 */
export function SubmitGroupDialog({
  view,
  onClose,
  onSubmitted,
  onFailed,
}: {
  view: WorkspaceView;
  onClose: () => void;
  onSubmitted: (receipt: Receipt) => void;
  onFailed: (message: string, blockIds: string[]) => void;
}) {
  const [busy, setBusy] = useState(false);
  const undone = view.sections.filter((s) => s.status !== "DONE");
  const resolveName = (id: string | null) => {
    const member = view.members.find((m) => m.accountId === id);
    return formatStudentLabel({ name: member?.name ?? msg("groupdocs.components.SectionDialogs.unclaimed"), studentCode: member?.studentCode });
  };
  const submit = async () => {
    setBusy(true);
    try {
      onSubmitted(await submitDocument(view.documentId, view.version));
    } catch (e) {
      const problem = e instanceof ApiError ? (e.problem as ApiError["problem"] & { issues?: { blockId?: string }[] }) : undefined;
      const blockIds = (problem?.issues ?? []).map((i) => i.blockId ?? "").filter(Boolean);
      const invalid = problem?.issues !== undefined;
      onFailed(invalid ? msg("groupdocs.components.SectionDialogs.cannotSubmitYetBecauseSome") : describeProblem(e, msg("groupdocs.components.SectionDialogs.couldNotSubmitPleaseTry")), blockIds);
    }
  };
  return (
    <Dialog
      open
      title={msg("groupdocs.components.SectionDialogs.submitGroupDocument")}
      icon="upload_file"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            {msg("groupdocs.components.SectionDialogs.back")}</Button>
          <Button variant="primary" onClick={() => void submit()} disabled={busy}>
            {undone.length ? msg("groupdocs.components.SectionDialogs.submitAnyway") : msg("groupdocs.components.SectionDialogs.submit")}
          </Button>
        </>
      }
    >
      {undone.length > 0 && (
        <TableFrame filledHead className="[&_table]:table-fixed">
          <colgroup><col /><col className="w-[32%]" /><col className="w-[140px]" /></colgroup><thead>
            <tr>
              <Th>{msg("groupdocs.components.SectionDialogs.section")}</Th>
              <Th>{msg("groupdocs.components.SectionDialogs.holder")}</Th>
              <Th>{msg("groupdocs.components.SectionDialogs.status")}</Th>
            </tr>
          </thead>
          <tbody>
            {undone.map((s) => (
              <tr key={s.id}>
                <Td>{s.title}</Td>
                <Td>{resolveName(s.holderId)}</Td>
                <Td><Pill tone={SECTION_STATUS[s.status].tone}>{SECTION_STATUS[s.status].text}</Pill></Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
      )}
    </Dialog>
  );
}
