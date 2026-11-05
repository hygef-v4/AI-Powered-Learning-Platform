import { ApiError } from "@/shared/api/client";

export function operationError(cause: unknown) {
  if (cause instanceof ApiError && cause.problem.status === 409) return "Dữ liệu đã thay đổi. Vui lòng tải lại trước khi tiếp tục.";
  // Nội dung server chưa kiểm chứng không được hiển thị SQL/stack trace cho người dùng.
  return cause instanceof ApiError ? "Không thể hoàn tất thao tác. Vui lòng thử lại." : cause instanceof Error ? cause.message : "Không thể hoàn tất thao tác. Vui lòng thử lại.";
}

export function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });
}
