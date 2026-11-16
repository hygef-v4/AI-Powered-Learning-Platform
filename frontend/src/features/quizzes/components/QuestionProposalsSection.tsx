"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Pill, Section, showToast } from "@/shared/ui";
import { getProposals, proposeQuestion, resubmitProposal, withdrawProposal } from "../api";
import { describeError, STATUS_LABELS, STATUS_TONES } from "../model/labels";
import type { ProposalView, QuestionContent } from "../model/types";
import { InlineQuestionDialog } from "./QuizDetailParts";
import { SimilarQuestionsAlert } from "./SimilarQuestionsAlert";
import { msg } from "@/i18n/msg";

/**
 * "Câu hỏi đề xuất" trong tab Học liệu của lớp (BR-U10-09): giảng viên đề xuất câu vào ngân hàng của môn, xem kết quả
 * duyệt, sửa rồi gửi lại hoặc rút đề xuất. Câu đã duyệt do Chủ nhiệm môn quản lý.
 */
export function QuestionProposalsSection({ classId }: { classId: string }) {
  const [rows, setRows] = useState<ProposalView[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<ProposalView | "new" | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback((signal?: AbortSignal) => {
    getProposals(classId, signal)
      .then((list) => { setRows(list); setError(null); })
      .catch((failure: unknown) => { if (!signal?.aborted) setError(describeError(failure)); });
  }, [classId]);

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const saveProposal = async (content: QuestionContent) => {
    try {
      if (editing === "new" || !editing) await proposeQuestion(classId, content);
      else await resubmitProposal(classId, editing.question.id, content, editing.question.version);
    } catch (failure) {
      throw new Error(describeError(failure));
    }
    setEditing(null);
    showToast(msg("quizzes.components.QuestionProposalsSection.proposalSentToTheSubject"));
    load();
  };

  const handleWithdraw = async (row: ProposalView) => {
    setBusy(true);
    try {
      await withdrawProposal(classId, row.question.id, row.question.version);
      showToast(msg("quizzes.components.QuestionProposalsSection.proposalWithdrawn"));
      load();
    } catch (failure) {
      setError(describeError(failure));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Section title={msg("quizzes.components.QuestionProposalsSection.questionsProposedToTheBank")} icon="help_center" className="mt-4" actions={
      <Button size="sm" icon="add" onClick={() => setEditing("new")} data-testid="proposals-new">{msg("quizzes.components.QuestionProposalsSection.proposeQuestions")}</Button>
    }>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!error && !rows && <p role="status" className="text-sm text-muted">{msg("quizzes.components.QuestionProposalsSection.loadingProposals")}</p>}
      {rows?.length === 0 && <p className="text-[13px] text-muted">{msg("quizzes.components.QuestionProposalsSection.noProposalsYetOnceThe")}</p>}
      <ul className="flex flex-col gap-2">
        {rows?.map((row) => {
          const open = row.question.status === "PENDING" || row.question.status === "REJECTED";
          return (
            <li key={row.question.id} className="flex flex-col gap-1 rounded-lg border border-line p-3 text-[13px]" data-testid="proposal-row">
              <p className="flex flex-wrap items-center gap-2">
                <Pill tone={STATUS_TONES[row.question.status]}>{STATUS_LABELS[row.question.status]}</Pill>
                <span className="line-clamp-2 whitespace-pre-wrap text-ink">{row.question.stem}</span>
              </p>
              {row.question.status === "REJECTED" && row.review?.reason && (
                <p className="text-danger" data-testid="proposal-reason">{msg("quizzes.components.QuestionProposalsSection.rejectionReason")}{row.review.reason}</p>
              )}
              <SimilarQuestionsAlert similar={row.similar} />
              {open && (
                <div className="flex gap-1">
                  <Button size="sm" variant="ghost" disabled={busy} onClick={() => setEditing(row)} data-testid="proposal-edit">{msg("quizzes.components.QuestionProposalsSection.editAndResubmit")}</Button>
                  <Button size="sm" variant="ghost" disabled={busy} onClick={() => void handleWithdraw(row)} data-testid="proposal-withdraw">{msg("quizzes.components.QuestionProposalsSection.withdrawProposal")}</Button>
                </div>
              )}
            </li>
          );
        })}
      </ul>
      {editing && (
        <InlineQuestionDialog initial={editing === "new" ? null : editing} title={editing === "new" ? msg("quizzes.components.QuestionProposalsSection.proposeAQuestion") : msg("quizzes.components.QuestionProposalsSection.editProposedQuestion")}
          onClose={() => setEditing(null)} onSave={saveProposal} />
      )}
    </Section>
  );
}
