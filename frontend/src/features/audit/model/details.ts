import type { AuditLogItem } from "./types";
import { msg } from "@/i18n/msg";

// Diễn giải phần details của một dòng audit: thao tác bị chặn, lý do đầy đủ và các giá trị kèm theo.

export const OPERATION_LABELS: Record<string, string> = {
  ANNOUNCEMENT_READ: msg("audit.model.details.operations.announcementRead"),
  ANNOUNCEMENT_MANAGE: msg("audit.model.details.operations.announcementManage"),
  NOTIFICATION_SELF: msg("audit.model.details.operations.notificationSelf"),
  PROFILE_VIEW: msg("audit.model.details.operations.profileView"),
  PROFILE_UPDATE: msg("audit.model.details.operations.profileUpdate"),
  PASSWORD_CHANGE: msg("audit.model.details.operations.passwordChange"),
  ACCOUNT_LIST: msg("audit.model.details.operations.accountList"),
  ACCOUNT_VIEW: msg("audit.model.details.operations.accountView"),
  ACCOUNT_CREATE: msg("audit.model.details.operations.accountCreate"),
  ACCOUNT_IMPORT: msg("audit.model.details.operations.accountImport"),
  ACCOUNT_UPDATE: msg("audit.model.details.operations.accountUpdate"),
  ACCOUNT_ROLE_CHANGE: msg("audit.model.details.operations.accountRoleChange"),
  ACCOUNT_STATUS_CHANGE: msg("audit.model.details.operations.accountStatusChange"),
  AUDIT_READ: msg("audit.model.details.operations.auditRead"),
  FILE_UPLOAD: msg("audit.model.details.operations.fileUpload"),
  SETTINGS_VIEW: msg("audit.model.details.operations.settingsView"),
  SETTINGS_EDIT: msg("audit.model.details.operations.settingsEdit"),
  SUBJECT_LIST: msg("audit.model.details.operations.subjectList"),
  SUBJECT_VIEW: msg("audit.model.details.operations.subjectView"),
  SUBJECT_CREATE: msg("audit.model.details.operations.subjectCreate"),
  SUBJECT_UPDATE: msg("audit.model.details.operations.subjectUpdate"),
  SUBJECT_MANAGER_CHANGE: msg("audit.model.details.operations.subjectManagerChange"),
  SUBJECT_STATUS_CHANGE: msg("audit.model.details.operations.subjectStatusChange"),
  CLASS_STUDENT: msg("audit.model.details.operations.classStudent"),
  CLASS_TEACHER: msg("audit.model.details.operations.classTeacher"),
  CLASS_MANAGER: msg("audit.model.details.operations.classManager"),
  CLASS_VIEW: msg("audit.model.details.operations.classView"),
  CLASS_MANAGE: msg("audit.model.details.operations.classManage"),
  CLASS_LIST: msg("audit.model.details.operations.classList"),
  CLASS_STUDENTS_VIEW: msg("audit.model.details.operations.classStudentsView"),
  CLASS_STUDENT_VIEW: msg("audit.model.details.operations.classStudentView"),
  MANAGED_SUBJECTS_VIEW: msg("audit.model.details.operations.managedSubjectsView"),
  MANAGED_SUBJECT_VIEW: msg("audit.model.details.operations.managedSubjectView"),
  TEACHER_CANDIDATES_VIEW: msg("audit.model.details.operations.teacherCandidatesView"),
  STUDENT_CANDIDATES_VIEW: msg("audit.model.details.operations.studentCandidatesView"),
  CLASS_CREATED: msg("audit.model.details.operations.classCreated"),
  CLASS_UPDATED: msg("audit.model.details.operations.classUpdated"),
  CLASS_TEACHER_CHANGED: msg("audit.model.details.operations.classTeacherChanged"),
  CLASS_STATUS_CHANGED: msg("audit.model.details.operations.classStatusChanged"),
  STUDENT_ENROLLED: msg("audit.model.details.operations.studentEnrolled"),
  STUDENT_REMOVED: msg("audit.model.details.operations.studentRemoved"),
  GRADING_REVIEW: msg("audit.model.details.operations.gradingReview"),
  GRADING_WRITE: msg("audit.model.details.operations.gradingWrite"),
  GRADEBOOK_READ: msg("audit.model.details.operations.gradebookRead"),
  GRADEBOOK_EXPORT: msg("audit.model.details.operations.gradebookExport"),
  MY_GRADE_READ: msg("audit.model.details.operations.myGradeRead"),
  ADMIN_DASHBOARD_VIEW: msg("audit.model.details.operations.adminDashboardView"),
  QUESTION_BANK_VIEW: msg("audit.model.details.operations.questionBankView"),
  QUESTION_BANK_MANAGE: msg("audit.model.details.operations.questionBankManage"),
  QUIZ_SUBJECT_MANAGE: msg("audit.model.details.operations.quizSubjectManage"),
  QUIZ_CLASS_MANAGE: msg("audit.model.details.operations.quizClassManage"),
  QUIZ_PRACTICE: msg("audit.model.details.operations.quizPractice"),
  MATERIAL_SUBJECT_MANAGE: msg("audit.model.details.operations.materialSubjectManage"),
  MATERIAL_CLASS_MANAGE: msg("audit.model.details.operations.materialClassManage"),
  MATERIAL_STAFF_READ: msg("audit.model.details.operations.materialStaffRead"),
  MATERIAL_STUDENT_READ: msg("audit.model.details.operations.materialStudentRead"),
  ASSIGNMENT_CLASS_MANAGE: msg("audit.model.details.operations.assignmentClassManage"),
  ASSIGNMENT_SUBJECT_MANAGE: msg("audit.model.details.operations.assignmentSubjectManage"),
  ASSIGNMENT_WORK: msg("audit.model.details.operations.assignmentWork"),
  ASSIGNMENT_ATTEMPT_REVIEW: msg("audit.model.details.operations.assignmentAttemptReview"),
  AI_PERSONAL: msg("audit.model.details.operations.aiPersonal"),
  MCP_SELF: msg("audit.model.details.operations.mcpSelf"),
  AI_SHARED_USE: msg("audit.model.details.operations.aiSharedUse"),
  AI_ADMIN: msg("audit.model.details.operations.aiAdmin"),
  GROUP_MANAGE: msg("audit.model.details.operations.groupManage"),
  GROUP_WORK: msg("audit.model.details.operations.groupWork"),
};

