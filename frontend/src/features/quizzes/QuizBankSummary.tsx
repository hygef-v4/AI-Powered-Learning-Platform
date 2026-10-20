"use client";

import { useEffect, useState } from "react";
import { getQuestions, getSubjectQuizzes } from "./api";

type Counts = { activeQuestions: number; draftQuestions: number; open: number; reviewed: number; draft: number };

/**
 * Số câu hỏi và Quiz của môn (U05 `SubjectSummarySection`): dòng trên thẻ môn ở Manager Dashboard (`inline`) hoặc
 * thẻ số liệu trên My Subject (`card`). Đếm qua tổng của các API danh sách; lỗi thì không hiện gì.
 */
export function QuizBankSummary({ subjectId, variant }: { subjectId: string; variant: "inline" | "card" }) {
  const [counts, setCounts] = useState<Counts | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all([
      getQuestions(subjectId, { status: "ACTIVE", size: 1 }),
      getQuestions(subjectId, { status: "DRAFT", size: 1 }),
      getSubjectQuizzes(subjectId, { status: "OPEN", size: 1 }),
      getSubjectQuizzes(subjectId, { status: "REVIEWED", size: 1 }),
      getSubjectQuizzes(subjectId, { status: "DRAFT", size: 1 }),
    ])
      .then(([active1, draft1, open, reviewed, draft]) => active && setCounts({
        activeQuestions: active1.total, draftQuestions: draft1.total, open: open.total, reviewed: reviewed.total, draft: draft.total,
      }))
      .catch(() => active && setCounts(null));
    return () => {
      active = false;
    };
  }, [subjectId]);

  if (!counts) return null;
  if (variant === "inline") {
    return (
      <p className="text-xs text-muted" data-testid="quiz-bank-summary">
        {counts.activeQuestions} câu hỏi đang dùng · {counts.open} quiz đang mở
      </p>
    );
  }
  return (
    <div className="grid gap-3 sm:grid-cols-2" data-testid="quiz-bank-summary">
      <div className="rounded-xl border border-line bg-white p-3">
        <p className="text-xs font-semibold text-muted">Câu hỏi trắc nghiệm</p>
        <p className="text-lg font-bold text-ink">{counts.activeQuestions} đang dùng · {counts.draftQuestions} nháp</p>
      </div>
      <div className="rounded-xl border border-line bg-white p-3">
        <p className="text-xs font-semibold text-muted">Quiz</p>
        <p className="text-lg font-bold text-ink">{counts.open} đang mở · {counts.reviewed} đã duyệt · {counts.draft} nháp</p>
      </div>
    </div>
  );
}
