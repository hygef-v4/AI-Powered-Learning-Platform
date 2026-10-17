import type { AccountStatus } from "../types";

export const STATUS_LABELS: Record<AccountStatus, string> = {
  PENDING: "Chờ kích hoạt",
  ACTIVE: "Đang hoạt động",
  DISABLED: "Đã vô hiệu hóa",
};

export const STATUS_TONES: Record<AccountStatus, string> = {
  PENDING: "bg-amber-50 text-amber-700",
  ACTIVE: "bg-emerald-50 text-emerald-700",
  DISABLED: "bg-slate-100 text-slate-500",
};

/** Lý do từ chối dòng CSV (AccountImportService) bằng tiếng Việt. */
export const IMPORT_REASON_LABELS: Record<string, string> = {
  MISSING_FIELD: "Thiếu email, họ tên hoặc vai trò",
  INVALID_EMAIL: "Email không hợp lệ",
  EMAIL_DOMAIN_NOT_ALLOWED: "Email không thuộc tên miền trường",
  DUPLICATE_IN_FILE: "Email trùng với dòng khác trong tệp",
  EMAIL_EXISTS: "Email đã có tài khoản",
  INVALID_DISPLAY_NAME: "Họ tên quá dài",
  INVALID_ROLE: "Vai trò không hợp lệ (không nhập được Quản trị viên)",
};

/** Lỗi nghiệp vụ khi Admin thao tác tài khoản. */
export const ADMIN_ERROR_LABELS: Record<string, string> = {
  EMAIL_EXISTS: "Email đã có tài khoản.",
  EMAIL_DOMAIN_NOT_ALLOWED: "Email phải thuộc tên miền của trường.",
  INVALID_EMAIL: "Email không hợp lệ.",
  VERSION_CONFLICT: "Dữ liệu đã được thay đổi ở phiên khác. Đã tải lại, hãy sửa lại.",
  LAST_ADMIN: "Phải còn ít nhất một Quản trị viên đang hoạt động.",
  SELF_CHANGE_BLOCKED: "Không thể tự đổi vai trò hoặc tự vô hiệu hóa chính mình.",
  ROLE_CHANGE_BLOCKED: "Tài khoản còn phụ trách môn hoặc lớp; hãy đổi phân công trước khi đổi vai trò.",
};

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
}
