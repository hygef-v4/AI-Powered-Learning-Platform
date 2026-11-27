import type { Tone } from "@/shared/ui";
import type { AuditFilterForm, AuditQuery, AuditResult } from "./types";
import { msg } from "@/i18n/msg";

export const RESULT_LABELS: Record<AuditResult, { label: string; tone: Tone }> = {
  SUCCESS: { label: msg("audit.model.labels.succeeded"), tone: "success" },
  DENIED: { label: msg("audit.model.labels.denied"), tone: "warning" },
  FAILURE: { label: msg("audit.model.labels.failed"), tone: "danger" },
};

/**
 * Hành động hệ thống còn ghi, cũng là các lựa chọn của ô lọc (rà lại 2026-11-26). Mã có dấu {@code *} lọc theo mẫu: môn
 * và lớp ghi kèm mã của nó, ví dụ {@code SUBJECT_CS101_CREATED}.
 */
export const ACTION_LABELS: Record<string, string> = {
  // U04, U05 Môn và lớp: "SUBJECT_*CREATED" khớp cả SUBJECT_CREATED cũ lẫn SUBJECT_CS101_CREATED mới.
  "SUBJECT_*CREATED": msg("audit.model.labels.createSubject"),
  "SUBJECT_*ARCHIVED": msg("audit.model.labels.archiveSubject"),
  "SUBJECT_*REOPENED": msg("audit.model.labels.reopenSubject"),
  "CLASS_*CREATED": msg("audit.model.labels.createClass"),
  "CLASS_*_OPENED": msg("audit.model.labels.openClass"),
  "CLASS_*_ARCHIVED": msg("audit.model.labels.archiveClass"),
  "CLASS_*_REOPENED": msg("audit.model.labels.reopenClass"),
  // Từ chối truy cập (mọi unit); U01 Tài khoản và đăng nhập
  ACCESS_DENIED: msg("audit.model.labels.accessDenied"),
  ACCOUNT_CREATED: msg("audit.model.labels.createAccount"),
  ACCOUNT_UPDATED: msg("audit.model.labels.editAccount"),
  ROLE_CHANGED: msg("audit.model.labels.changeRole"),
  ACCOUNT_DISABLED: msg("audit.model.labels.disableAccount"),
  ACCOUNT_ENABLED: msg("audit.model.labels.reEnableAccount"),
  ACCOUNTS_IMPORTED: msg("audit.model.labels.importAccountList"),
  ACCOUNT_ACTIVATED: msg("audit.model.labels.activateAccount"),
  LOGIN_FAILED: msg("audit.model.labels.failedSignIn"),
  LOGIN_BLOCKED_LOCKED: msg("audit.model.labels.signInBlockedByTemporary"),
  ACCOUNT_TEMP_LOCKED: msg("audit.model.labels.temporarilyLockAccount"),
  // U03 Cài đặt và sao lưu
  SETTING_UPDATED: msg("audit.model.labels.editSettings"),
  BACKUP_COMPLETED: msg("audit.model.labels.backUpData"),
  BACKUP_FAILED: msg("audit.model.labels.backupFailed"),
  // U06 Kết nối AI
  AI_CONNECTION_CHANGED: msg("audit.model.labels.changeAiApiKey"),
  AI_CONNECTION_TESTED: msg("audit.model.labels.checkAiApiKey"),
  QUOTA_CHANGED: msg("audit.model.labels.changeAiQuota"),
};

/**
 * Mã hệ thống đã thôi ghi: còn nhãn để đọc bản ghi cũ, không đưa vào ô lọc.
 */
