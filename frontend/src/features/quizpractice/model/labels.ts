import { ApiError } from "@/shared/api/client";
import type { ActivitySignal, LeavePenalty, PenaltyView, SubmitMode } from "./types";
import { msg } from "@/i18n/msg";
import { NUMBER_LOCALE } from "@/i18n/number";

const SYSTEM_BUSY = msg("quizpractice.model.labels.theSystemIsBusyPlease");

const ERROR_LABELS: Record<string, string> = {
  QUIZ_NOT_FOUND: msg("quizpractice.model.labels.quizNotFoundOrYou"),
  QUIZ_ATTEMPT_NOT_FOUND: msg("quizpractice.model.labels.attemptNotFound"),
  QUIZ_NOT_OPEN: msg("quizpractice.model.labels.theQuizWasWithdrawnOr"),
  ANSWER_VERSION_CONFLICT: msg("quizpractice.model.labels.answersWereJustSavedFrom"),
  ATTEMPT_TIME_UP: msg("quizpractice.model.labels.timeIsUp"),
  ATTEMPT_SUBMITTED: msg("quizpractice.model.labels.thisAttemptHasBeenSubmitted"),
  QUIZ_PRACTICE_STUDENT_ONLY: msg("quizpractice.model.labels.onlyStudentsTakeQuizPractice"),
  PENALTY_NOT_DECIDABLE: msg("quizpractice.model.labels.thisAttemptHasNoSuggested"),
  PENALTY_POINTS_INVALID: msg("quizpractice.model.labels.theDeductionMustBeGreater"),
  SYSTEM_BUSY,
};

export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    const code = error.problem.code;
    const label = code ? ERROR_LABELS[code] : undefined;
    if (label) return label;
    if (error.problem.status >= 500) return SYSTEM_BUSY;
    return error.message;
  }
  return SYSTEM_BUSY;
}

export function readErrorCode(error: unknown): string | undefined {
  return error instanceof ApiError ? error.problem.code : undefined;
}

export { formatDateTime } from "@/shared/dateTime";

export function formatPoints(value: number | null | undefined): string {
  return value === null || value === undefined ? "—" : Number(value).toLocaleString(NUMBER_LOCALE, { maximumFractionDigits: 2 });
}

export function formatDuration(fromIso: string, toIso: string | null): string {
  if (!toIso) return "—";
  const minutes = Math.max(0, Math.round((new Date(toIso).getTime() - new Date(fromIso).getTime()) / 60000));
  return msg("quizpractice.model.labels.min", { minutes: String(minutes) });
}

export function formatAttempts(used: number, max: number | null): string {
  return max === null ? msg("quizpractice.model.labels.attemptsUnlimited", { used: String(used) }) : msg("quizpractice.model.labels.attempts", { used: String(used), max: String(max) });
}

/** Cờ tín hiệu cho giảng viên (BR-U11-32); chỉ là dấu hiệu, tín hiệu tốt thì không hiện gì. */
export function describeSignal(signal: ActivitySignal | null | undefined): string | null {
  return signal === "NO_SIGNAL" ? msg("quizpractice.model.labels.noSignalReceived")
    : signal === "SIGNAL_LOST" ? msg("quizpractice.model.labels.signalPartlyLost")
      : signal === "UNAVAILABLE" ? msg("quizpractice.model.labels.couldNotReadDataAt") : null;
}

/** Quy tắc gợi ý trừ điểm để báo sinh viên trước và trong khi làm bài (BR-U11-33): giảng viên mới là người quyết định. */
export function describePenaltyRule(rule: LeavePenalty): string {
  const free = rule.freeLeaves > 0 ? msg("quizpractice.model.labels.theFirstPageLeavesAre", { freeLeaves: String(rule.freeLeaves) }) : msg("quizpractice.model.labels.eachPageLeave");
  const max = rule.maxPenalty === null ? "" : msg("quizpractice.model.labels.upToPoints", { value: String(formatPoints(rule.maxPenalty)) });
  const min = rule.minAwaySeconds > 0 ? msg("quizpractice.model.labels.onlyCountedWhenAwayFor", { minAwaySeconds: String(rule.minAwaySeconds) }) : "";
  return msg("quizpractice.model.labels.mayLosePointsDecidedBy", { free: String(free), value: String(formatPoints(rule.pointsPerLeave)), max: String(max), min: String(min) });
}

/** Dòng rời trang và điểm trừ trên kết quả của sinh viên (BR-U11-33); không có gì để báo thì trả về null. */
export function describePenaltyResult(penalty: PenaltyView): string | null {
  const leaves = msg("quizpractice.model.labels.leftThePageTimes", { value: String(penalty.leaveCount ?? 0) });
  if (penalty.decision === "APPLIED") {
    return msg("quizpractice.model.labels.originalScoreLecturerDeductedPoints", { value: String(formatPoints(penalty.rawScore)), value2: String(leaves.toLowerCase()), value3: String(formatPoints(penalty.penaltyPoints)), decisionReason: String(penalty.decisionReason) });
  }
  if (penalty.decision === "DISMISSED") return msg("quizpractice.model.labels.theLecturerDecidedNotTo", { leaves: String(leaves), decisionReason: String(penalty.decisionReason) });
  if (penalty.decisionPending) return msg("quizpractice.model.labels.noPointsDeductedYetThe", { leaves: String(leaves) });
  return (penalty.leaveCount ?? 0) > 0 ? `${leaves}.` : null;
}

/** Lượt được hệ thống tự nộp thì ghi rõ lý do (L4); nộp tay thì không có nhãn. */
export function submitModeText(mode: SubmitMode): string | null {
  switch (mode) {
    case "AUTO_TIME_LIMIT":
      return msg("quizpractice.model.labels.autoSubmittedWhenTimeRan");
    case "AUTO_RETIRED":
      return msg("quizpractice.model.labels.autoSubmittedWhenTheQuiz");
    case "AUTO_ACCESS_LOST":
      return msg("quizpractice.model.labels.autoSubmittedWhenClassAccessEnded");
    default:
      return null;
  }
}
