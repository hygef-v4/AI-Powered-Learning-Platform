import { ApiError } from "@/shared/api/client";
import { msg } from "@/i18n/msg";

export function formatOperationError(cause: unknown) {
  if (cause instanceof ApiError && cause.problem.status === 409) return msg("academics.model.format.duLieuDaThayDoiVui");
  // Nội dung server chưa kiểm chứng không được hiển thị SQL/stack trace cho người dùng.
  return cause instanceof ApiError ? msg("academics.model.format.khongTheHoanTatThaoTac") : cause instanceof Error ? cause.message : msg("academics.model.format.khongTheHoanTatThaoTac");
}

export { formatDate } from "@/shared/dateTime";

export type ClassFieldError = { field: "code" | "startsOn"; message: string };

export const CLASS_START_DATE_REQUIRED = msg("academics.model.format.chonNgayBatDau");
export const CLASS_START_DATE_RANGE = msg("academics.model.format.ngayBatDauPhaiTrongKhoang");
const CLASS_FIELD_ERRORS: Record<string, ClassFieldError> = {
  CLASS_EXISTS: { field: "code", message: msg("academics.model.format.maLopDaTonTaiTrong") },
  INVALID_CLASS_START_DATE: { field: "startsOn", message: CLASS_START_DATE_RANGE },
};

/** Maps class form API errors to the field they belong to (BR-U05-64, D-U04-05); other errors stay form-level. */
export function formatClassFieldError(cause: unknown): ClassFieldError | null {
  if (!(cause instanceof ApiError) || !cause.problem.code) return null;
  return CLASS_FIELD_ERRORS[cause.problem.code] ?? null;
}

/** Formats a class start date `yyyy-MM-dd` as `d/M/yyyy` without timezone shifts (BR-U05-61). */
export { formatDateOnly as formatStartDate } from "@/shared/dateTime";
