"use client";

import { useEffect, useState } from "react";
import { ActivityTimeline } from "@/shared/activity/ActivityTimeline";
import { Alert, Button, ClickableRow, Dialog, Section, TableFrame, Td, Th } from "@/shared/ui";
import { getClassAttempts, getTeacherAttempt } from "./api";
import { errorMessage, formatDateTime, formatPoints, submitModeText } from "./labels";
import { ResultQuestions } from "./ResultQuestions";
import type { ClassAttempts, StudentAttempts, TeacherAttemptDetail } from "./types";

/**
 * Tab "Lượt làm của sinh viên" trên Quiz Detail (U10 `QuizDetailSection`, BR-U11-31): chỉ giảng viên của lớp, chỉ đọc.
 * Mở từ lớp (có `classId`) mới có phần này.
 */
export function QuizAttemptsSection({ quizId, classId }: { quizId: string; classId?: string }) {
  const [data, setData] = useState<ClassAttempts | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [student, setStudent] = useState<StudentAttempts | null>(null);
  const [detail, setDetail] = useState<TeacherAttemptDetail | null>(null);

  useEffect(() => {
    if (!classId) return;
    let active = true;
    getClassAttempts(classId, quizId)
      .then((next) => active && setData(next))
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [quizId, classId]);

  if (!classId) return null;
  const open = async (attemptId: string) => {
    try {
      setDetail(await getTeacherAttempt(classId, attemptId));
    } catch (failure) {
      setError(errorMessage(failure));
    }
  };

  return (
    <Section title={`Lượt làm của sinh viên${data ? ` (${data.studentsAttempted})` : ""}`} icon="groups" className="mt-4">
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!data && !error && <p role="status" className="text-[13px] text-muted">Đang tải lượt làm…</p>}
      {data && <>
        <p className="mb-2 text-xs text-muted" data-testid="quiz-attempts-summary">
          {data.studentsAttempted} / {data.totalStudents} sinh viên đã làm · Chỉ đọc. Quiz Practice không vào sổ điểm.
        </p>
        <TableFrame>
          <thead><tr><Th>Sinh viên</Th><Th>Lượt</Th><Th>Điểm cao nhất</Th><Th>Lần gần nhất</Th></tr></thead>
          <tbody>
            {data.students.map((s) => (
              <ClickableRow key={s.accountId} onClick={() => s.attempts > 0 && setStudent(s)} data-testid="quiz-attempts-row">
                <Td><span className="font-semibold">{s.displayName ?? "—"}</span><span className="block text-xs text-muted">{s.email}</span></Td>
                <Td>{s.attempts}{data.maxAttempts ? ` / ${data.maxAttempts}` : ""}</Td>
                <Td>{formatPoints(s.bestScore)}</Td>
                <Td className="whitespace-nowrap text-muted">{formatDateTime(s.lastSubmittedAt)}</Td>
              </ClickableRow>
            ))}
          </tbody>
        </TableFrame>
      </>}
      {student && !detail && (
        <Dialog open onClose={() => setStudent(null)} title={`Lượt làm · ${student.displayName ?? ""}`} footer={<Button onClick={() => setStudent(null)}>Đóng</Button>}>
          <ul className="flex flex-col gap-2 text-[13px]">
            {student.rows.map((row) => (
              <li key={row.attemptId} className="flex items-center justify-between gap-2">
                <span>Lần {row.attemptNo} · {row.status === "SUBMITTED" ? `${formatPoints(row.score)} / ${formatPoints(row.maxScore)}` : "Đang làm"}</span>
                {row.status === "SUBMITTED" && <Button size="sm" onClick={() => void open(row.attemptId)} data-testid="quiz-attempts-open">Xem bài</Button>}
              </li>
            ))}
          </ul>
        </Dialog>
      )}
      {detail && (
        <Dialog open size="lg" onClose={() => setDetail(null)} title={`${detail.displayName ?? ""} · Lần ${detail.result.attemptNo}`}
          footer={<Button onClick={() => setDetail(null)}>Đóng</Button>}>
          <p className="mb-2 text-[13px]" data-testid="teacher-attempt-summary">
            Điểm {formatPoints(detail.result.score)} / {formatPoints(detail.result.maxScore)} · nộp lúc {formatDateTime(detail.result.submittedAt)}
            {submitModeText(detail.result.submitMode) && <> · {submitModeText(detail.result.submitMode)}</>}
          </p>
          <div className="mb-3 flex flex-col gap-1.5" data-testid="teacher-attempt-activity">
            <p className="text-[13px] font-semibold text-ink">Dòng thời gian hoạt động</p>
            {detail.activity ? <ActivityTimeline summary={detail.activity} /> : <p className="text-xs text-muted">Không có dữ liệu hoạt động cho lượt này.</p>}
            <p className="text-xs text-muted">Dữ liệu hoạt động chỉ để tham khảo; nền tảng không kết luận gian lận.</p>
          </div>
          <ResultQuestions questions={detail.result.questions} showScore />
        </Dialog>
      )}
    </Section>
  );
}

/** Cột Bài nộp trên dòng Quiz ở tab Đánh giá (U10 `QuizRowAction`): số sinh viên đã làm. */
export function QuizAttemptCount({ quizId, classId }: { quizId: string; classId: string }) {
  const [count, setCount] = useState<number | null>(null);
  useEffect(() => {
    let active = true;
    getClassAttempts(classId, quizId)
      .then((next) => active && setCount(next.studentsAttempted))
      .catch(() => active && setCount(null));
    return () => {
      active = false;
    };
  }, [quizId, classId]);
  return count === null ? null : <span className="text-xs text-muted" data-testid="quiz-attempt-count">{count} sinh viên đã làm</span>;
}
