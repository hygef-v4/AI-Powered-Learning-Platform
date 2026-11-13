import type { Tone } from "@/shared/ui";
import type { AuditFilterForm, AuditQuery, AuditResult } from "./types";

export const RESULT_LABELS: Record<AuditResult, { label: string; tone: Tone }> = {
  SUCCESS: { label: "Thành công", tone: "success" },
  DENIED: { label: "Từ chối", tone: "warning" },
  FAILURE: { label: "Thất bại", tone: "danger" },
};

/** Nhãn hành động thường gặp; mã chưa có nhãn hiện nguyên mã. */
export const ACTION_LABELS: Record<string, string> = {
  LOGIN: "Đăng nhập",
  ACCESS_DENIED: "Truy cập bị từ chối",
  ROLE_CHANGED: "Đổi vai trò",
  ACCOUNT_CREATED: "Tạo tài khoản",
  ACCOUNT_UPDATED: "Sửa tài khoản",
  ACCOUNT_STATUS_CHANGED: "Đổi trạng thái tài khoản",
  SETTING_UPDATED: "Sửa cài đặt",
  BACKUP_COMPLETED: "Sao lưu dữ liệu",
  BACKUP_FAILED: "Sao lưu thất bại",
  GRADE_PUBLISHED: "Công bố điểm",
  AUDIT_QUERIED: "Tra cứu audit",
  // U06 Kết nối AI
  AI_CONNECTION_CHANGED: "Đổi khóa API AI",
  AI_CONNECTION_TESTED: "Kiểm tra khóa API AI",
  QUOTA_CHANGED: "Đổi hạn mức AI",
  MCP_CREDENTIAL_CREATED: "Tạo token agent MCP",
  MCP_REVOKED: "Thu hồi token agent MCP",
  MCP_TOOL_CALLED: "Agent MCP gọi công cụ",
  // U12 Nhóm và bài nhóm
  GROUP_CREATED: "Tạo nhóm",
  GROUP_RENAMED: "Đổi tên nhóm",
  GROUP_UPDATED: "Sửa nhóm",
  GROUP_DELETED: "Xóa nhóm",
  GROUP_LEADER_CHANGED: "Đổi trưởng nhóm",
  LEADER_REQUEST_SUBMITTED: "Gửi yêu cầu đổi trưởng nhóm",
  LEADER_REQUEST_CANCELLED: "Hủy yêu cầu đổi trưởng nhóm",
  LEADER_REQUEST_APPROVED: "Duyệt yêu cầu đổi trưởng nhóm",
  LEADER_REQUEST_REJECTED: "Từ chối yêu cầu đổi trưởng nhóm",
  GROUP_SECTION_ASSIGNED: "Giao mục tài liệu nhóm",
  GROUP_SECTION_RELEASED: "Nhả mục tài liệu nhóm",
  GROUP_SUBMITTED: "Nộp bài nhóm",
  GROUP_FORMATION_UPDATED: "Cài đặt tự chia nhóm",
  GROUP_PROPOSAL_SUBMITTED: "Gửi đề xuất nhóm",
  GROUP_PROPOSAL_APPROVED: "Duyệt đề xuất nhóm",
  GROUP_PROPOSAL_REJECTED: "Từ chối đề xuất nhóm",
  ANNOUNCEMENT_CREATED: "Tạo thông báo",
  QUIZ_REOPENED: "Mở lại quiz",
  QUIZ_PENALTY_APPLIED: "Trừ điểm lượt quiz",
  QUIZ_PENALTY_DISMISSED: "Quyết định không trừ điểm lượt quiz",
  QUESTION_PROPOSED: "Đề xuất câu vào ngân hàng",
  QUESTION_APPROVED: "Duyệt câu đề xuất",
  QUESTION_REJECTED: "Từ chối câu đề xuất",
  QUIZ_RETIRED: "Lưu trữ quiz",
  QUIZ_ATTEMPT_SUBMITTED: "Nộp bài quiz",
  QUESTION_CREATED: "Tạo câu hỏi",
  QUESTION_ACTIVATED: "Kích hoạt câu hỏi",
  QUESTION_RETIRED: "Ngừng dùng câu hỏi",
  QUESTION_DELETED: "Xóa câu hỏi",
  QUESTIONS_IMPORTED: "Nhập câu hỏi",
  QUIZ_CREATED: "Tạo quiz",
  QUIZ_PUBLISHED: "Phát hành quiz",
  QUIZ_DELETED: "Xóa quiz",
  QUIZ_CLONED: "Nhân bản quiz",
  QUIZ_COPIED: "Sao chép quiz",
  QUIZ_VERSION_CREATED: "Tạo phiên bản quiz",
  QUIZ_AI_DRAFT_ACCEPTED: "Chấp nhận đề xuất AI cho quiz",
  // U01 Tài khoản
  ACCOUNT_ACTIVATED: "Kích hoạt tài khoản",
  ACCOUNT_DISABLED: "Vô hiệu hóa tài khoản",
  ACCOUNT_ENABLED: "Bật lại tài khoản",
  ACCOUNT_TEMP_LOCKED: "Khóa tạm tài khoản",
  ACCOUNTS_IMPORTED: "Nhập danh sách tài khoản",
  LOGIN_FAILED: "Đăng nhập thất bại",
  LOGIN_BLOCKED_LOCKED: "Chặn đăng nhập do đang khóa tạm",
  AUTH_FAILED: "Xác thực thất bại",
  PASSWORD_CHANGED: "Đổi mật khẩu",
  PASSWORD_RESET: "Đặt lại mật khẩu",
  PROFILE_UPDATED: "Sửa hồ sơ",
  // U04 Môn học
  SUBJECT_CREATED: "Tạo môn",
  SUBJECT_UPDATED: "Sửa môn",
  SUBJECT_ARCHIVED: "Lưu trữ môn",
  SUBJECT_REOPENED: "Mở lại môn",
  SUBJECT_MANAGER_CHANGED: "Đổi chủ nhiệm môn",
  // U05 Lớp và ghi danh
  CLASS_CREATED: "Tạo lớp",
  CLASS_UPDATED: "Sửa lớp",
  CLASS_STATUS_CHANGED: "Đổi trạng thái lớp",
  CLASS_TEACHER_CHANGED: "Đổi giảng viên của lớp",
  STUDENT_ENROLLED: "Ghi danh sinh viên",
  STUDENT_REMOVED: "Xóa sinh viên khỏi lớp",
  // U06 Khóa AI và token agent
  AI_CONNECTION_KEY_REVEALED: "Xem khóa API AI",
  MCP_CREDENTIAL_UPDATED: "Sửa token agent MCP",
  MCP_TOKEN_REVEALED: "Xem token agent MCP",
  // U07 Soạn bài tập
  ASSIGNMENT_CREATED: "Tạo bài tập",
  ASSIGNMENT_UPDATED: "Sửa bài tập",
  ASSIGNMENT_DELETED: "Xóa bài tập",
  ASSIGNMENT_REVIEWED: "Duyệt bài tập",
  ASSIGNMENT_PUBLISHED: "Phát hành bài tập",
  ASSIGNMENT_RETIRED: "Lưu trữ bài tập",
  ASSIGNMENT_CLONED: "Nhân bản bài tập",
  ASSIGNMENT_COPIED: "Sao chép bài tập",
  ASSIGNMENT_VERSION_CREATED: "Tạo phiên bản bài tập",
  ASSIGNMENT_SCHEDULE_CHANGED: "Đổi lịch bài tập",
  ASSIGNMENT_DOCX_IMPORTED: "Nhập bài tập từ DOCX",
  ASSIGNMENT_SKELETON_SAVED: "Lưu khung bài làm",
  ASSIGNMENT_AI_DRAFT_REQUESTED: "Yêu cầu AI soạn bài tập",
  ASSIGNMENT_AI_DRAFT_SAVED: "Lưu đề xuất AI cho bài tập",
  ASSIGNMENT_AI_DRAFT_APPLIED: "Áp dụng đề xuất AI cho bài tập",
  ASSIGNMENT_AI_DRAFT_DISCARDED: "Bỏ đề xuất AI cho bài tập",
  RUBRIC_UPDATED: "Sửa rubric",
  // U08 Làm bài
  ATTEMPT_STARTED: "Bắt đầu làm bài",
  ATTEMPT_SAVED: "Lưu bài làm",
  ASSIGNMENT_SUBMITTED: "Nộp bài",
  SUBMIT_REJECTED: "Từ chối nộp bài",
  ATTEMPT_DOCX_IMPORTED: "Nhập bài làm từ DOCX",
  ATTEMPT_DOCX_EXPORTED: "Xuất bài làm ra DOCX",
  PRACTICE_FEEDBACK_REQUESTED: "Yêu cầu góp ý bài luyện tập",
  // U09 Chấm điểm
  GRADE_SAVED: "Lưu điểm",
  GRADE_CHANGED: "Đổi điểm chính thức",
  GRADE_FINALIZED: "Chốt điểm",
  GRADE_DISTRIBUTION_VISIBILITY_CHANGED: "Đổi hiển thị phân bố điểm",
  GRADEBOOK_EXPORTED: "Xuất sổ điểm",
  // U12 Tài liệu nhóm
  GROUP_DOC_IMPORTED: "Nhập tài liệu nhóm",
  GROUP_PEER_REVIEW_SAVED: "Lưu đánh giá chéo",
  GROUP_ASSIGNMENT_LOCKED: "Chốt phân công tài liệu nhóm",
  GROUP_ASSIGNMENT_UNLOCKED: "Mở chốt phân công tài liệu nhóm",
  GROUP_SECTION_LOCKED: "Khóa mục tài liệu nhóm",
  GROUP_SECTION_UNLOCKED: "Mở khóa mục tài liệu nhóm",
  GROUP_SECTION_RESTORED: "Khôi phục mục tài liệu nhóm",
  GROUP_SECTION_TAKEN_OVER: "Nhận lại mục tài liệu nhóm",
  // U13 Học liệu
  MATERIAL_CREATED: "Tạo học liệu",
  MATERIAL_UPDATED: "Sửa học liệu",
  MATERIAL_ARCHIVED: "Lưu trữ học liệu",
  MATERIAL_VERSION_CREATED: "Tạo phiên bản học liệu",
  MATERIAL_MODULE_CREATED: "Tạo module",
  MATERIAL_MODULE_UPDATED: "Sửa module",
  MATERIAL_MODULE_DELETED: "Xóa module",
  MATERIAL_MODULE_QUIZ_ATTACHED: "Gắn quiz vào module",
  MATERIAL_MODULE_QUIZ_DETACHED: "Gỡ quiz khỏi module",
  // U14 Thông báo
  ANNOUNCEMENT_UPDATED: "Sửa thông báo",
  ANNOUNCEMENT_DELETED: "Xóa thông báo",
};

