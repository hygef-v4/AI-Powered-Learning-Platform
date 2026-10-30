"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, ClickableRow, Pill, Section, TableFrame, Td, Th } from "@/shared/ui";
import { copyQuizToClass, createClassQuiz, getClassQuizzes, getCopySources } from "../api";
import { CreateQuizDialog } from "./CreateQuizDialog";
import { errorMessage, QUIZ_STATUS_LABELS, QUIZ_STATUS_TONES } from "../model/labels";
import { quizRowActions } from "../slots";
import type { QuizRow } from "../model/types";

/** Gắn vào `TeacherClassTab` của U05: chỉ vẽ phần Quiz ở tab Đánh giá. */
export function ClassQuizzesTab({ classId, tab }: { classId: string; tab: string }) {
  return tab === "evals" ? <ClassQuizzesSection classId={classId} /> : null;
}

/**
 * Phần Quiz trong tab Đánh giá của Teacher Class Detail (U05 `TeacherClassTab`, F5, F6, F11): Quiz của lớp và Quiz
 * của môn (chỉ xem); một popup Quiz mới vừa tạo quiz trống vừa copy quiz từ lớp khác.
 */
export function ClassQuizzesSection({ classId }: { classId: string }) {
  const router = useRouter();
  const [rows, setRows] = useState<QuizRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    getClassQuizzes(classId)
      .then((list) => active && setRows(list))
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [classId, attempt]);

  const open = (row: QuizRow) => router.push(`/quizzes/${row.id}${row.readOnly ? `?classId=${encodeURIComponent(classId)}` : ""}`);

  return (
    <Section title="Quiz" icon="quiz" actions={
      <Button size="sm" variant="primary" icon="add" onClick={() => setCreating(true)} data-testid="class-quizzes-create">Tạo quiz</Button>
    }>
      {error && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => { setError(null); setAttempt((n) => n + 1); }}>Thử lại</Button>}>{error}</Alert>}
      {!error && !rows && <p role="status" className="text-sm text-muted">Đang tải quiz…</p>}
      {rows && (
        <TableFrame className="[&_table]:min-w-[560px] [&_table]:table-fixed">
          <colgroup><col /><col className="w-[110px]" /><col className="w-[130px]" /><col className="w-[160px]" /></colgroup><thead><tr><Th>Quiz</Th><Th className="max-sm:hidden">Phạm vi</Th><Th>Trạng thái</Th><Th className="max-sm:hidden">Bài nộp</Th></tr></thead>
          <tbody>
            {rows.length === 0 && <tr><Td colSpan={4} className="py-6 text-center text-muted">Lớp chưa có quiz.</Td></tr>}
            {rows.map((row) => (
              <ClickableRow key={row.id} onClick={() => open(row)} data-testid="class-quizzes-row">
                <Td><span className="font-semibold [overflow-wrap:anywhere]">{row.title}</span><span className="block text-xs text-muted">{row.itemCount} câu · Luyện tập<span className="sm:hidden"> · {row.readOnly ? "Của môn" : "Của lớp"}</span></span></Td>
                <Td className="max-sm:hidden">{row.readOnly ? <Pill tone="info">Của môn</Pill> : <Pill>Của lớp</Pill>}</Td>
                <Td><Pill tone={QUIZ_STATUS_TONES[row.status]}>{QUIZ_STATUS_LABELS[row.status]}</Pill></Td>
                <Td className="max-sm:hidden"><SlotOutlet slot={quizRowActions} props={{ quizId: row.id, classId }} /></Td>
              </ClickableRow>
            ))}
          </tbody>
        </TableFrame>
      )}
      {creating && (
        <CreateQuizDialog onClose={() => setCreating(false)} onCreate={(title) => createClassQuiz(classId, title)}
          copy={{ load: () => getCopySources(classId), run: (sourceId) => copyQuizToClass(classId, sourceId) }}
          onCreated={(quiz) => router.push(`/quizzes/${quiz.quiz.id}`)} />
      )}
    </Section>
  );
}
