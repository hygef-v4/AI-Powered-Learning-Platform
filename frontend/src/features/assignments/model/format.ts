import { ApiError } from "@/shared/api/client";
import type { ValidationIssue } from "@/shared/document";

const VN_OFFSET_MS = 7 * 3600 * 1000;

/** Ngày giờ theo giờ Việt Nam (UTC+7). */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "–";
  return new Date(iso).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" });
}

export function formatTime(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(iso).toLocaleTimeString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit" });
}

/** ISO → giá trị ô datetime-local theo giờ Việt Nam. */
export function toLocalInput(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(new Date(iso).getTime() + VN_OFFSET_MS).toISOString().slice(0, 16);
}

/** Giá trị ô datetime-local (giờ Việt Nam) → ISO; rỗng thì null. */
export function fromLocalInput(value: string): string | null {
  if (!value) return null;
  return new Date(`${value}:00+07:00`).toISOString();
}

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