const LEGACY_ACTION_LABELS: Record<string, string> = {
  LOGIN: msg("audit.model.labels.signIn"),
  ACCOUNT_STATUS_CHANGED: msg("audit.model.labels.changeAccountStatus"),
  GRADE_PUBLISHED: msg("audit.model.labels.publishGrades"),
  MCP_CREDENTIAL_CREATED: msg("audit.model.labels.createMcpAgentToken"),
  MCP_REVOKED: msg("audit.model.labels.revokeMcpAgentToken"),
  MCP_TOOL_CALLED: msg("audit.model.labels.mcpAgentToolCall"),
  GROUP_CREATED: msg("audit.model.labels.createGroup"),
  GROUP_RENAMED: msg("audit.model.labels.renameGroup"),
  GROUP_UPDATED: msg("audit.model.labels.editGroup"),
  GROUP_DELETED: msg("audit.model.labels.deleteGroup"),
  GROUP_LEADER_CHANGED: msg("audit.model.labels.changeGroupLeader"),
  LEADER_REQUEST_SUBMITTED: msg("audit.model.labels.requestLeaderChange"),
  LEADER_REQUEST_CANCELLED: msg("audit.model.labels.cancelLeaderChangeRequest"),
  LEADER_REQUEST_APPROVED: msg("audit.model.labels.approveLeaderChangeRequest"),
  LEADER_REQUEST_REJECTED: msg("audit.model.labels.rejectLeaderChangeRequest"),
  GROUP_SECTION_ASSIGNED: msg("audit.model.labels.assignGroupDocumentSection"),
  GROUP_SECTION_RELEASED: msg("audit.model.labels.releaseGroupDocumentSection"),
  GROUP_SUBMITTED: msg("audit.model.labels.submitGroupWork"),
  GROUP_FORMATION_UPDATED: msg("audit.model.labels.selfGroupingSettings"),
  GROUP_PROPOSAL_SUBMITTED: msg("audit.model.labels.submitGroupProposal"),
  GROUP_PROPOSAL_APPROVED: msg("audit.model.labels.approveGroupProposal"),
  GROUP_PROPOSAL_REJECTED: msg("audit.model.labels.rejectGroupProposal"),
  ANNOUNCEMENT_CREATED: msg("audit.model.labels.createAnnouncement"),
  QUIZ_REOPENED: msg("audit.model.labels.reopenQuiz"),
  QUIZ_PENALTY_APPLIED: msg("audit.model.labels.deductQuizAttemptPoints"),
  QUIZ_PENALTY_DISMISSED: msg("audit.model.labels.decideNotToDeductQuiz"),
  QUESTION_PROPOSED: msg("audit.model.labels.proposeQuestionToBank"),
  QUESTION_APPROVED: msg("audit.model.labels.approveProposedQuestion"),
  QUESTION_REJECTED: msg("audit.model.labels.rejectProposedQuestion"),
  QUIZ_RETIRED: msg("audit.model.labels.archiveQuiz"),
  QUIZ_ATTEMPT_SUBMITTED: msg("audit.model.labels.submitQuiz"),
  QUESTION_CREATED: msg("audit.model.labels.createQuestion"),
  QUESTION_ACTIVATED: msg("audit.model.labels.activateQuestion"),
  QUESTION_RETIRED: msg("audit.model.labels.retireQuestion"),
  QUESTION_DELETED: msg("audit.model.labels.deleteQuestion"),
  QUESTIONS_IMPORTED: msg("audit.model.labels.importQuestions"),
  QUIZ_CREATED: msg("audit.model.labels.createQuiz"),
  QUIZ_PUBLISHED: msg("audit.model.labels.publishQuiz"),
  QUIZ_DELETED: msg("audit.model.labels.deleteQuiz"),
  QUIZ_CLONED: msg("audit.model.labels.duplicateQuiz"),
  QUIZ_COPIED: msg("audit.model.labels.copyQuiz"),
  QUIZ_VERSION_CREATED: msg("audit.model.labels.createQuizVersion"),
  QUIZ_AI_DRAFT_ACCEPTED: msg("audit.model.labels.acceptAiProposalForQuiz"),
  AUTH_FAILED: msg("audit.model.labels.authenticationFailed"),
  AI_CONNECTION_KEY_REVEALED: msg("audit.model.labels.viewAiApiKey"),
  MCP_CREDENTIAL_UPDATED: msg("audit.model.labels.editMcpAgentToken"),
  MCP_TOKEN_REVEALED: msg("audit.model.labels.viewMcpAgentToken"),
  ASSIGNMENT_CREATED: msg("audit.model.labels.createAssignment"),
  ASSIGNMENT_UPDATED: msg("audit.model.labels.editAssignment"),
  ASSIGNMENT_DELETED: msg("audit.model.labels.deleteAssignment"),
  ASSIGNMENT_REVIEWED: msg("audit.model.labels.finalizeAssignment"),
  ASSIGNMENT_PUBLISHED: msg("audit.model.labels.publishAssignment"),
  ASSIGNMENT_RETIRED: msg("audit.model.labels.archiveAssignment"),
  ASSIGNMENT_CLONED: msg("audit.model.labels.duplicateAssignment"),
  ASSIGNMENT_COPIED: msg("audit.model.labels.copyAssignment"),
  ASSIGNMENT_VERSION_CREATED: msg("audit.model.labels.createAssignmentVersion"),
  ASSIGNMENT_SCHEDULE_CHANGED: msg("audit.model.labels.changeAssignmentSchedule"),
  ASSIGNMENT_CLOSED: msg("audit.model.labels.closeSubmissions"),
  ASSIGNMENT_DOCX_IMPORTED: msg("audit.model.labels.importAssignmentFromDocx"),
  ASSIGNMENT_SKELETON_SAVED: msg("audit.model.labels.saveWorkOutline"),
  ASSIGNMENT_AI_DRAFT_REQUESTED: msg("audit.model.labels.requestAiAssignmentDraft"),
  ASSIGNMENT_AI_DRAFT_SAVED: msg("audit.model.labels.saveAiProposalForAssignment"),
  ASSIGNMENT_AI_DRAFT_APPLIED: msg("audit.model.labels.applyAiProposalToAssignment"),
  ASSIGNMENT_AI_DRAFT_DISCARDED: msg("audit.model.labels.discardAiProposalForAssignment"),
  RUBRIC_UPDATED: msg("audit.model.labels.editRubric"),
  ATTEMPT_STARTED: msg("audit.model.labels.startWork"),
  ATTEMPT_SAVED: msg("audit.model.labels.saveWork"),
  ASSIGNMENT_SUBMITTED: msg("audit.model.labels.submitWork"),
  SUBMIT_REJECTED: msg("audit.model.labels.submissionRejected"),
  ATTEMPT_DOCX_IMPORTED: msg("audit.model.labels.importWorkFromDocx"),
  ATTEMPT_DOCX_EXPORTED: msg("audit.model.labels.exportWorkToDocx"),
  PRACTICE_FEEDBACK_REQUESTED: msg("audit.model.labels.requestPracticeFeedback"),
  GRADE_SAVED: msg("audit.model.labels.saveGrade"),
  GRADE_CHANGED: msg("audit.model.labels.changeOfficialGrade"),
  GRADE_FINALIZED: msg("audit.model.labels.finalizeGrades"),
  GRADE_DISTRIBUTION_VISIBILITY_CHANGED: msg("audit.model.labels.changeGradeDistributionVisibility"),
  GRADEBOOK_EXPORTED: msg("audit.model.labels.exportGradebook"),
  GROUP_DOC_IMPORTED: msg("audit.model.labels.importGroupDocument"),
  GROUP_ASSIGNMENT_LOCKED: msg("audit.model.labels.lockGroupDocumentAssignments"),
  GROUP_ASSIGNMENT_UNLOCKED: msg("audit.model.labels.unlockGroupDocumentAssignments"),
  GROUP_SECTION_LOCKED: msg("audit.model.labels.lockGroupDocumentSection"),
  GROUP_SECTION_UNLOCKED: msg("audit.model.labels.unlockGroupDocumentSection"),
  GROUP_SECTION_RESTORED: msg("audit.model.labels.restoreGroupDocumentSection"),
  GROUP_SECTION_TAKEN_OVER: msg("audit.model.labels.reclaimGroupDocumentSection"),
  MATERIAL_CREATED: msg("audit.model.labels.createLearningMaterial"),
  MATERIAL_UPDATED: msg("audit.model.labels.editLearningMaterial"),
  MATERIAL_ARCHIVED: msg("audit.model.labels.archiveLearningMaterial"),
  MATERIAL_VERSION_CREATED: msg("audit.model.labels.createLearningMaterialVersion"),
  MATERIAL_MODULE_CREATED: msg("audit.model.labels.createModule"),
  MATERIAL_MODULE_UPDATED: msg("audit.model.labels.editModule"),
  MATERIAL_MODULE_DELETED: msg("audit.model.labels.deleteModule"),
  MATERIAL_MODULE_QUIZ_ATTACHED: msg("audit.model.labels.attachQuizToModule"),
  MATERIAL_MODULE_QUIZ_DETACHED: msg("audit.model.labels.removeQuizFromModule"),
  ANNOUNCEMENT_UPDATED: msg("audit.model.labels.editAnnouncement"),
  ANNOUNCEMENT_DELETED: msg("audit.model.labels.deleteAnnouncement"),
  PASSWORD_CHANGED: msg("audit.model.labels.changePassword"),
  PASSWORD_RESET: msg("audit.model.labels.resetPassword"),
  PROFILE_UPDATED: msg("audit.model.labels.editProfile"),
  ROLE_CHANGE_REJECTED: msg("audit.model.labels.rejectRoleChange"),
  FILE_UPLOAD: msg("audit.model.labels.uploadFile"),
  FILE_DOWNLOAD: msg("audit.model.labels.downloadFile"),
  SUBJECT_CREATED: msg("audit.model.labels.createSubject"),
  SUBJECT_UPDATED: msg("audit.model.labels.editSubject"),
  SUBJECT_ARCHIVED: msg("audit.model.labels.archiveSubject"),
  SUBJECT_REOPENED: msg("audit.model.labels.reopenSubject"),
  SUBJECT_MANAGER_CHANGED: msg("audit.model.labels.changeSubjectManager"),
  CLASS_CREATED: msg("audit.model.labels.createClass"),
  CLASS_UPDATED: msg("audit.model.labels.editClass"),
  CLASS_STATUS_CHANGED: msg("audit.model.labels.changeClassStatus"),
  CLASS_TEACHER_CHANGED: msg("audit.model.labels.changeClassLecturer"),
  STUDENT_ENROLLED: msg("audit.model.labels.enrollStudents"),
  STUDENT_REMOVED: msg("audit.model.labels.removeStudentFromClass"),
};

