import { ApiError } from "@/shared/api/client";
import type { ValidationIssue } from "@/shared/document";
import { msg } from "@/i18n/msg";
import type { AiDraft } from "./types";

export function requiresAiDraftReview(draft: AiDraft | null): boolean {
  return draft?.provenance.templateReviewRequired === true
    || (draft?.proposal?.validation?.issues.some((issue) => issue.code === "TEMPLATE_CONFLICT"
      || issue.code === "MISSING_REQUIRED_REFERENCE" || issue.code === "QUESTION_COUNT_CONFLICT") ?? false);
}

export function parseQuestionCount(input: string): number | undefined | null {
  if (!input.trim()) return undefined;
  const count = Number(input);
  return Number.isInteger(count) && count >= 1 && count <= 10 ? count : null;
}

export function parseQuestionPoints(input: string): number[] | null {
  if (!input.trim()) return [];
  const entries = input.trim().split(/\s+/);
  if (entries.length > 10 || entries.some((entry) => !/^\d+([.,]\d{1,2})?$/.test(entry))) return null;
  const points = entries.map((entry) => Number(entry.replace(",", ".")));
  if (points.some((point) => point <= 0 || point > 1000)) return null;
  return points.reduce((total, point) => total + Math.round(point * 100), 0) <= 100_000 ? points : null;
}

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
