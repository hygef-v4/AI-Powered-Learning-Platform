"use client";

import { cx, Pill } from "@/shared/ui";
import { formatPoints } from "../model/labels";
import type { ResultQuestion } from "../model/types";

/** "Xem lại từng câu": lựa chọn của mình, đúng sai; nhãn "Đáp án đúng" và giải thích chỉ khi được hiện (BR-U11-20). */
export function ResultQuestions({ questions, showScore }: { questions: ResultQuestion[]; showScore: boolean }) {
  return (
    <ol className="flex flex-col gap-3" data-testid="result-questions">
      {questions.map((q, i) => {
        const blank = q.selectedOptionIds.length === 0;
        return (
          <li key={q.questionId} className="rounded-xl border border-line bg-surface p-3 text-[13px]">
            <p className="mb-1 flex flex-wrap items-center gap-2 text-xs text-muted">
              <span className="font-bold text-ink">Câu {i + 1}</span>
              {blank ? <Pill>Bỏ trống</Pill> : q.correct ? <Pill tone="success">Đúng</Pill> : <Pill tone="danger">Sai</Pill>}
              {showScore && q.points !== null && <span>{formatPoints(q.points)} / {formatPoints(q.maxPoints)} điểm</span>}
            </p>
            <p className="mb-2 whitespace-pre-wrap text-ink">{q.stem}</p>
            <ul className="flex flex-col gap-1">
              {q.options.map((option) => {
                const chosen = q.selectedOptionIds.includes(option.optionId);
                const right = q.correctOptionIds?.includes(option.optionId) ?? false;
                return (
                  <li key={option.optionId} className={cx("flex flex-wrap items-center gap-2 rounded-lg px-2 py-1",
                    right ? "bg-success-soft" : chosen && q.correctOptionIds ? "bg-danger-soft" : chosen ? "bg-shell" : "")}>
                    <span>{option.text}</span>
                    {right && <Pill tone="success">Đáp án đúng</Pill>}
                    {chosen && <Pill tone="info">Bạn chọn</Pill>}
                  </li>
                );
              })}
            </ul>
            {q.explanation && <p className="mt-2 whitespace-pre-wrap text-body"><span className="font-semibold">Giải thích:</span> {q.explanation}</p>}
          </li>
        );
      })}
    </ol>
  );
}