export const REASON_TEXTS: Record<string, string> = {
  ROLE_DENIED: msg("audit.model.details.reasons.roleDenied"),
  ROLE_NOT_ALLOWED: msg("audit.model.details.reasons.roleNotAllowed"),
  ACCOUNT_INACTIVE: msg("audit.model.details.reasons.accountInactive"),
  ACCOUNT_NOT_ACTIVE: msg("audit.model.details.reasons.accountNotActive"),
  NO_ACTOR: msg("audit.model.details.reasons.noActor"),
  UNKNOWN_ACTION: msg("audit.model.details.reasons.unknownAction"),
  NOT_OWNER: msg("audit.model.details.reasons.notOwner"),
  OUT_OF_SUBJECT_SCOPE: msg("audit.model.details.reasons.outOfSubjectScope"),
  SUBJECT_OUT_OF_SCOPE: msg("audit.model.details.reasons.subjectOutOfScope"),
  OUT_OF_CLASS_SCOPE: msg("audit.model.details.reasons.outOfClassScope"),
  CLASS_OUT_OF_SCOPE: msg("audit.model.details.reasons.classOutOfScope"),
  OUT_OF_SCOPE: msg("audit.model.details.reasons.outOfScope"),
  GRADING_SCOPE: msg("audit.model.details.reasons.gradingScope"),
  ENROLLMENT_REQUIRED: msg("audit.model.details.reasons.enrollmentRequired"),
  ACCESS_DENIED: msg("audit.model.details.reasons.accessDenied"),
  AUDIT_FORBIDDEN: msg("audit.model.details.reasons.auditForbidden"),
};

export const ROLE_LABELS: Record<string, string> = {
  ADMIN: msg("audit.model.details.roles.admin"),
  SUBJECT_MANAGER: msg("audit.model.details.roles.subjectManager"),
  TEACHER: msg("audit.model.details.roles.teacher"),
  STUDENT: msg("audit.model.details.roles.student"),
};

export const AUDIENCE_LABELS: Record<string, string> = {
  manager: msg("audit.model.details.audiences.manager"),
  teacher: msg("audit.model.details.audiences.teacher"),
  student: msg("audit.model.details.audiences.student"),
};

/** Khóa details đã hiện riêng (thao tác, vai trò, màn đang dùng) nên không lặp lại trong danh sách chi tiết. */
const SHOWN_KEYS = new Set(["action", "actorRole", "audience"]);

const readText = (details: Record<string, unknown>, key: string): string | null => {
  const value = details[key];
  return typeof value === "string" && value.trim() ? value : null;
};

/** Thao tác người dùng định làm khi bị chặn (details.action); không có thì rỗng. */
export function describeOperation(item: AuditLogItem): string | null {
  const code = readText(item.details ?? {}, "action");
  return code ? (OPERATION_LABELS[code] ?? code) : null;
}

export function describeRole(code: string | null): string | null {
  return code ? (ROLE_LABELS[code] ?? code) : null;
}

/**
 * Lý do đầy đủ: sai vai trò thì nêu vai trò lúc đó ("Vai trò Giảng viên không được dùng chức năng này"), mã quen thì
 * dịch thành câu, còn lại trả nguyên lý do đã ghi.
 */
export function describeReason(item: AuditLogItem): string | null {
  const reason = item.reason?.trim();
  if (!reason) return null;
  const role = describeRole(readText(item.details ?? {}, "actorRole"));
  if (role && (reason === "ROLE_DENIED" || reason === "ROLE_NOT_ALLOWED")) {
    return msg("audit.model.details.ui.roleCannot", { role });
  }
  return REASON_TEXTS[reason] ?? null;
}

export type DetailEntry = { label: string; value: string };

/** Các dòng cho phần "Xem chi tiết": mã đối tượng, thao tác, vai trò, màn, mã lý do rồi các khóa details còn lại. */
export function listDetailEntries(item: AuditLogItem): DetailEntry[] {
  const details = item.details ?? {};
  const entries: DetailEntry[] = [];
  const push = (label: string, value: string | null) => {
    if (value) entries.push({ label, value });
  };
  push(msg("audit.model.details.ui.objectId"), item.objectId);
  push(msg("audit.model.details.ui.blockedOperation"), describeOperation(item));
  push(msg("audit.model.details.ui.actorRole"), describeRole(readText(details, "actorRole")));
  const audience = readText(details, "audience");
  push(msg("audit.model.details.ui.screen"), audience ? (AUDIENCE_LABELS[audience] ?? audience) : null);
  push(msg("audit.model.details.ui.reasonCode"), item.reason);
  for (const [key, value] of Object.entries(details)) {
    if (SHOWN_KEYS.has(key) || value === null || value === undefined || value === "") continue;
    entries.push({ label: key, value: typeof value === "object" ? JSON.stringify(value) : String(value) });
  }
  return entries;
}
