"use client";

import { formatStudentLabel } from "@/shared/studentCode";
import { useEffect, useState } from "react";
import { ActivityTimeline } from "@/shared/activity/ActivityTimeline";
import { Alert, Button, ClickableRow, Dialog, Icon, Pagination, Pill, Section, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { decideAttemptPenalty, getClassAttempts, getTeacherAttempt } from "../api";
import { describeError, formatDateTime, formatPoints, describeSignal, submitModeText } from "../model/labels";
import { ResultQuestions } from "./ResultQuestions";
import type { ClassAttempts, PenaltyDecisionInput, PenaltyView, StudentAttempts, TeacherAttemptDetail } from "../model/types";
import { msg } from "@/i18n/msg";

const POINTS = /^\d+([.,]\d{1,2})?$/;

/**
 * Giảng viên quyết định trừ hay không trừ điểm của một lượt (BR-U11-33): hệ thống chỉ gợi ý mức trừ, chưa quyết định thì
 * điểm giữ nguyên. Mở lại được để đổi quyết định.
 */
function AttemptPenaltyPanel({ penalty, onDecide }: { penalty: PenaltyView; onDecide: (input: PenaltyDecisionInput) => Promise<void> }) {
  const [points, setPoints] = useState(String(penalty.decision === "APPLIED" ? penalty.penaltyPoints : penalty.suggestedPenalty));
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const value = Number(points.trim().replace(",", "."));
  const pointsValid = POINTS.test(points.trim()) && value > 0 && value <= penalty.rawScore;
  const hasReason = reason.trim() !== "";
  const decide = async (input: PenaltyDecisionInput) => {
    setBusy(true);
    try {
      await onDecide(input);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mb-3 flex flex-col gap-2 rounded-lg border border-line p-3 text-[13px]" data-testid="teacher-attempt-penalty">
      <p>{msg("quizpractice.components.TeacherQuizSections.diemGoc")}{formatPoints(penalty.rawScore)} {msg("quizpractice.components.TeacherQuizSections.roiTrang")}{penalty.leaveCount ?? 0} {msg("quizpractice.components.TeacherQuizSections.lanGoiYTru")}{formatPoints(penalty.suggestedPenalty)} {msg("quizpractice.components.TeacherQuizSections.diem")}</p>
      <p className="text-muted" data-testid="teacher-attempt-penalty-state">
        {penalty.decision === "APPLIED" && <>{msg("quizpractice.components.TeacherQuizSections.daTru")}{formatPoints(penalty.penaltyPoints)} {msg("quizpractice.components.TeacherQuizSections.diem2")}{penalty.decisionReason}</>}
        {penalty.decision === "DISMISSED" && <>{msg("quizpractice.components.TeacherQuizSections.daQuyetDinhKhongTru")}{penalty.decisionReason}</>}
        {!penalty.decision && msg("quizpractice.components.TeacherQuizSections.chuaQuyetDinhDiemChuaBi")}
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <TextInput aria-label={msg("quizpractice.components.TeacherQuizSections.soDiemTru")} inputMode="decimal" value={points} invalid={!pointsValid} wrapperClassName="w-24"
          onChange={(event) => setPoints(event.target.value)} data-testid="teacher-attempt-penalty-points" />
        <TextInput aria-label={msg("quizpractice.components.TeacherQuizSections.lyDoQuyetDinh")} placeholder={msg("quizpractice.components.TeacherQuizSections.lyDoQuyetDinh")} value={reason} maxLength={1000} wrapperClassName="min-w-48 flex-1"
          onChange={(event) => setReason(event.target.value)} data-testid="teacher-attempt-penalty-reason" />
        <Button size="sm" variant="primary" disabled={busy || !hasReason || !pointsValid}
          onClick={() => void decide({ decision: "APPLIED", points: value, reason: reason.trim() })} data-testid="teacher-attempt-penalty-apply">{msg("quizpractice.components.TeacherQuizSections.truDiem")}</Button>
        <Button size="sm" disabled={busy || !hasReason}
          onClick={() => void decide({ decision: "DISMISSED", reason: reason.trim() })} data-testid="teacher-attempt-penalty-dismiss">{msg("quizpractice.components.TeacherQuizSections.khongTru")}</Button>
      </div>
    </div>
  );
}

/** Số sinh viên mỗi trang của bảng Lượt làm. */
const PAGE_SIZE = 10;

/**
 * Tab "Lượt làm của sinh viên" trên Quiz Detail (U10 `QuizDetailSection`, BR-U11-31): chỉ giảng viên của lớp; chỉ sửa được
 * quyết định trừ điểm (BR-U11-33). Mở từ lớp (có `classId`) mới có phần này. Tiêu đề không kèm số, không có dòng tóm tắt.
 */
export function QuizAttemptsSection({ quizId, classId }: { quizId: string; classId?: string }) {
  const [data, setData] = useState<ClassAttempts | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [student, setStudent] = useState<StudentAttempts | null>(null);
  const [detail, setDetail] = useState<TeacherAttemptDetail | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (!classId) return;
    let active = true;
    getClassAttempts(classId, quizId)
      .then((next) => active && setData(next))
      .catch((failure: unknown) => active && setError(describeError(failure)));
    return () => {
      active = false;
    };
  }, [quizId, classId, reloadKey]);

  if (!classId) return null;
  const open = async (attemptId: string) => {
    try {
      setDetail(await getTeacherAttempt(classId, attemptId));
    } catch (failure) {
      setError(describeError(failure));
    }
  };

  const decide = async (attemptId: string, input: PenaltyDecisionInput) => {
    try {
      setDetail(await decideAttemptPenalty(classId, attemptId, input));
      setStudent(null);
      setReloadKey((k) => k + 1);
    } catch (failure) {
      setError(describeError(failure));
    }
  };
  const penalty = detail?.result.penalty;
  const detailStudent = data?.students.find((s) => s.rows.some((row) => row.attemptId === detail?.result.attemptId));

  return (
    <Section title={msg("quizpractice.components.TeacherQuizSections.luotLamCuaSinhVien")} icon="groups">
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!data && !error && <p role="status" className="text-[13px] text-muted">{msg("quizpractice.components.TeacherQuizSections.dangTaiLuotLam")}</p>}
      {data && <>
        {/* Cùng kiểu các bảng khác: hàng tiêu đề tô nền, bấm cả dòng để mở (mũi tên cuối dòng), phân trang dưới bảng. */}
        <TableFrame filledHead className="[&_table]:min-w-[660px] [&_table]:table-fixed">
          <colgroup><col className="w-[104px]" /><col /><col className="w-[72px]" /><col className="w-[96px]" /><col className="w-[96px]" /><col className="w-[40px]" /></colgroup>
          <thead><tr><Th>MSSV</Th><Th>{msg("quizpractice.components.TeacherQuizSections.sinhVien")}</Th><Th>{msg("quizpractice.components.TeacherQuizSections.luot")}</Th><Th>{msg("quizpractice.components.TeacherQuizSections.diemCaoNhat")}</Th><Th>{msg("quizpractice.components.TeacherQuizSections.roiTrang2")}</Th><Th><span className="sr-only">{msg("quizpractice.components.TeacherQuizSections.mo")}</span></Th></tr></thead>
          <tbody>
            {data.students.length === 0 && <tr><Td colSpan={6} className="py-6 text-center! text-muted">{msg("quizpractice.components.TeacherQuizSections.lopChuaCoSinhVien")}</Td></tr>}
            {data.students.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map((s) => (
              <ClickableRow key={s.accountId} onClick={() => s.attempts > 0 && setStudent(s)} data-testid="quiz-attempts-row"
                className={s.attempts > 0 ? undefined : "cursor-default"}>
                <Td className="font-medium text-body" data-testid="quiz-attempts-student-code">{s.studentCode}</Td>
                <Td>
                  {s.attempts > 0 ? (
                    // Nút trong ô đầu để mở lượt làm bằng bàn phím; dòng vẫn bấm chuột được như trước.
                    <button type="button" onClick={(event) => { event.stopPropagation(); setStudent(s); }}
                      className="block max-w-full truncate text-left font-semibold text-ink hover:text-primary-ink" title={s.displayName ?? undefined}>
                      {s.displayName ?? "—"}
                    </button>
                  ) : (
                    <span className="block truncate font-semibold" title={s.displayName ?? undefined}>{s.displayName ?? "—"}</span>
                  )}
                  <span className="block truncate text-xs text-muted" title={s.email ?? undefined}>{s.email}</span>
                  {s.lastSubmittedAt && <span className="block text-xs text-muted tabular-nums">{msg("quizpractice.components.TeacherQuizSections.lanGanNhat")}{formatDateTime(s.lastSubmittedAt)}</span>}
                </Td>
                <Td className="tabular-nums">{s.attempts}{data.maxAttempts ? ` / ${data.maxAttempts}` : ""}</Td>
                <Td className="tabular-nums">{formatPoints(s.bestScore)}</Td>
                <Td data-testid="quiz-attempts-leaves">
                  {s.lastLeaveCount === null ? "—" : msg("quizpractice.components.TeacherQuizSections.lan", { lastLeaveCount: String(s.lastLeaveCount) })}
                  {s.signalWarning && <span className="block"><Pill tone="warning">{msg("quizpractice.components.TeacherQuizSections.thieuTinHieu")}</Pill></span>}
                  {s.penaltyPending && <span className="block" data-testid="quiz-attempts-penalty-pending"><Pill tone="warning">{msg("quizpractice.components.TeacherQuizSections.choQuyetDinh")}</Pill></span>}
                </Td>
                <Td className="text-faint">{s.attempts > 0 && <Icon name="chevron_right" size={18} />}</Td>
              </ClickableRow>
            ))}
          </tbody>
        </TableFrame>
        <Pagination page={page} pageSize={PAGE_SIZE} total={data.students.length} onPageChange={setPage} summary={false} />
      </>}
      {student && !detail && (
        <Dialog open onClose={() => setStudent(null)} title={msg("quizpractice.components.TeacherQuizSections.luotLam", { value: String(formatStudentLabel({ name: student.displayName, studentCode: student.studentCode })) })}>
          <ul className="flex flex-col gap-2 text-[13px]">
            {student.rows.map((row) => (
              <li key={row.attemptId} className="flex items-center justify-between gap-2">
                <span>
                  {msg("quizpractice.components.TeacherQuizSections.lan2")}{row.attemptNo} · {row.status === "SUBMITTED" ? `${formatPoints(row.score)} / ${formatPoints(row.maxScore)}` : msg("quizpractice.components.TeacherQuizSections.dangLam")}
                  {row.leaveCount !== null && msg("quizpractice.components.TeacherQuizSections.roiTrangLan", { leaveCount: String(row.leaveCount) })}
                  {describeSignal(row.activitySignal) && <> · <Pill tone="warning">{describeSignal(row.activitySignal)}</Pill></>}
                  {!!row.suggestedPenalty && !row.penaltyDecision && <> · <Pill tone="warning">{msg("quizpractice.components.TeacherQuizSections.goiYTru")}{formatPoints(row.suggestedPenalty)}</Pill></>}
                  {row.penaltyDecision === "APPLIED" && msg("quizpractice.components.TeacherQuizSections.daTruDiem")}
                  {row.penaltyDecision === "DISMISSED" && msg("quizpractice.components.TeacherQuizSections.khongTruDiem")}
                </span>
                {row.status === "SUBMITTED" && <Button size="sm" onClick={() => void open(row.attemptId)} data-testid="quiz-attempts-open">{msg("quizpractice.components.TeacherQuizSections.xemBai")}</Button>}
              </li>
            ))}
          </ul>
        </Dialog>
      )}
      {detail && (
        <Dialog open size="lg" onClose={() => setDetail(null)} title={msg("quizpractice.components.TeacherQuizSections.lan3", { value: String(formatStudentLabel({ name: detail.displayName, studentCode: detailStudent?.studentCode })), attemptNo: String(detail.result.attemptNo) })}>
          <p className="mb-2 text-[13px]" data-testid="teacher-attempt-summary">
            {msg("quizpractice.components.TeacherQuizSections.diem3")}{formatPoints(detail.result.score)} / {formatPoints(detail.result.maxScore)} {msg("quizpractice.components.TeacherQuizSections.nopLuc")}{formatDateTime(detail.result.submittedAt)}
            {submitModeText(detail.result.submitMode) && <> · {submitModeText(detail.result.submitMode)}</>}
          </p>
          <div className="mb-3 flex flex-col gap-1.5" data-testid="teacher-attempt-activity">
            <p className="text-[13px] font-semibold text-ink">{msg("quizpractice.components.TeacherQuizSections.dongThoiGianHoatDong")}</p>
            {detail.activity ? <ActivityTimeline summary={detail.activity} /> : <p className="text-xs text-muted">{msg("quizpractice.components.TeacherQuizSections.khongCoDuLieuHoatDong")}</p>}
            {describeSignal(detail.activitySignal) && <p data-testid="teacher-attempt-signal"><Pill tone="warning">{describeSignal(detail.activitySignal)}</Pill></p>}
            <p className="text-xs text-muted">{msg("quizpractice.components.TeacherQuizSections.tinHieuDoTrinhDuyetTu")}</p>
          </div>
          {!!penalty?.suggestedPenalty && (
            <AttemptPenaltyPanel key={`${detail.result.attemptId}-${penalty.decision}-${penalty.penaltyPoints}`} penalty={penalty}
              onDecide={(input) => decide(detail.result.attemptId, input)} />
          )}
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
  return count === null ? null : <span className="text-xs text-muted" data-testid="quiz-attempt-count">{count} {msg("quizpractice.components.TeacherQuizSections.sinhVienDaLam")}</span>;
}
