"use client";

import { useState } from "react";
import { Card, cx, Pill } from "@/shared/ui";
import { formatPoints } from "../model/labels";
import type { ResultQuestion } from "../model/types";
import { QuestionNav, QuestionPrevNext, type PagerTone } from "./QuestionPager";
import { msg } from "@/i18n/msg";

const toneOf = (q: ResultQuestion | undefined): PagerTone =>
  !q || q.selectedOptionIds.length === 0 ? "blank" : q.correct ? "right" : q.correct === false ? "wrong" : "done";

/**
 * "Xem lại từng câu", mỗi câu một trang: lựa chọn của mình, đúng sai; nhãn "Đáp án đúng" và giải thích chỉ khi được hiện
 * (BR-U11-20). Trang kết quả truyền `index` để giữ câu đang xem trên URL; popup của giảng viên tự giữ trong component.
 */
export function ResultQuestions({ questions, showScore, index, onIndex }: {
  questions: ResultQuestion[];
  showScore: boolean;
  index?: number;
  onIndex?: (index: number) => void;
}) {
  const [own, setOwn] = useState(0);
  const current = index ?? own;
  const go = onIndex ?? setOwn;
  const q = questions[current];
  if (!q) return null;
  const blank = q.selectedOptionIds.length === 0;
  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_240px]" data-testid="result-questions">
      <Card as="section" className="text-[13px]" aria-label={msg("quizpractice.components.ResultQuestions.cau", { value: String(current + 1) })}>
        <p className="mb-2 flex flex-wrap items-center gap-2 text-xs text-muted">
          <span className="font-bold text-ink">{msg("quizpractice.components.ResultQuestions.cau2")}{current + 1} / {questions.length}</span>
          {blank ? <Pill>{msg("quizpractice.components.ResultQuestions.boTrong")}</Pill> : q.correct ? <Pill tone="success">{msg("quizpractice.components.ResultQuestions.dung")}</Pill> : <Pill tone="danger">Sai</Pill>}
          {showScore && q.points !== null && <span>{formatPoints(q.points)} / {formatPoints(q.maxPoints)} {msg("quizpractice.components.ResultQuestions.diem")}</span>}
        </p>
        <p className="mb-3 whitespace-pre-wrap text-[14px] text-ink">{q.stem}</p>
        <ul className="flex flex-col gap-1">
          {q.options.map((option) => {
            const chosen = q.selectedOptionIds.includes(option.optionId);
            const right = q.correctOptionIds?.includes(option.optionId) ?? false;
            return (
              <li key={option.optionId} className={cx("flex flex-wrap items-center gap-2 rounded-lg px-2 py-1",
                right ? "bg-success-soft" : chosen && q.correctOptionIds ? "bg-danger-soft" : chosen ? "bg-shell" : "")}>
                <span>{option.text}</span>
                {right && <Pill tone="success">{msg("quizpractice.components.ResultQuestions.dapAnDung")}</Pill>}
                {chosen && <Pill tone="info">{msg("quizpractice.components.ResultQuestions.banChon")}</Pill>}
              </li>
            );
          })}
        </ul>
        {q.explanation && <p className="mt-2 whitespace-pre-wrap text-body"><span className="font-semibold">{msg("quizpractice.components.ResultQuestions.giaiThich")}</span> {q.explanation}</p>}
        <QuestionPrevNext count={questions.length} index={current} onChange={go} testIdPrefix="result-question" />
      </Card>
      <Card as="nav" padding="sm" aria-label={msg("quizpractice.components.ResultQuestions.danhSachCau")} data-testid="result-question-nav">
        <QuestionNav count={questions.length} index={current} onChange={go} toneOf={(i) => toneOf(questions[i])} />
      </Card>
    </div>
  );
}
