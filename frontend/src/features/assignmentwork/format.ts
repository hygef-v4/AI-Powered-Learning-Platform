import { ApiError } from "@/shared/api/client";
import type { ValidationIssue } from "@/shared/document";

/** Ngày giờ theo giờ Việt Nam (UTC+7). */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "–";
  return new Date(iso).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" });
}

export function formatTime(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(iso).toLocaleTimeString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

/** Còn lại tới hạn dạng "2 ngày 3 giờ", "15 phút"; quá hạn trả "Đã quá hạn". */
export function remaining(deadline: string | null, now: number): string {
  if (!deadline) return "";
  const ms = new Date(deadline).getTime() - now;
  if (ms <= 0) return "Đã quá hạn";
  const minutes = Math.floor(ms / 60000);
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  const mins = minutes % 60;
  if (days > 0) return `${days} ngày ${hours} giờ`;
  if (hours > 0) return `${hours} giờ ${mins} phút`;
  if (mins > 0) return `${mins} phút`;
  return `${Math.ceil(ms / 1000)} giây`;
}

export function problemIssues(error: unknown): ValidationIssue[] {
  if (!(error instanceof ApiError)) return [];
  const issues = (error.problem as { issues?: ValidationIssue[] }).issues;
  return Array.isArray(issues) ? issues : [];
}

export function errorMessage(error: unknown, fallback = "Không thực hiện được. Hãy thử lại."): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 404) return "Không tìm thấy hoặc bạn không có quyền.";
    return error.problem.detail ?? fallback;
  }
  return error instanceof Error && error.message ? error.message : fallback;
}

export function errorCode(error: unknown): string | undefined {
  return error instanceof ApiError ? error.problem.code : undefined;
}

export function requestRef(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}
