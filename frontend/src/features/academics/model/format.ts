import { ApiError } from "@/shared/api/client";

export function formatOperationError(cause: unknown) {
  if (cause instanceof ApiError && cause.problem.status === 409) return "Dữ liệu đã thay đổi. Vui lòng tải lại trước khi tiếp tục.";
  // Nội dung server chưa kiểm chứng không được hiển thị SQL/stack trace cho người dùng.
  return cause instanceof ApiError ? "Không thể hoàn tất thao tác. Vui lòng thử lại." : cause instanceof Error ? cause.message : "Không thể hoàn tất thao tác. Vui lòng thử lại.";
}

export { formatDate } from "@/shared/dateTime";

export type ClassFieldError = { field: "code" | "startsOn"; message: string };

export const CLASS_START_DATE_REQUIRED = "Chọn ngày bắt đầu";
export const CLASS_START_DATE_RANGE = "Ngày bắt đầu phải trong khoảng 2000–2099";
const CLASS_FIELD_ERRORS: Record<string, ClassFieldError> = {
  CLASS_EXISTS: { field: "code", message: "Mã lớp đã tồn tại trong môn ở học kỳ này" },
  INVALID_CLASS_START_DATE: { field: "startsOn", message: CLASS_START_DATE_RANGE },
};

/** Maps class form API errors to the field they belong to (BR-U05-64, D-U04-05); other errors stay form-level. */
export function formatClassFieldError(cause: unknown): ClassFieldError | null {
  if (!(cause instanceof ApiError) || !cause.problem.code) return null;
  return CLASS_FIELD_ERRORS[cause.problem.code] ?? null;
}

/** Formats a class start date `yyyy-MM-dd` as `d/M/yyyy` without timezone shifts (BR-U05-61). */
export { formatDateOnly as formatStartDate } from "@/shared/dateTime";
