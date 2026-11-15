"use client";

import { useCallback, useState } from "react";
import { Button, Dialog } from "@/shared/ui";
import { changeSubjectManager } from "../api";
import { ManagerPicker } from "./ManagerPicker";
import type { ManagerCandidate, SearchSubjectManagers } from "../model/managerSearch";
import {
  shouldReloadSubject,
  SubjectMutationFeedback,
  useSubjectMutation,
  type SubjectMutationProps,
} from "../hooks/useSubjectMutation";

/**
 * Popup Đổi chủ nhiệm môn, cùng kiểu popup Đổi vai trò: một ô chọn, mặc định là chủ nhiệm hiện tại; bỏ chọn (x) là bỏ
 * phân công. Chỉ bấm Lưu được khi đã đổi.
 */
export function ManagerChangeDialog({
  subject,
  onClose,
  onSaved,
  onReload,
  onAccessError,
  searchManagers,
}: SubjectMutationProps & { onClose: () => void; searchManagers?: SearchSubjectManagers }) {
  const current: ManagerCandidate | null =
    subject.managerId !== null && subject.manager
      ? { ...subject.manager, role: "SUBJECT_MANAGER", status: "ACTIVE" }
      : null;
  const [manager, setManager] = useState<ManagerCandidate | null>(current);
  const mutation = useSubjectMutation(onAccessError);
  const handleClose = useCallback(() => {
    if (mutation.canClose()) onClose();
  }, [mutation, onClose]);
  const conflict = shouldReloadSubject(mutation.error);
  const changed = (manager?.accountId ?? null) !== subject.managerId;
  function handleSubmit() {
    if (!changed || conflict) return;
    void mutation.run(
      () =>
        changeSubjectManager(subject.id, {
          managerId: manager?.accountId ?? null,
          version: subject.version,
        }),
      onSaved,
    );
  }
  return (
    <Dialog
      open
      title="Đổi chủ nhiệm môn"
      size="lg"
      onClose={handleClose}
      dismissible={!mutation.busy}
      footer={
        <Button
          variant="primary"
          onClick={handleSubmit}
          disabled={mutation.busy || conflict || !changed}
          data-testid="subject-manager-save"
        >
          Lưu
        </Button>
      }
    >
      <ManagerPicker
        value={manager}
        onChange={setManager}
        disabled={mutation.busy}
        searchManagers={searchManagers}
      />
      <SubjectMutationFeedback error={mutation.error} onReload={onReload} />
    </Dialog>
  );
}