/** Môn, lớp ghi kèm mã: SUBJECT_CS101_CREATED → "Tạo môn CS101". */
const CODED_ACTION = /^(SUBJECT|CLASS)_(.+)_(CREATED|OPENED|ARCHIVED|REOPENED)$/;
const CODED_ACTION_LABELS: Record<string, (code: string) => string> = {
  SUBJECT_CREATED: (code) => msg("audit.model.labels.createSubjectCode", { code }),
  SUBJECT_ARCHIVED: (code) => msg("audit.model.labels.archiveSubjectCode", { code }),
  SUBJECT_REOPENED: (code) => msg("audit.model.labels.reopenSubjectCode", { code }),
  CLASS_CREATED: (code) => msg("audit.model.labels.createClassCode", { code }),
  CLASS_OPENED: (code) => msg("audit.model.labels.openClassCode", { code }),
  CLASS_ARCHIVED: (code) => msg("audit.model.labels.archiveClassCode", { code }),
  CLASS_REOPENED: (code) => msg("audit.model.labels.reopenClassCode", { code }),
};

/** Loại đối tượng hệ thống còn ghi, cũng là các lựa chọn của ô lọc. */
export const OBJECT_TYPE_LABELS: Record<string, string> = {
  ACCOUNT: msg("audit.model.labels.account"),
  SUBJECT: msg("audit.model.labels.subject"),
  COURSE_CLASS: msg("audit.model.labels.class"),
  SETTING: msg("audit.model.labels.setting"),
  SETTINGS: msg("audit.model.labels.settingsPage"),
  BACKUP_RUN: msg("audit.model.labels.backupRun"),
  AI_CONNECTION: msg("audit.model.labels.aiApiKey"),
  AI_QUOTA: msg("audit.model.labels.aiQuota"),
  AI: msg("audit.model.labels.aiSettings"),
  ADMIN_DASHBOARD: msg("audit.model.labels.adminDashboard"),
  NOTIFICATION: msg("audit.model.labels.personalNotification"),
  AUDIT_LOG: msg("audit.model.labels.auditLog"),
};

