import { ApiError } from "@/shared/api/client";
import type { ValidationIssue } from "@/shared/document";

import { formatRemaining, formatTime as sharedFormatTime } from "@/shared/dateTime";
import { msg } from "@/i18n/msg";

/** Ngày giờ theo giờ Việt Nam (UTC+7), dùng chung từ shared/dateTime. */
export { formatDateTime } from "@/shared/dateTime";

/**
 * Bài cá nhân, nhóm (yêu cầu 2026-11-10): lịch sử nộp bài chỉ mở khi đã nộp và đã qua hạn nộp đúng giờ. Bài luyện tập
 * xem lịch sử lúc nào cũng được.
 */
export function canOpenHistory(detail: { gradingMode: string; closesAt: string | null; serverTime: string }, submitted: boolean): boolean {
  if (detail.gradingMode === "PRACTICE") return true;
  return submitted && !!detail.closesAt && new Date(detail.serverTime).getTime() > new Date(detail.closesAt).getTime();
}

/**
 * Lịch sử bài cá nhân (yêu cầu 2026-11-10): nộp lại là ghi đè nên chỉ có bản đúng hạn (bản nộp cuối trước hạn nộp); bài có
 * hạn nộp muộn thì thêm bản nộp muộn (bản nộp cuối trong hạn nộp muộn). Mới nhất trước.
 */
export function listHistoryVersions<T extends { status: string; late: boolean; attemptNo: number }>(attempts: T[]): T[] {
  const submitted = attempts.filter((a) => a.status === "SUBMITTED").sort((a, b) => b.attemptNo - a.attemptNo);
  const onTime = submitted.find((a) => !a.late);
  const late = submitted.find((a) => a.late);
  return [late, onTime].filter((a): a is T => !!a);
}

/** Giờ có giây: hai lần lưu hoặc nộp trong cùng một phút vẫn phân biệt được. */
export function formatTime(iso: string | null | undefined): string {
  return iso ? sharedFormatTime(iso, true) : "";
}

/** Còn lại tới hạn dạng "2 ngày 3 giờ", "15 phút"; quá hạn trả "Đã quá hạn". */
export function describeRemaining(deadline: string | null, now: number): string {
  if (!deadline) return "";
  return formatRemaining(deadline, now) ?? msg("assignmentwork.model.format.daQuaHan");
}


export function readProblemIssues(error: unknown): ValidationIssue[] {
  if (!(error instanceof ApiError)) return [];
  const issues = (error.problem as { issues?: ValidationIssue[] }).issues;
  return Array.isArray(issues) ? issues : [];
}

export function describeError(error: unknown, fallback = msg("assignmentwork.model.format.khongThucHienDuocHayThu")): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 404) return msg("assignmentwork.model.format.khongTimThayHoacBanKhong");
    return error.problem.detail ?? fallback;
  }
  return error instanceof Error && error.message ? error.message : fallback;
}

export function readErrorCode(error: unknown): string | undefined {
  return error instanceof ApiError ? error.problem.code : undefined;
}

export function requestRef(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}
