"use client";

import { useCallback } from "react";
import { Button, Dialog } from "@/shared/ui";
import { changeSubjectStatus } from "./api";
import { needsSubjectReload, SubjectMutationFeedback, useSubjectMutation, type SubjectMutationProps } from "./subjectMutation";

export function SubjectStatusButton({ archived, disabled, onClick }: { archived: boolean; disabled: boolean; onClick: () => void }) {
  return <Button id="subject-status-open" variant={archived ? "primary" : "danger"} disabled={disabled} onClick={onClick} data-testid="subject-status-open">{archived ? "Mở lại môn" : "Lưu trữ môn"}</Button>;
}

export function SubjectStatusActions({ subject, onClose, onSaved, onReload, onAccessError }: SubjectMutationProps & { onClose: () => void }) {
  const mutation = useSubjectMutation(onAccessError);
  const close = useCallback(() => { if (mutation.canClose()) onClose(); }, [mutation, onClose]);
  const archived = subject.status === "ARCHIVED";
  return <Dialog open title={archived ? "Mở lại môn học?" : "Lưu trữ môn học?"} tone={archived ? "primary" : "warning"} onClose={close} dismissible={!mutation.busy}
    footer={<><Button onClick={close} disabled={mutation.busy} data-testid="subject-status-cancel">Hủy</Button>
      <Button variant={archived ? "primary" : "danger"} disabled={mutation.busy || needsSubjectReload(mutation.error)} data-testid="subject-status-confirm"
        onClick={() => { if (!needsSubjectReload(mutation.error)) void mutation.run(() => changeSubjectStatus(subject.id, { status: archived ? "ACTIVE" : "ARCHIVED", version: subject.version }), onSaved); }}>{mutation.busy ? "Đang lưu…" : archived ? "Xác nhận mở lại" : "Xác nhận lưu trữ"}</Button></>}>
    <p className="text-sm font-semibold break-words">{subject.code} — {subject.name}</p>
    <p>{archived ? "Môn sẽ hoạt động trở lại, chưa có chủ nhiệm môn. Các lớp vẫn ở trạng thái lưu trữ." : "Mọi lớp chưa lưu trữ của môn sẽ được lưu trữ theo, giữ nguyên sinh viên và dữ liệu học tập. Chủ nhiệm môn sẽ bị gỡ khỏi môn. Không thể tạo lớp mới khi môn đang lưu trữ."}</p>
    <SubjectMutationFeedback error={mutation.error} onReload={onReload} />
  </Dialog>;
}
