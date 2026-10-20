"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, ClickableRow, Dialog, Pill, Section, TableFrame, Td, Th } from "@/shared/ui";
import { copyQuizToClass, createClassQuiz, getClassQuizzes, getCopySources } from "./api";
import { CreateQuizDialog } from "./CreateQuizDialog";
import { errorMessage, QUIZ_STATUS_LABELS, QUIZ_STATUS_TONES } from "./labels";
import { quizRowActions } from "./slots";
import type { QuizRow } from "./types";

/** Gắn vào `TeacherClassTab` của U05: chỉ vẽ phần Quiz ở tab Đánh giá. */
export function ClassQuizzesTab({ classId, tab }: { classId: string; tab: string }) {
  return tab === "evals" ? <ClassQuizzesSection classId={classId} /> : null;
}

/**
 * Phần Quiz trong tab Đánh giá của Teacher Class Detail (U05 `TeacherClassTab`, F5, F6, F11): Quiz của lớp và Quiz
 * của môn (chỉ xem); Tạo Quiz, Copy Quiz từ lớp khác.
 */
export function ClassQuizzesSection({ classId }: { classId: string }) {
  const router = useRouter();
  const [rows, setRows] = useState<QuizRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [sources, setSources] = useState<QuizRow[] | null>(null);
  const [copyError, setCopyError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getClassQuizzes(classId)
      .then((list) => active && setRows(list))
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [classId]);

  const open = (row: QuizRow) => router.push(`/quizzes/${row.id}${row.readOnly ? `?classId=${encodeURIComponent(classId)}` : ""}`);
  const openCopy = async () => {
    setCopyError(null);
    try {
      setSources(await getCopySources(classId));
    } catch (failure) {
      setCopyError(errorMessage(failure));
    }
  };

  return (
    <Section title="Quiz" icon="quiz" actions={<div className="flex flex-wrap gap-2">
      <Button size="sm" onClick={() => void openCopy()} data-testid="class-quizzes-copy">Copy Quiz từ lớp khác</Button>
      <Button size="sm" variant="primary" icon="add" onClick={() => setCreating(true)} data-testid="class-quizzes-create">Tạo Quiz</Button>
    </div>}>
      {copyError && <Alert tone="danger" role="alert" className="mb-2">{copyError}</Alert>}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!error && !rows && <p role="status" className="text-[13px] text-muted">Đang tải quiz…</p>}
      {rows && (
        <TableFrame>
          <thead><tr><Th>Quiz</Th><Th>Phạm vi</Th><Th>Trạng thái</Th><Th>Bài nộp</Th></tr></thead>
          <tbody>
            {rows.length === 0 && <tr><Td colSpan={4} className="py-6 text-center text-muted">Lớp chưa có quiz.</Td></tr>}
            {rows.map((row) => (
              <ClickableRow key={row.id} onClick={() => open(row)} data-testid="class-quizzes-row">
                <Td><span className="font-semibold">{row.title}</span><span className="block text-xs text-muted">Quiz · Practice · {row.itemCount} câu</span></Td>
                <Td>{row.readOnly ? <Pill tone="info">Của môn</Pill> : <Pill>Của lớp</Pill>}</Td>
                <Td><Pill tone={QUIZ_STATUS_TONES[row.status]}>{QUIZ_STATUS_LABELS[row.status]}</Pill></Td>
                <Td><SlotOutlet slot={quizRowActions} props={{ quizId: row.id, classId }} /></Td>
              </ClickableRow>
            ))}
          </tbody>
        </TableFrame>
      )}
      {creating && (
        <CreateQuizDialog onClose={() => setCreating(false)} onCreate={(title) => createClassQuiz(classId, title)}
          onCreated={(quiz) => router.push(`/quizzes/${quiz.quiz.id}`)} />
      )}
      {sources && (
        <Dialog open onClose={() => setSources(null)} size="lg" icon="quiz" title="Copy Quiz từ lớp khác" footer={<Button onClick={() => setSources(null)}>Đóng</Button>}>
          {sources.length === 0
            ? <p className="text-[13px] text-muted">Không có quiz ở lớp khác cùng môn mà bạn đang dạy.</p>
            : <ul className="flex flex-col gap-2 text-[13px]">
              {sources.map((source) => (
                <li key={source.id} className="flex items-center justify-between gap-2 rounded-lg border border-line p-2">
                  <span><span className="font-semibold">{source.title}</span> · {source.itemCount} câu</span>
                  <Button size="sm" data-testid="class-quizzes-copy-source"
                    onClick={() => void copyQuizToClass(classId, source.id)
                      .then((copy) => router.push(`/quizzes/${copy.quiz.id}`))
                      .catch((failure: unknown) => setCopyError(errorMessage(failure)))}>Copy</Button>
                </li>
              ))}
            </ul>}
        </Dialog>
      )}
    </Section>
  );
}
