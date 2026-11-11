import { LOAD_FAILED_MESSAGE } from "@/shared/api/messages";
import { ApiError } from "@/shared/api/client";

export const ADJUSTMENT_REASON_MESSAGE = "Nhập lý do khi cộng hoặc trừ điểm.";

export function formatErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.problem.code === "ADJUSTMENT_REASON_REQUIRED") {
    return ADJUSTMENT_REASON_MESSAGE;
  }
  if (error instanceof ApiError && error.problem.status === 409) {
    switch (error.problem.code) {
      case "GRADE_LOCKED":
        return "Điểm đã chốt không thể chỉnh sửa. Tải lại để xem bản chấm đã khóa; nội dung bạn đang sửa vẫn được giữ.";
      case "SUBMISSION_CHANGED":
      case "NOT_LATEST_ATTEMPT":
        return "Bài nộp đã thay đổi. Mở bài nộp hiện tại để kiểm tra trước khi chấm tiếp; nội dung bạn đang sửa vẫn được giữ.";
      case "RUBRIC_CHANGED":
        return "Rubric đã thay đổi. Tải lại và kiểm tra rubric trước khi chấm tiếp; nội dung bạn đang sửa vẫn được giữ.";
      case "GRADE_VERSION_CONFLICT":
        return "Dữ liệu đã thay đổi. Tải lại để kiểm tra phiên bản mới; nội dung bạn đang sửa vẫn được giữ.";
    }
    if (error.problem.detail) return error.problem.detail;
    if (error.message && error.message !== "Conflict" && error.message !== "HTTP 409") {
      return error.message;
    }
    return "Dữ liệu đã thay đổi. Tải lại để kiểm tra phiên bản mới; nội dung bạn đang sửa vẫn được giữ.";
  }
  return error instanceof Error
    ? error.message
    : LOAD_FAILED_MESSAGE;
}
