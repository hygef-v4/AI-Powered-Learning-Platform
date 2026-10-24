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

/** Form bộ lọc → tham số API (trừ người thực hiện, đổi riêng qua U01). */
export function toQuery(form: AuditFilterForm, actorId: string | undefined, page: number, size: number): AuditQuery {
  return {
    actorId,
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
