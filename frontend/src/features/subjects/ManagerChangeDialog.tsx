"use client";

import { useCallback, useId, useState } from "react";
import { Button, Dialog } from "@/shared/ui";
import { changeSubjectManager } from "./api";
import { ManagerPicker } from "./ManagerPicker";
import type { ManagerCandidate, SearchSubjectManagers } from "./managerSearch";
import { needsSubjectReload, SubjectMutationFeedback, useSubjectMutation, type SubjectMutationProps } from "./subjectMutation";

export function ManagerChangeDialog({ subject, onClose, onSaved, onReload, onAccessError, searchManagers }: SubjectMutationProps & { onClose: () => void; searchManagers?: SearchSubjectManagers }) {
  const id = useId();
  const [mode, setMode] = useState<"assign" | "unassign">("assign");
  const [manager, setManager] = useState<ManagerCandidate | null>(null);
  const mutation = useSubjectMutation(onAccessError);
  const close = useCallback(() => { if (mutation.canClose()) onClose(); }, [mutation, onClose]);
  const conflict = needsSubjectReload(mutation.error);
  const valid = mode === "unassign" || manager !== null;
  function submit() {
    if (!valid || conflict) return;
    void mutation.run(() => changeSubjectManager(subject.id, { managerId: mode === "unassign" ? null : manager!.accountId, version: subject.version }), onSaved);
  }
  return <Dialog open title="Đổi / bỏ chủ nhiệm" size="lg" onClose={close} dismissible={!mutation.busy}
    footer={<><Button onClick={close} disabled={mutation.busy} data-testid="subject-manager-cancel">Hủy</Button>
      <Button variant="primary" onClick={submit} disabled={mutation.busy || conflict || !valid} data-testid="subject-manager-save">{mutation.busy ? "Đang lưu…" : "Xác nhận thay đổi"}</Button></>}>
    <p className="text-sm">Chủ nhiệm hiện tại: {subject.managerId === null ? "Chưa phân công" : subject.manager ? `${subject.manager.displayName} (${subject.manager.email})` : "Không tra được thông tin chủ nhiệm"}</p>
    <fieldset disabled={mutation.busy} className="flex flex-col gap-3">
      <legend className="mb-2 text-xs font-semibold">Thao tác phân công</legend>
      <label className="flex items-center gap-2"><input type="radio" name={id} checked={mode === "assign"} onChange={() => { setMode("assign"); setManager(null); }} data-testid="subject-manager-assign" />Phân công chủ nhiệm</label>
      <label className="flex items-center gap-2"><input type="radio" name={id} checked={mode === "unassign"} onChange={() => { setMode("unassign"); setManager(null); }} data-testid="subject-manager-unassign" />Bỏ phân công chủ nhiệm</label>
      {mode === "assign" ? <ManagerPicker value={manager} onChange={setManager} disabled={mutation.busy} searchManagers={searchManagers} /> : <p className="text-sm text-muted">Môn sẽ không có chủ nhiệm. Người cũ mất quyền chủ nhiệm từ request tiếp theo sau khi lưu thành công.</p>}
    </fieldset>
    <SubjectMutationFeedback error={mutation.error} onReload={onReload} />
  </Dialog>;
}
