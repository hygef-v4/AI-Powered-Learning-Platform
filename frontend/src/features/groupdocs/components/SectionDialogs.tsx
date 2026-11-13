"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { SearchPicker } from "@/features/assignments/components/SearchPicker";
import { Alert, Button, Dialog, Field, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { SECTION_STATUS } from "../model/status";
import { assignSection, submitDocument } from "../api";
import type { Receipt, SectionView, WorkspaceView } from "../model/types";

function problemMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.problem.status === 409) return e.problem.detail ?? "Dữ liệu vừa thay đổi, hãy tải lại.";
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
      setError(problemMessage(e, "Không giao được mục."));
      setBusy(false);
    }
  };
  return (
    <Dialog
      open
      title="Giao mục"
      icon="person_add"
      onClose={onClose}
      footer={
        <Button variant="primary" onClick={() => void assign()} disabled={!changed || busy}>
          Giao mục
        </Button>
      }
    >
      <Field label="Thành viên nhận mục" htmlFor="assign-member">
        <SearchPicker id="assign-member" label="Thành viên nhận mục" placeholder="Tìm thành viên" optionIcon="person"
          emptyText="Không có thành viên phù hợp."
          options={view.members.filter((m) => m.accountId !== assignee).map((m) => ({ id: m.accountId, label: m.name ?? m.accountId }))}
          onPick={setAssignee}
          selected={chosen ? { label: chosen.name ?? chosen.accountId, clearLabel: "Bỏ chọn thành viên", onClear: () => setAssignee("") } : undefined} />
      </Field>
      {heldByOther && <Alert tone="warning">Giao cho người khác sẽ nhả mục trước đó.</Alert>}
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
  const name = (id: string | null) => view.members.find((m) => m.accountId === id)?.name ?? "Chưa ai nhận";
  const submit = async () => {
    setBusy(true);
    try {
      onSubmitted(await submitDocument(view.documentId, view.version));
    } catch (e) {
      const problem = e instanceof ApiError ? (e.problem as ApiError["problem"] & { issues?: { blockId?: string }[] }) : undefined;
      const blockIds = (problem?.issues ?? []).map((i) => i.blockId ?? "").filter(Boolean);
      const invalid = problem?.issues !== undefined;
      onFailed(invalid ? "Chưa nộp được vì còn mục chưa xong." : problemMessage(e, "Không nộp được, thử lại sau."), blockIds);
    }
  };
  return (
    <Dialog
      open
      title="Nộp tài liệu nhóm?"
      icon="upload_file"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            Quay lại
          </Button>
          <Button variant="primary" onClick={() => void submit()} disabled={busy}>
            {undone.length ? "Vẫn nộp" : "Nộp"}
          </Button>
        </>
      }
    >
      {undone.length > 0 && (
        <TableFrame filledHead className="[&_table]:table-fixed">
          <colgroup><col /><col className="w-[32%]" /><col className="w-[140px]" /></colgroup><thead>
            <tr>
              <Th>Mục</Th>
              <Th>Người giữ</Th>
              <Th>Trạng thái</Th>
            </tr>
          </thead>
          <tbody>
            {undone.map((s) => (
              <tr key={s.id}>
                <Td>{s.title}</Td>
                <Td>{name(s.holderId)}</Td>
                <Td><Pill tone={SECTION_STATUS[s.status].tone}>{SECTION_STATUS[s.status].text}</Pill></Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
      )}
    </Dialog>
  );
}
