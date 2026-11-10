"use client";

import { useEffect, useState } from "react";
import { ActivityTimeline } from "@/shared/activity/ActivityTimeline";
import { Alert, Button, ClickableRow, Dialog, Icon, PageNav, Pill, Section, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { decideAttemptPenalty, getClassAttempts, getTeacherAttempt } from "../api";
import { errorMessage, formatDateTime, formatPoints, signalText, submitModeText } from "../model/labels";
import { ResultQuestions } from "./ResultQuestions";
import type { ClassAttempts, PenaltyDecisionInput, PenaltyView, StudentAttempts, TeacherAttemptDetail } from "../model/types";

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
      <p>Điểm gốc {formatPoints(penalty.rawScore)} · rời trang {penalty.leaveCount ?? 0} lần · gợi ý trừ {formatPoints(penalty.suggestedPenalty)} điểm</p>
      <p className="text-muted" data-testid="teacher-attempt-penalty-state">
        {penalty.decision === "APPLIED" && <>Đã trừ {formatPoints(penalty.penaltyPoints)} điểm: {penalty.decisionReason}</>}
        {penalty.decision === "DISMISSED" && <>Đã quyết định không trừ: {penalty.decisionReason}</>}
        {!penalty.decision && "Chưa quyết định, điểm chưa bị trừ."}
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <TextInput aria-label="Số điểm trừ" inputMode="decimal" value={points} invalid={!pointsValid} wrapperClassName="w-24"
          onChange={(event) => setPoints(event.target.value)} data-testid="teacher-attempt-penalty-points" />
        <TextInput aria-label="Lý do quyết định" placeholder="Lý do quyết định" value={reason} maxLength={1000} wrapperClassName="min-w-48 flex-1"
          onChange={(event) => setReason(event.target.value)} data-testid="teacher-attempt-penalty-reason" />
        <Button size="sm" variant="primary" disabled={busy || !hasReason || !pointsValid}
          onClick={() => void decide({ decision: "APPLIED", points: value, reason: reason.trim() })} data-testid="teacher-attempt-penalty-apply">Trừ điểm</Button>
        <Button size="sm" disabled={busy || !hasReason}
          onClick={() => void decide({ decision: "DISMISSED", reason: reason.trim() })} data-testid="teacher-attempt-penalty-dismiss">Không trừ</Button>
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
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [quizId, classId, reloadKey]);

  if (!classId) return null;
  const open = async (attemptId: string) => {
    try {
      setDetail(await getTeacherAttempt(classId, attemptId));
    } catch (failure) {
      setError(errorMessage(failure));
    }
  };

  const decide = async (attemptId: string, input: PenaltyDecisionInput) => {
    try {
      setDetail(await decideAttemptPenalty(classId, attemptId, input));
      setStudent(null);
      setReloadKey((k) => k + 1);
    } catch (failure) {
      setError(errorMessage(failure));
    }
  };
  const penalty = detail?.result.penalty;

  return (
    <Section title="Lượt làm của sinh viên" icon="groups">
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!data && !error && <p role="status" className="text-[13px] text-muted">Đang tải lượt làm…</p>}
      {data && <>
        {/* Cùng kiểu các bảng khác: hàng tiêu đề tô nền, bấm cả dòng để mở (mũi tên cuối dòng), phân trang dưới bảng. */}
        <TableFrame filledHead className="[&_table]:min-w-[560px] [&_table]:table-fixed">
          <colgroup><col /><col className="w-[72px]" /><col className="w-[96px]" /><col className="w-[96px]" /><col className="w-[40px]" /></colgroup>
          <thead><tr><Th>Sinh viên</Th><Th>Lượt</Th><Th>Điểm cao nhất</Th><Th>Rời trang</Th><Th><span className="sr-only">Mở</span></Th></tr></thead>
          <tbody>
            {data.students.length === 0 && <tr><Td colSpan={5} className="py-6 text-center! text-muted">Lớp chưa có sinh viên.</Td></tr>}
            {data.students.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map((s) => (
              <ClickableRow key={s.accountId} onClick={() => s.attempts > 0 && setStudent(s)} data-testid="quiz-attempts-row"
                className={s.attempts > 0 ? undefined : "cursor-default"}>
                <Td>
                  <span className="block truncate font-semibold" title={s.displayName ?? undefined}>{s.displayName ?? "—"}</span>
                  <span className="block truncate text-xs text-muted" title={s.email ?? undefined}>{s.email}</span>
                  {s.lastSubmittedAt && <span className="block text-xs text-muted tabular-nums">Lần gần nhất {formatDateTime(s.lastSubmittedAt)}</span>}
                </Td>
                <Td className="tabular-nums">{s.attempts}{data.maxAttempts ? ` / ${data.maxAttempts}` : ""}</Td>
                <Td className="tabular-nums">{formatPoints(s.bestScore)}</Td>
                <Td data-testid="quiz-attempts-leaves">
                  {s.lastLeaveCount === null ? "—" : `${s.lastLeaveCount} lần`}
                  {s.signalWarning && <span className="block"><Pill tone="warning">Thiếu tín hiệu</Pill></span>}
                  {s.penaltyPending && <span className="block" data-testid="quiz-attempts-penalty-pending"><Pill tone="warning">Chờ quyết định</Pill></span>}
                </Td>
                <Td className="text-faint">{s.attempts > 0 && <Icon name="chevron_right" size={18} />}</Td>
              </ClickableRow>
            ))}
          </tbody>
        </TableFrame>
        <PageNav page={page} size={PAGE_SIZE} total={data.students.length} onPage={setPage} />
      </>}
      {student && !detail && (
        <Dialog open onClose={() => setStudent(null)} title={`Lượt làm · ${student.displayName ?? ""}`}>
          <ul className="flex flex-col gap-2 text-[13px]">
            {student.rows.map((row) => (
              <li key={row.attemptId} className="flex items-center justify-between gap-2">
                <span>
                  Lần {row.attemptNo} · {row.status === "SUBMITTED" ? `${formatPoints(row.score)} / ${formatPoints(row.maxScore)}` : "Đang làm"}
                  {row.leaveCount !== null && ` · rời trang ${row.leaveCount} lần`}
                  {signalText(row.activitySignal) && <> · <Pill tone="warning">{signalText(row.activitySignal)}</Pill></>}
                  {!!row.suggestedPenalty && !row.penaltyDecision && <> · <Pill tone="warning">Gợi ý trừ {formatPoints(row.suggestedPenalty)}</Pill></>}
                  {row.penaltyDecision === "APPLIED" && " · đã trừ điểm"}
                  {row.penaltyDecision === "DISMISSED" && " · không trừ điểm"}
                </span>
                {row.status === "SUBMITTED" && <Button size="sm" onClick={() => void open(row.attemptId)} data-testid="quiz-attempts-open">Xem bài</Button>}
              </li>
            ))}
          </ul>
        </Dialog>
      )}
      {detail && (
        <Dialog open size="lg" onClose={() => setDetail(null)} title={`${detail.displayName ?? ""} · Lần ${detail.result.attemptNo}`}>
          <p className="mb-2 text-[13px]" data-testid="teacher-attempt-summary">
            Điểm {formatPoints(detail.result.score)} / {formatPoints(detail.result.maxScore)} · nộp lúc {formatDateTime(detail.result.submittedAt)}
            {submitModeText(detail.result.submitMode) && <> · {submitModeText(detail.result.submitMode)}</>}
          </p>
          <div className="mb-3 flex flex-col gap-1.5" data-testid="teacher-attempt-activity">
            <p className="text-[13px] font-semibold text-ink">Dòng thời gian hoạt động</p>
            {detail.activity ? <ActivityTimeline summary={detail.activity} /> : <p className="text-xs text-muted">Không có dữ liệu hoạt động cho lượt này.</p>}
            {signalText(detail.activitySignal) && <p data-testid="teacher-attempt-signal"><Pill tone="warning">{signalText(detail.activitySignal)}</Pill></p>}
            <p className="text-xs text-muted">Tín hiệu do trình duyệt tự báo, có thể sai hoặc bị chặn: chỉ là dấu hiệu, nền tảng không kết luận gian lận.</p>
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
  return count === null ? null : <span className="text-xs text-muted" data-testid="quiz-attempt-count">{count} sinh viên đã làm</span>;
}
