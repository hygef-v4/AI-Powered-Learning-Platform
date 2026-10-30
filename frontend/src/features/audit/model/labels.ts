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
};

export const OBJECT_TYPE_LABELS: Record<string, string> = {
  ACCOUNT: "Tài khoản",
  SETTING: "Cài đặt",
  SETTINGS: "Trang cài đặt",
  BACKUP: "Bản sao lưu",
  SUBJECT: "Môn học",
  CLASS: "Lớp",
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
