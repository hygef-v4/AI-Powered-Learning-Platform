import { ApiError } from "@/shared/api/client";
import type { ValidationIssue } from "@/shared/document";

/** Ngày giờ theo giờ Việt Nam (UTC+7) và cặp đổi giá trị ô datetime-local, dùng chung từ shared/dateTime. */
export { formatDateTime, formatTime, fromLocalInput, toLocalInput } from "@/shared/dateTime";

export function problemIssues(error: unknown): ValidationIssue[] {
  if (!(error instanceof ApiError)) return [];
  const issues = (error.problem as { issues?: ValidationIssue[] }).issues;
  return Array.isArray(issues) ? issues : [];
}

export function problemCode(error: unknown): string | undefined {
  return error instanceof ApiError ? error.problem.code : undefined;
}

export function errorMessage(error: unknown, fallback = "Không thực hiện được. Hãy thử lại."): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 404) return "Không tìm thấy hoặc bạn không có quyền.";
    return error.problem.detail ?? fallback;
  }
  return error instanceof Error && error.message ? error.message : fallback;
}

export function isConflict(error: unknown): boolean {
  return error instanceof ApiError && error.problem.status === 409;
}

export function requestRef(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : String(Date.now()) + Math.random();
}