export const OBJECT_TYPE_LABELS: Record<string, string> = {
  ACCOUNT: "Tài khoản",
  SETTING: "Cài đặt",
  SETTINGS: "Trang cài đặt",
  BACKUP: "Bản sao lưu",
  SUBJECT: "Môn học",
  CLASS: "Lớp",
  COURSE_CLASS: "Lớp",
  BACKUP_RUN: "Lần sao lưu",
  MATERIAL: "Học liệu",
  MATERIAL_MODULE: "Module học liệu",
  NOTIFICATION: "Thông báo cá nhân",
  QUESTION: "Câu hỏi",
  QUIZ_AI_DRAFT: "Đề xuất AI cho quiz",
  QUIZ_ATTEMPT: "Lượt làm quiz",
  ASSIGNMENT_ATTEMPT: "Lượt làm bài tập",
  ASSIGNMENT: "Bài tập",
  SUBMISSION: "Bài nộp",
  AUDIT_LOG: "Nhật ký audit",
  AI_CONNECTION: "Khóa API AI",
  AI_QUOTA: "Hạn mức AI",
  QUOTA_POLICY: "Hạn mức AI chung",
  MCP_CREDENTIAL: "Token agent MCP",
  MCP_TOOL: "Công cụ MCP",
  CLASS_GROUP: "Nhóm của lớp",
  LEADER_CHANGE_REQUEST: "Yêu cầu đổi trưởng nhóm",
  GROUP_DOCUMENT: "Tài liệu nhóm",
  GROUP_SECTION: "Mục tài liệu nhóm",
  ANNOUNCEMENT: "Thông báo",
  QUIZ: "Quiz",
  ATTEMPT: "Lượt làm bài",
};