/** Loại đối tượng chỉ còn trong bản ghi cũ. */
const LEGACY_OBJECT_TYPE_LABELS: Record<string, string> = {
  BACKUP: msg("audit.model.labels.backup"),
  CLASS: msg("audit.model.labels.class"),
  MATERIAL: msg("audit.model.labels.learningMaterial"),
  MATERIAL_MODULE: msg("audit.model.labels.materialModule"),
  QUESTION: msg("audit.model.labels.question"),
  QUIZ_AI_DRAFT: msg("audit.model.labels.aiProposalForQuiz"),
  QUIZ_ATTEMPT: msg("audit.model.labels.quizAttempt"),
  ASSIGNMENT_ATTEMPT: msg("audit.model.labels.assignmentAttempt"),
  ASSIGNMENT: msg("audit.model.labels.assignment"),
  SUBMISSION: msg("audit.model.labels.submission"),
  QUOTA_POLICY: msg("audit.model.labels.sharedAiQuota"),
  MCP_CREDENTIAL: msg("audit.model.labels.mcpAgentToken"),
  MCP_TOOL: msg("audit.model.labels.mcpTool"),
  CLASS_GROUP: msg("audit.model.labels.classGroup"),
  LEADER_CHANGE_REQUEST: msg("audit.model.labels.leaderChangeRequest"),
  GROUP_DOCUMENT: msg("audit.model.labels.groupDocument"),
  GROUP_SECTION: msg("audit.model.labels.groupDocumentSection"),
  ANNOUNCEMENT: msg("audit.model.labels.announcement"),
  QUIZ: msg("audit.model.labels.quiz"),
  FILE: msg("audit.model.labels.file"),
  ATTEMPT: msg("audit.model.labels.attempt"),
};

