import type { Tone } from "@/shared/ui/tones";
import type { AccountStatus } from "../types";

// Nhãn và màu trạng thái theo README của design: Hoạt động xanh lá, Chờ kích hoạt vàng, Vô hiệu hóa đỏ.
export const STATUS_LABELS: Record<AccountStatus, string> = {
  PENDING: "Chờ kích hoạt",
  ACTIVE: "Hoạt động",
  DISABLED: "Vô hiệu hóa",
};

export const STATUS_TONES: Record<AccountStatus, Tone> = {
  PENDING: "warning",
  ACTIVE: "success",
  DISABLED: "danger",
};

/** Lý do từ chối dòng CSV (AccountImportService), hiện sau "Lỗi: " ở cột Kết quả kiểm tra (design `a02b`). */
export const IMPORT_REASON_LABELS: Record<string, string> = {
  MISSING_FIELD: "thiếu email, họ tên hoặc vai trò",
  INVALID_EMAIL: "email không hợp lệ",
  EMAIL_DOMAIN_NOT_ALLOWED: "email không thuộc tên miền trường",
  DUPLICATE_IN_FILE: "email trùng dòng khác trong tệp",
  EMAIL_EXISTS: "email đã tồn tại",
  INVALID_DISPLAY_NAME: "họ tên quá dài",
  INVALID_ROLE: "vai trò không hợp lệ",
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

export function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value));
}
