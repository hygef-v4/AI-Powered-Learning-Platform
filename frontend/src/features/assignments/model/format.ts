import { ApiError } from "@/shared/api/client";
import type { ValidationIssue } from "@/shared/document";
import { msg } from "@/i18n/msg";

/** Ngày giờ theo giờ Việt Nam (UTC+7) và cặp đổi giá trị ô datetime-local, dùng chung từ shared/dateTime. */
export { formatDateTime, formatTime, convertFromLocalInput as fromLocalInput, convertToLocalInput as toLocalInput } from "@/shared/dateTime";

export function readProblemIssues(error: unknown): ValidationIssue[] {
  if (!(error instanceof ApiError)) return [];
  const issues = (error.problem as { issues?: ValidationIssue[] }).issues;
  return Array.isArray(issues) ? issues : [];
}

export function readProblemCode(error: unknown): string | undefined {
  return error instanceof ApiError ? error.problem.code : undefined;
}

export function describeError(error: unknown, fallback = msg("assignments.model.format.couldNotCompleteItPlease")): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 404) return msg("assignments.model.format.notFoundOrYouDo");
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
