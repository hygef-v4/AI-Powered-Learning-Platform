import { ApiError } from "@/shared/api/client";

const SYSTEM_BUSY = "Hệ thống tạm bận, vui lòng thử lại sau.";

const ERROR_LABELS: Record<string, string> = {
  QUIZ_NOT_FOUND: "Không tìm thấy quiz hoặc bạn không học lớp này.",
  QUIZ_ATTEMPT_NOT_FOUND: "Không tìm thấy lượt làm.",
  QUIZ_NOT_OPEN: "Quiz đã ngưng hoặc lớp không còn mở.",
  ANSWER_VERSION_CONFLICT: "Câu trả lời vừa được lưu từ nơi khác. Dữ liệu mới nhất đã được tải lại.",
  ATTEMPT_TIME_UP: "Đã hết thời gian làm bài.",
  ATTEMPT_SUBMITTED: "Lượt này đã nộp.",
  QUIZ_PRACTICE_STUDENT_ONLY: "Chỉ sinh viên làm Quiz Practice.",
  SYSTEM_BUSY,
};

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

export function formatDateTime(value: string | null): string {
  return value ? new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "—";
}

export function formatPoints(value: number | null | undefined): string {
  return value === null || value === undefined ? "—" : Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

export function formatDuration(fromIso: string, toIso: string | null): string {
  if (!toIso) return "—";
  const minutes = Math.max(0, Math.round((new Date(toIso).getTime() - new Date(fromIso).getTime()) / 60000));
  return `${minutes} phút`;
}

export function attemptsText(used: number, max: number | null): string {
  return max === null ? `${used} lượt · không giới hạn` : `${used} / ${max} lượt`;
}
