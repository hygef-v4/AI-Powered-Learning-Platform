import { ApiError } from "@/shared/api/client";
import type { Tone } from "@/shared/ui";
import type { Difficulty, QuestionStatus, QuestionType } from "./types";

export const TYPE_LABELS: Record<QuestionType, string> = { MCQ_SINGLE: "Một đáp án", MCQ_MULTI: "Nhiều đáp án" };
export const DIFFICULTY_LABELS: Record<Difficulty, string> = { EASY: "Dễ", MEDIUM: "Trung bình", HARD: "Khó" };
export const STATUS_LABELS: Record<QuestionStatus, string> = { DRAFT: "Nháp", ACTIVE: "Đang dùng", RETIRED: "Ngưng" };
export const STATUS_TONES: Record<QuestionStatus, Tone> = { DRAFT: "warning", ACTIVE: "success", RETIRED: "neutral" };
export const DIFFICULTY_TONES: Record<Difficulty, Tone> = { EASY: "success", MEDIUM: "info", HARD: "danger" };

const SYSTEM_BUSY = "Hệ thống tạm bận, vui lòng thử lại sau.";

const ERROR_LABELS: Record<string, string> = {
  QUESTION_NOT_FOUND: "Không tìm thấy câu hỏi hoặc bạn không quản lý môn này.",
  SUBJECT_NOT_FOUND: "Không tìm thấy môn hoặc bạn không quản lý môn này.",
  SUBJECT_NOT_ACTIVE: "Môn đã lưu trữ, ngân hàng câu hỏi chỉ xem.",
  QUESTION_VERSION_CONFLICT: "Câu hỏi đã được thay đổi ở phiên khác. Dữ liệu mới nhất đã được tải lại.",
  QUESTION_DELETE_BLOCKED: "Câu đã kích hoạt không xóa được; hãy ngưng dùng câu này.",
  IMPORT_FILE_TYPE: "Chỉ nhận tệp .xlsx hoặc .csv UTF-8.",
  IMPORT_FILE_TOO_LARGE: "Tệp tối đa 5 MB.",
  IMPORT_TOO_MANY_ROWS: "Tối đa 500 dòng mỗi lần nhập.",
  SYSTEM_BUSY,
};

/** Thông điệp cho người dùng: ưu tiên nhãn theo mã, sau đó thông điệp an toàn của server. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const code = error.problem.code;
    const label = code ? ERROR_LABELS[code] : undefined;
    if (label) return label;
    if (error.problem.status >= 500) return SYSTEM_BUSY;
    return error.message;
  }
  return SYSTEM_BUSY;
}

export function errorCode(error: unknown): string | undefined {
  return error instanceof ApiError ? error.problem.code : undefined;
}

export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" });
}
