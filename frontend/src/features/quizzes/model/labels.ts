import { ApiError } from "@/shared/api/client";
import type { Tone } from "@/shared/ui";
import type { Difficulty, QuestionStatus, QuestionType, QuizStatus, Violation } from "./types";
import { msg } from "@/i18n/msg";
import { NUMBER_LOCALE } from "@/i18n/number";

export const TYPE_LABELS: Record<QuestionType, string> = { MCQ_SINGLE: msg("quizzes.model.labels.singleAnswer"), MCQ_MULTI: msg("quizzes.model.labels.multipleAnswers") };
export const DIFFICULTY_LABELS: Record<Difficulty, string> = { EASY: msg("quizzes.model.labels.easy"), MEDIUM: msg("quizzes.model.labels.medium"), HARD: msg("quizzes.model.labels.hard") };
export const STATUS_LABELS: Record<QuestionStatus, string> = { DRAFT: msg("quizzes.model.labels.draft"), PENDING: msg("quizzes.model.labels.awaitingApproval"), REJECTED: msg("quizzes.model.labels.rejected"), ACTIVE: msg("quizzes.model.labels.active"), RETIRED: msg("quizzes.model.labels.retired") };
export const STATUS_TONES: Record<QuestionStatus, Tone> = { DRAFT: "warning", PENDING: "info", REJECTED: "danger", ACTIVE: "success", RETIRED: "neutral" };
export const DIFFICULTY_TONES: Record<Difficulty, Tone> = { EASY: "success", MEDIUM: "info", HARD: "danger" };

const SYSTEM_BUSY = msg("quizzes.model.labels.theSystemIsBusyPlease");

const ERROR_LABELS: Record<string, string> = {
  QUESTION_NOT_FOUND: msg("quizzes.model.labels.questionNotFoundOrYou"),
  SUBJECT_NOT_FOUND: msg("quizzes.model.labels.subjectNotFoundOrYou"),
  SUBJECT_NOT_ACTIVE: msg("quizzes.model.labels.theSubjectIsArchivedThe"),
  QUESTION_VERSION_CONFLICT: msg("quizzes.model.labels.theQuestionWasChangedIn"),
  QUESTION_DELETE_BLOCKED: msg("quizzes.model.labels.activatedQuestionsCannotBeDeleted"),
  IMPORT_FILE_TYPE: msg("quizzes.model.labels.onlyXlsxOrUtf8"),
  IMPORT_FILE_TOO_LARGE: msg("quizzes.model.labels.filesCanBeAtMost"),
  IMPORT_TOO_MANY_ROWS: msg("quizzes.model.labels.atMost500RowsPer"),
  QUIZ_NOT_FOUND: msg("quizzes.model.labels.quizNotFoundOrYou"),
  CLASS_NOT_FOUND: msg("quizzes.model.labels.classNotFoundOrYou"),
  QUIZ_VERSION_CONFLICT: msg("quizzes.model.labels.theQuizWasChangedIn"),
  QUIZ_NOT_EDITABLE: msg("quizzes.model.labels.publishedQuizzesCannotBeEdited"),
  QUIZ_DELETE_BLOCKED: msg("quizzes.model.labels.publishedQuizzesCannotBeDeleted"),
  QUIZ_VERSION_EXISTS: msg("quizzes.model.labels.thisQuizAlreadyHasA"),
  CLASS_NOT_OPEN: msg("quizzes.model.labels.theClassIsNotOpen"),
  CLASS_ARCHIVED: msg("quizzes.model.labels.theClassIsArchivedIts"),
  QUIZ_COPY_SUBJECT_MISMATCH: msg("quizzes.model.labels.quizzesCanOnlyBeCopied"),
  QUIZ_FULL: msg("quizzes.model.labels.aQuizCanHaveAt"),
  AI_DRAFT_NOT_FOUND: msg("quizzes.model.labels.aiProposalNotFound"),
  AI_DRAFT_CLOSED: msg("quizzes.model.labels.theProposalWasAcceptedOr"),
  AI_DRAFT_NOT_READY: msg("quizzes.model.labels.theAiHasNotFinished"),
  AI_DRAFT_INVALID: msg("quizzes.model.labels.someQuestionsAreInvalidFix"),
  MATERIAL_NOT_SELECTABLE: msg("quizzes.model.labels.someDocumentsCannotBeSelected"),
  MATERIAL_SERVICE_UNAVAILABLE: msg("quizzes.model.labels.couldNotGetTheSource"),
  AI_SERVICE_UNAVAILABLE: msg("quizzes.model.labels.couldNotSendTheAi"),
  AI_OUTPUT_INVALID: msg("quizzes.model.labels.theAiResultIsNot"),
  QUIZ_PUBLISH_FAILED: msg("quizzes.model.labels.quizNotReadyToPublish"),
  SYSTEM_BUSY,
};