export function describeAction(code: string): string {
  const label = ACTION_LABELS[code] ?? LEGACY_ACTION_LABELS[code];
  if (label) return label;
  const coded = CODED_ACTION.exec(code);
  const describeCoded = coded ? CODED_ACTION_LABELS[`${coded[1]}_${coded[3]}`] : undefined;
  return coded && describeCoded ? describeCoded(coded[2]!) : code;
}
export const describeObjectType = (code: string) => OBJECT_TYPE_LABELS[code] ?? LEGACY_OBJECT_TYPE_LABELS[code] ?? code;

/** Mã dài (UUID) rút gọn "3f9c2a…e81" trong bảng; popup chi tiết hiện đầy đủ. */
export function shortenId(id: string): string {
  return id.length > 14 ? `${id.slice(0, 6)}…${id.slice(-3)}` : id;
}

const VN_TIME = "Asia/Ho_Chi_Minh";

function splitDateParts(iso: string): Record<string, string> {
  const list = new Intl.DateTimeFormat("vi-VN", {
    timeZone: VN_TIME,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(iso));
  return Object.fromEntries(list.map((part) => [part.type, part.value]));
}

/** Ngày giờ hiển thị dùng chung từ shared/dateTime (giờ Việt Nam). */
export { formatDateTime, formatFull, formatShort } from "@/shared/dateTime";

/** Ngày yyyy-mm-dd theo giờ Việt Nam của một thời điểm. */
export function formatVnDate(date: Date): string {
  const p = splitDateParts(date.toISOString());
  return `${p.year}-${p.month}-${p.day}`;
}

/** Mặc định 7 ngày gần nhất: từ 6 ngày trước tới hôm nay (theo giờ Việt Nam). */
export function defaultFilter(now: Date = new Date()): AuditFilterForm {
  const from = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000);
  return { actorEmail: "", action: "", objectType: "", objectId: "", result: "", fromDate: formatVnDate(from), toDate: formatVnDate(now) };
}

/** Đầu ngày và cuối ngày theo giờ Việt Nam (UTC+7) đổi sang ISO-8601. */
export function toDayStart(date: string): string {
  return new Date(`${date}T00:00:00+07:00`).toISOString();
}

export function toDayEnd(date: string): string {
  return new Date(`${date}T23:59:59.999+07:00`).toISOString();
}

