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
    if (e.problem.status === 409) return e.problem.detail ?? msg("groupdocs.components.SectionDialogs.duLieuVuaThayDoiHay");
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
      setError(describeProblem(e, msg("groupdocs.components.SectionDialogs.khongGiaoDuocMuc")));
      setBusy(false);
    }
  };
  return (
    <Dialog
      open
      title={msg("groupdocs.components.SectionDialogs.giaoMuc")}
      icon="person_add"
      onClose={onClose}
      footer={
        <Button variant="primary" onClick={() => void assign()} disabled={!changed || busy}>
          {msg("groupdocs.components.SectionDialogs.giaoMuc")}</Button>
      }
    >
      <Field label={msg("groupdocs.components.SectionDialogs.thanhVienNhanMuc")} htmlFor="assign-member">
        <SearchPicker id="assign-member" label={msg("groupdocs.components.SectionDialogs.thanhVienNhanMuc")} placeholder={msg("groupdocs.components.SectionDialogs.timThanhVien")} optionIcon="person"
          emptyText={msg("groupdocs.components.SectionDialogs.khongCoThanhVienPhuHop")}
          options={view.members.filter((m) => m.accountId !== assignee).map((m) => ({ id: m.accountId, label: formatStudentLabel({ ...m, name: m.name ?? m.accountId }) }))}
          onPick={setAssignee}
          selected={chosen ? { label: formatStudentLabel({ ...chosen, name: chosen.name ?? chosen.accountId }), clearLabel: msg("groupdocs.components.SectionDialogs.boChonThanhVien"), onClear: () => setAssignee("") } : undefined} />
      </Field>
      {heldByOther && <Alert tone="warning">{msg("groupdocs.components.SectionDialogs.giaoChoNguoiKhacSeNha")}</Alert>}
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
    return formatStudentLabel({ name: member?.name ?? msg("groupdocs.components.SectionDialogs.chuaAiNhan"), studentCode: member?.studentCode });
  };
  const submit = async () => {
    setBusy(true);
    try {
      onSubmitted(await submitDocument(view.documentId, view.version));
    } catch (e) {
      const problem = e instanceof ApiError ? (e.problem as ApiError["problem"] & { issues?: { blockId?: string }[] }) : undefined;
      const blockIds = (problem?.issues ?? []).map((i) => i.blockId ?? "").filter(Boolean);
      const invalid = problem?.issues !== undefined;
      onFailed(invalid ? msg("groupdocs.components.SectionDialogs.chuaNopDuocViConMuc") : describeProblem(e, msg("groupdocs.components.SectionDialogs.khongNopDuocThuLaiSau")), blockIds);
    }
  };
  return (
    <Dialog
      open
      title={msg("groupdocs.components.SectionDialogs.nopTaiLieuNhom")}
      icon="upload_file"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            {msg("groupdocs.components.SectionDialogs.quayLai")}</Button>
          <Button variant="primary" onClick={() => void submit()} disabled={busy}>
            {undone.length ? msg("groupdocs.components.SectionDialogs.vanNop") : msg("groupdocs.components.SectionDialogs.nop")}
          </Button>
        </>
      }
    >
      {undone.length > 0 && (
        <TableFrame filledHead className="[&_table]:table-fixed">
          <colgroup><col /><col className="w-[32%]" /><col className="w-[140px]" /></colgroup><thead>
            <tr>
              <Th>{msg("groupdocs.components.SectionDialogs.muc")}</Th>
              <Th>{msg("groupdocs.components.SectionDialogs.nguoiGiu")}</Th>
              <Th>{msg("groupdocs.components.SectionDialogs.trangThai")}</Th>
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
