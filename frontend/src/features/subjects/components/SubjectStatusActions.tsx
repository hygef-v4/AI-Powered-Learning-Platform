"use client";

import { useCallback } from "react";
import { Button, Dialog } from "@/shared/ui";
import { changeSubjectStatus } from "../api";
import { needsSubjectReload, SubjectMutationFeedback, useSubjectMutation, type SubjectMutationProps } from "../hooks/subjectMutation";

/** Popup Lưu trữ / Mở lại môn: như popup Vô hiệu hóa tài khoản, một câu hỏi lại và nút xác nhận. */
export function SubjectStatusActions({ subject, onClose, onSaved, onReload, onAccessError }: SubjectMutationProps & { onClose: () => void }) {
  const mutation = useSubjectMutation(onAccessError);
  const close = useCallback(() => { if (mutation.canClose()) onClose(); }, [mutation, onClose]);
  const archived = subject.status === "ARCHIVED";
  return <Dialog open title={archived ? "Mở lại môn học?" : "Lưu trữ môn học?"} icon={archived ? "lock_open" : "archive"} tone={archived ? "primary" : "danger"} onClose={close} dismissible={!mutation.busy}
    footer={<Button variant={archived ? "primary" : "danger"} className="w-full" disabled={mutation.busy || needsSubjectReload(mutation.error)} data-testid="subject-status-confirm"
      onClick={() => { if (!needsSubjectReload(mutation.error)) void mutation.run(() => changeSubjectStatus(subject.id, { status: archived ? "ACTIVE" : "ARCHIVED", version: subject.version }), onSaved); }}>{archived ? "Mở lại" : "Lưu trữ"}</Button>}>
    <p>Bạn có chắc muốn {archived ? "mở lại" : "lưu trữ"} môn học {subject.name} không?</p>
    {mutation.error !== null && <SubjectMutationFeedback error={mutation.error} onReload={onReload} />}
  </Dialog>;
}
