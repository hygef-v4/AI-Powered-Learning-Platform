"use client";

import { StatTile } from "@/shared/ui/StatTile";
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
    <div className="contents" data-testid="quiz-bank-summary">
      <StatTile label="Câu hỏi" value={counts.activeQuestions} meta={`${counts.draftQuestions} nháp`} href="/manager/questions" />
      <StatTile label="Quiz" value={counts.open} meta={`đang mở · ${counts.reviewed} đã duyệt · ${counts.draft} nháp`} href="/manager/quizzes" />
    </div>
  );
}