/** Form bộ lọc → tham số API; có actorId (mở từ Account Detail) thì lọc đúng tài khoản đó, không thì gửi chữ đã gõ. */
export function buildQuery(form: AuditFilterForm, actorId: string | undefined, page: number, size: number): AuditQuery {
  return {
    actorId,
    actor: actorId ? undefined : form.actorEmail.trim() || undefined,
    action: form.action.trim() || undefined,
    objectType: form.objectType.trim() || undefined,
    objectId: form.objectId.trim() || undefined,
    result: form.result || undefined,
    from: form.fromDate ? toDayStart(form.fromDate) : undefined,
    to: form.toDate ? toDayEnd(form.toDate) : undefined,
    page,
    size,
  };
}

/** Lý do từ chối hay gặp, ghi gọn 2–3 chữ. */
const REASON_LABELS: Record<string, string> = {
  ROLE_DENIED: msg("audit.model.labels.wrongRole"),
  UNKNOWN_EMAIL: "Sai email",
  BAD_PASSWORD: msg("audit.model.labels.wrongPassword"),
  LOCKED: msg("audit.model.labels.accountLocked"),
  ACCOUNT_INACTIVE: msg("audit.model.labels.accountLocked"),
  ACCOUNT_NOT_ACTIVE: msg("audit.model.labels.accountLocked"),
  TOO_MANY_FAILURES: msg("audit.model.labels.tooManyAttempts"),
  RATE_LIMITED: msg("audit.model.labels.overLimit"),
  OUT_OF_SCOPE: msg("audit.model.labels.outOfScope"),
  CLASS_OUT_OF_SCOPE: msg("audit.model.labels.outOfScope"),
  GRADING_SCOPE: msg("audit.model.labels.outOfScope"),
  NOT_OWNER: msg("audit.model.labels.notOwner"),
  ENROLLMENT_REQUIRED: msg("audit.model.labels.notEnrolled"),
  TOKEN_ACCOUNT_MISMATCH: msg("audit.model.labels.wrongAccount"),
  TOOL_ERROR: msg("audit.model.labels.toolError"),
  ARGS_TOO_LARGE: msg("audit.model.labels.dataTooLarge"),
  DRIVE_ABUSE_BLOCKED: msg("audit.model.labels.blocked"),
  API_KEY_INVALID: msg("audit.model.labels.wrongApiKey"),
  AI_SERVICE_UNAVAILABLE: msg("audit.model.labels.aiOverloaded"),
  ACCESS_DENIED: msg("audit.model.labels.noPermission"),
};

/**
 * Lý do trên bảng audit, ngắn 2–3 chữ: mã quen thì dịch, mã lạ đoán theo đuôi mã, câu chữ (thường là lỗi nhà cung cấp AI) thì
 * rút theo mã HTTP; còn lại lấy 3 chữ đầu. Lý do đầy đủ hiện khi rê chuột.
 */
export function shortenReason(reason: string | null | undefined): string {
  const value = reason?.trim() ?? "";
  if (!value) return "—";
  if (REASON_LABELS[value]) return REASON_LABELS[value];
  if (/^[A-Z][A-Z0-9_]+$/.test(value)) {
    if (/NOT_FOUND$/.test(value)) return msg("audit.model.labels.notFound");
    if (/(FORBIDDEN|DENIED|NOT_ALLOWED|ONLY)$/.test(value)) return msg("audit.model.labels.noPermission");
    if (/INVALID/.test(value)) return msg("audit.model.labels.invalid");
    if (/TOO_LARGE/.test(value)) return msg("audit.model.labels.tooLarge");
    if (/UNAVAILABLE/.test(value)) return msg("audit.model.labels.suspended");
    if (/CONFLICT/.test(value)) return msg("audit.model.labels.conflict");
    return msg("audit.model.labels.denied2");
  }
  if (/key api|khóa|HTTP 40[13]/i.test(value)) return msg("audit.model.labels.wrongKey");
  if (/HTTP 429|hạn mức|giới hạn/i.test(value)) return msg("audit.model.labels.quotaExhausted");
  if (/quá tải|HTTP 5\d\d/i.test(value)) return msg("audit.model.labels.aiOverloaded");
  if (/timeout|kịp|hết thời gian/i.test(value)) return msg("audit.model.labels.timedOut");
  if (/HTTP 400|từ chối/i.test(value)) return msg("audit.model.labels.denied2");
  const words = value.split(/\s+/);
  return words.length > 3 ? `${words.slice(0, 3).join(" ")}…` : value;
}
