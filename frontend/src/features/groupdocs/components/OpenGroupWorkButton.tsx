"use client";

import Link from "next/link";
import { formatDateTime } from "@/features/groups/model/format";
import { ButtonLink, Pill } from "@/shared/ui";
import { useMyGroupDoc } from "../hooks/useMyGroupDoc";
import { msg } from "@/i18n/msg";

/** Nội dung thẻ Làm bài của bài nhóm, như bài cá nhân: dòng bản nộp của nhóm (nếu đã nộp) và nút mở tài liệu nhóm. */
export function OpenGroupWorkButton({ assignmentId }: { assignmentId: string; classId: string }) {
  const { view, message } = useMyGroupDoc(assignmentId);

  if (message) return <p className="text-[13px] text-muted">{message}</p>;
  if (!view) return null;
  const href = `/group-docs/${encodeURIComponent(view.documentId)}`;
  const submission = view.lastSubmission;
  return (
    <>
      {submission && (
        <ul aria-label={msg("groupdocs.components.OpenGroupWorkButton.submission")} className="-mx-2 flex flex-col">
          <li>
            <Link href={href} className="row-interactive flex items-center justify-between gap-3 rounded-lg px-2 py-2 text-[13px] hover:bg-shell">
              <span className="font-mono text-xs text-muted">{formatDateTime(submission.submittedAt)}</span>
              <Pill tone={submission.late ? "accent" : "success"}>{submission.late ? msg("groupdocs.components.OpenGroupWorkButton.submittedLate") : msg("groupdocs.components.OpenGroupWorkButton.submitted")}</Pill>
            </Link>
          </li>
        </ul>
      )}
      <ButtonLink href={href} variant="primary">{msg("groupdocs.components.OpenGroupWorkButton.openGroupDocument")}</ButtonLink>
    </>
  );
}