export const actionLabel = (code: string) => ACTION_LABELS[code] ?? code;
export const objectTypeLabel = (code: string) => OBJECT_TYPE_LABELS[code] ?? code;

/** Mã dài (UUID) rút gọn "3f9c2a…e81" trong bảng; popup chi tiết hiện đầy đủ. */
export function shortId(id: string): string {
  return id.length > 14 ? `${id.slice(0, 6)}…${id.slice(-3)}` : id;
}

const VN_TIME = "Asia/Ho_Chi_Minh";

function parts(iso: string): Record<string, string> {
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

/** "14/10 17:55" theo giờ Việt Nam. */
export function formatShort(iso: string): string {
  const p = parts(iso);
  return `${p.day}/${p.month} ${p.hour}:${p.minute}`;
}

/** "17:55 14/10/2026" theo giờ Việt Nam: giờ phút rồi ngày tháng năm. */
export function formatDateTime(iso: string): string {
  const p = parts(iso);
  return `${p.hour}:${p.minute} ${p.day}/${p.month}/${p.year}`;
}

/** "14/10/2026 17:55:12" theo giờ Việt Nam. */
export function formatFull(iso: string): string {
  const p = parts(iso);
  return `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}:${p.second}`;
}

/** Ngày yyyy-mm-dd theo giờ Việt Nam của một thời điểm. */
export function vnDate(date: Date): string {
  const p = parts(date.toISOString());
  return `${p.year}-${p.month}-${p.day}`;
}

/** Mặc định 7 ngày gần nhất: từ 6 ngày trước tới hôm nay (theo giờ Việt Nam). */
export function defaultFilter(now: Date = new Date()): AuditFilterForm {
  const from = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000);
  return { actorEmail: "", action: "", objectType: "", objectId: "", result: "", fromDate: vnDate(from), toDate: vnDate(now) };
}

