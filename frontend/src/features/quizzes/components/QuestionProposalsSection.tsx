"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert, Button, Pill, Section, showToast } from "@/shared/ui";
import { getProposals, proposeQuestion, resubmitProposal, withdrawProposal } from "../api";
import { errorMessage, STATUS_LABELS, STATUS_TONES } from "../model/labels";
import type { ProposalView, QuestionContent } from "../model/types";
import { InlineQuestionDialog } from "./QuizDetailParts";
import { SimilarQuestionsAlert } from "./SimilarQuestionsAlert";

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
      .catch((failure: unknown) => { if (!signal?.aborted) setError(errorMessage(failure)); });
  }, [classId]);

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const save = async (content: QuestionContent) => {
    try {
      if (editing === "new" || !editing) await proposeQuestion(classId, content);
      else await resubmitProposal(classId, editing.question.id, content, editing.question.version);
    } catch (failure) {
      throw new Error(errorMessage(failure));
    }
    setEditing(null);
    showToast("Đã gửi đề xuất cho Chủ nhiệm môn");
    load();
  };

  const withdraw = async (row: ProposalView) => {
    setBusy(true);
    try {
      await withdrawProposal(classId, row.question.id, row.question.version);
      showToast("Đã rút đề xuất");
      load();
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Section title="Câu hỏi đề xuất vào ngân hàng" icon="help_center" className="mt-4" actions={
      <Button size="sm" icon="add" onClick={() => setEditing("new")} data-testid="proposals-new">Đề xuất câu</Button>
    }>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!error && !rows && <p role="status" className="text-sm text-muted">Đang tải đề xuất…</p>}
      {rows?.length === 0 && <p className="text-[13px] text-muted">Chưa có đề xuất. Chủ nhiệm môn duyệt thì câu mới vào ngân hàng của môn.</p>}
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
                <p className="text-danger" data-testid="proposal-reason">Lý do từ chối: {row.review.reason}</p>
              )}
              <SimilarQuestionsAlert similar={row.similar} />
              {open && (
                <div className="flex gap-1">
                  <Button size="sm" variant="ghost" disabled={busy} onClick={() => setEditing(row)} data-testid="proposal-edit">Sửa và gửi lại</Button>
                  <Button size="sm" variant="ghost" disabled={busy} onClick={() => void withdraw(row)} data-testid="proposal-withdraw">Rút đề xuất</Button>
                </div>
              )}
            </li>
          );
        })}
      </ul>
      {editing && (
        <InlineQuestionDialog initial={editing === "new" ? null : editing} title={editing === "new" ? "Đề xuất câu hỏi" : "Sửa câu đề xuất"}
          onClose={() => setEditing(null)} onSave={save} />
      )}
    </Section>
  );
}
