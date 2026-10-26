import { ApiError } from "@/shared/api/client";
import type { Tone } from "@/shared/ui";
import type { Difficulty, QuestionStatus, QuestionType, QuizStatus } from "./types";

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
  QUIZ_NOT_FOUND: "Không tìm thấy quiz hoặc bạn không có quyền với quiz này.",
  CLASS_NOT_FOUND: "Không tìm thấy lớp hoặc bạn không dạy lớp này.",
  QUIZ_VERSION_CONFLICT: "Quiz đã được thay đổi ở phiên khác. Dữ liệu mới nhất đã được tải lại.",
  QUIZ_NOT_EDITABLE: "Quiz đã phát hành không sửa được; hãy tạo version mới.",
  QUIZ_DELETE_BLOCKED: "Quiz đã phát hành không xóa được; hãy ngưng quiz.",
  QUIZ_VERSION_EXISTS: "Quiz này đã có một version mới đang soạn.",
  CLASS_NOT_OPEN: "Lớp chưa mở nên chưa phát hành được quiz của lớp.",
  CLASS_ARCHIVED: "Lớp đã lưu trữ, quiz của lớp chỉ xem.",
  QUIZ_COPY_SUBJECT_MISMATCH: "Chỉ copy quiz giữa các lớp cùng môn.",
  QUIZ_FULL: "Quiz tối đa 200 câu.",
  AI_DRAFT_NOT_FOUND: "Không tìm thấy đề xuất AI.",
  AI_DRAFT_CLOSED: "Đề xuất đã được chấp nhận hoặc bỏ ở phiên khác.",
  AI_DRAFT_NOT_READY: "AI chưa soạn xong đề xuất.",
  AI_DRAFT_INVALID: "Có câu chưa hợp lệ, hãy sửa trước khi chấp nhận.",
  MATERIAL_NOT_SELECTABLE: "Có tài liệu không chọn được hoặc chưa trích được chữ.",
  MATERIAL_SERVICE_UNAVAILABLE: "Chưa lấy được tài liệu tham khảo, vui lòng thử lại.",
  AI_SERVICE_UNAVAILABLE: "Chưa gửi được yêu cầu AI, vui lòng thử lại.",
  AI_OUTPUT_INVALID: "Kết quả AI không đúng định dạng câu hỏi; hãy gửi lại yêu cầu.",
  SYSTEM_BUSY,
};

/** Lý do đề xuất lỗi: nhãn theo mã, không có thì thông điệp an toàn của U06. */
export function failureLabel(code: string | null, message: string | null): string {
  return (code && ERROR_LABELS[code]) || message || "AI chưa soạn được đề xuất.";
}

export const QUIZ_STATUS_LABELS: Record<QuizStatus, string> = { DRAFT: "Nháp", REVIEWED: "Đã duyệt", OPEN: "Đang mở", RETIRED: "Ngưng" };
export const QUIZ_STATUS_TONES: Record<QuizStatus, Tone> = { DRAFT: "warning", REVIEWED: "info", OPEN: "success", RETIRED: "neutral" };

export function formatPoints(value: number): string {
  return Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

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
