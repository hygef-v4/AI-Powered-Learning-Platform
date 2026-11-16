"use client";

import { useCallback } from "react";
import { Button, Dialog } from "@/shared/ui";
import { changeSubjectStatus } from "../api";
import {
  shouldReloadSubject,
  SubjectMutationFeedback,
  useSubjectMutation,
  type SubjectMutationProps,
} from "../hooks/useSubjectMutation";
import { msg } from "@/i18n/msg";

/** Popup Lưu trữ / Mở lại môn: như popup Vô hiệu hóa tài khoản, một câu hỏi lại và nút xác nhận. */
export function SubjectStatusActions({
  subject,
  onClose,
  onSaved,
  onReload,
  onAccessError,
}: SubjectMutationProps & { onClose: () => void }) {
  const mutation = useSubjectMutation(onAccessError);
  const handleClose = useCallback(() => {
    if (mutation.canClose()) onClose();
  }, [mutation, onClose]);
  const archived = subject.status === "ARCHIVED";
  return (
    <Dialog
      open
      title={archived ? msg("subjects.components.SubjectStatusActions.reopenSubject") : msg("subjects.components.SubjectStatusActions.archiveSubject")}
      icon={archived ? "lock_open" : "archive"}
      tone={archived ? "primary" : "danger"}
      onClose={handleClose}
      dismissible={!mutation.busy}
      footer={
        <Button
          variant={archived ? "primary" : "danger"}
          className="w-full"
          disabled={mutation.busy || shouldReloadSubject(mutation.error)}
          data-testid="subject-status-confirm"
          onClick={() => {
            if (!shouldReloadSubject(mutation.error))
              void mutation.run(
                () =>
                  changeSubjectStatus(subject.id, {
                    status: archived ? "ACTIVE" : "ARCHIVED",
                    version: subject.version,
                  }),
                onSaved,
              );
          }}
        >
          {archived ? msg("subjects.components.SubjectStatusActions.reopen") : msg("subjects.components.SubjectStatusActions.archive")}
        </Button>
      }
    >
      <p>
        {msg("subjects.components.SubjectStatusActions.areYouSureYouWant")}{archived ? msg("subjects.components.SubjectStatusActions.reopen2") : msg("subjects.components.SubjectStatusActions.archive2")} {msg("subjects.components.SubjectStatusActions.theSubject")}{subject.name} {msg("subjects.components.SubjectStatusActions.text")}</p>
      {!archived && (
        <p>
          {msg("subjects.components.SubjectStatusActions.draftAndOpenClassesWill")}</p>
      )}
      {mutation.error !== null && (
        <SubjectMutationFeedback error={mutation.error} onReload={onReload} />
      )}
    </Dialog>
  );
}