/** Lỗi phát hành theo `field` và `code`; mã chưa có nhãn thì hiện thông điệp của máy chủ (KAN-223). */
export function describeViolation(violation: Violation): string {
  const position = /^items\[(\d+)\]$/.exec(violation.field)?.[1];
  switch (`${position ? "items[]" : violation.field}:${violation.code}`) {
    case "title:REQUIRED": return msg("quizzes.model.labels.violationTitleRequired");
    case "items:ITEMS_REQUIRED": return msg("quizzes.model.labels.violationItemsRequired");
    case "items:ITEM_COUNT": return msg("quizzes.model.labels.violationItemsTooMany");
    case "activityNotice:ACTIVITY_NOTICE_REQUIRED": return msg("quizzes.model.labels.violationActivityNoticeRequired");
    case "settings.leavePenalty:PENALTY_NEEDS_MONITORING": return msg("quizzes.model.labels.violationPenaltyNeedsMonitoring");
    case "items[]:QUESTION_MISSING": return msg("quizzes.model.labels.violationQuestionMissing", { position: String(position) });
    case "items[]:QUESTION_NOT_ACTIVE": return msg("quizzes.model.labels.violationQuestionNotActive", { position: String(position) });
    default: return violation.message;
  }
}

/** Cảnh báo nguồn tài liệu do U10 ghi cố định bằng tiếng Việt (QuizDraftResultHandler); câu lạ giữ nguyên. */
const SOURCE_WARNING_LABELS: Record<string, string> = {
  "Câu hỏi AI tạo có thể không bám tài liệu đã chọn. Hệ thống bỏ prompt bổ sung và tạo lại một lần chỉ theo tài liệu nguồn.":
    msg("quizzes.model.labels.sourceRetried"),
  "Yêu cầu của bạn có thể không liên quan đến tài liệu đã chọn; câu hỏi được tạo theo tài liệu nguồn.":
    msg("quizzes.model.labels.requestNotMatchingSources"),
};

export function describeSourceWarning(warning: string): string {
  return SOURCE_WARNING_LABELS[warning] ?? warning;
}

/** Lý do đề xuất lỗi: nhãn theo mã, không có thì thông điệp an toàn của U06. */
export function describeFailure(code: string | null, message: string | null): string {
  return (code && ERROR_LABELS[code]) || message || msg("quizzes.model.labels.theAiCouldNotWrite");
}

export const QUIZ_STATUS_LABELS: Record<QuizStatus, string> = { DRAFT: msg("quizzes.model.labels.draft"), OPEN: msg("quizzes.model.labels.open"), RETIRED: msg("quizzes.model.labels.archived") };
export const QUIZ_STATUS_TONES: Record<QuizStatus, Tone> = { DRAFT: "warning", OPEN: "success", RETIRED: "danger" };

export function formatPoints(value: number): string {
  return Number(value).toLocaleString(NUMBER_LOCALE, { maximumFractionDigits: 2 });
}

/** Thông điệp cho người dùng: ưu tiên nhãn theo mã, sau đó thông điệp an toàn của server. */
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