/** Đầu ngày và cuối ngày theo giờ Việt Nam (UTC+7) đổi sang ISO-8601. */
export function dayStart(date: string): string {
  return new Date(`${date}T00:00:00+07:00`).toISOString();
}

export function dayEnd(date: string): string {
  return new Date(`${date}T23:59:59.999+07:00`).toISOString();
}

/** Form bộ lọc → tham số API; có actorId (mở từ Account Detail) thì lọc đúng tài khoản đó, không thì gửi chữ đã gõ. */
export function toQuery(form: AuditFilterForm, actorId: string | undefined, page: number, size: number): AuditQuery {
  return {
    actorId,
    actor: actorId ? undefined : form.actorEmail.trim() || undefined,
    action: form.action.trim() || undefined,
    objectType: form.objectType.trim() || undefined,
    objectId: form.objectId.trim() || undefined,
    result: form.result || undefined,
    from: form.fromDate ? dayStart(form.fromDate) : undefined,
    to: form.toDate ? dayEnd(form.toDate) : undefined,
    page,
    size,
  };
}

/** Lý do từ chối hay gặp, ghi gọn 2–3 chữ. */
const REASON_LABELS: Record<string, string> = {
  ROLE_DENIED: "Sai vai trò",
  UNKNOWN_EMAIL: "Sai email",
  BAD_PASSWORD: "Sai mật khẩu",
  LOCKED: "Tài khoản khóa",
  ACCOUNT_INACTIVE: "Tài khoản khóa",
  ACCOUNT_NOT_ACTIVE: "Tài khoản khóa",
  TOO_MANY_FAILURES: "Thử quá nhiều",
  RATE_LIMITED: "Quá giới hạn",
  OUT_OF_SCOPE: "Ngoài phạm vi",
  CLASS_OUT_OF_SCOPE: "Ngoài phạm vi",
  GRADING_SCOPE: "Ngoài phạm vi",
  NOT_OWNER: "Không sở hữu",
  ENROLLMENT_REQUIRED: "Chưa ghi danh",
  TOKEN_ACCOUNT_MISMATCH: "Sai tài khoản",
  TOOL_ERROR: "Lỗi công cụ",
  ARGS_TOO_LARGE: "Dữ liệu quá lớn",
  DRIVE_ABUSE_BLOCKED: "Bị chặn",
  API_KEY_INVALID: "Sai khóa API",
  AI_SERVICE_UNAVAILABLE: "AI quá tải",
  ACCESS_DENIED: "Không có quyền",
};

/**
 * Lý do trên bảng audit, ngắn 2–3 chữ: mã quen thì dịch, mã lạ đoán theo đuôi mã, câu chữ (thường là lỗi nhà cung cấp AI) thì
 * rút theo mã HTTP; còn lại lấy 3 chữ đầu. Lý do đầy đủ hiện khi rê chuột.
 */
export function shortReason(reason: string | null | undefined): string {
  const value = reason?.trim() ?? "";
  if (!value) return "—";
  if (REASON_LABELS[value]) return REASON_LABELS[value];
  if (/^[A-Z][A-Z0-9_]+$/.test(value)) {
    if (/NOT_FOUND$/.test(value)) return "Không tìm thấy";
    if (/(FORBIDDEN|DENIED|NOT_ALLOWED|ONLY)$/.test(value)) return "Không có quyền";
    if (/INVALID/.test(value)) return "Không hợp lệ";
    if (/TOO_LARGE/.test(value)) return "Quá lớn";
    if (/UNAVAILABLE/.test(value)) return "Tạm ngưng";
    if (/CONFLICT/.test(value)) return "Bị xung đột";
    return "Bị từ chối";
  }
  if (/key api|khóa|HTTP 40[13]/i.test(value)) return "Sai khóa";
  if (/HTTP 429|hạn mức|giới hạn/i.test(value)) return "Hết hạn mức";
  if (/quá tải|HTTP 5\d\d/i.test(value)) return "AI quá tải";
  if (/timeout|kịp|hết thời gian/i.test(value)) return "Quá giờ";
  if (/HTTP 400|từ chối/i.test(value)) return "Bị từ chối";
  const words = value.split(/\s+/);
  return words.length > 3 ? `${words.slice(0, 3).join(" ")}…` : value;
}
