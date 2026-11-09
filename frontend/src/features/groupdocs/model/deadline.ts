import { formatDateTime } from "@/features/groups/model/format";
import type { WorkspaceView } from "./types";

/** Bắt đầu nhắc hạn trên trang tài liệu nhóm khi còn 30 phút. */
export const DEADLINE_WARNING_MS = 30 * 60_000;

export type DeadlineNotice = { tone: "warning" | "danger"; title: string; details: string[] };

function formatClock(iso: string): string {
  return formatDateTime(iso).slice(-5);
}

function countMinutes(ms: number): number {
  return Math.max(1, Math.ceil(ms / 60_000));
}

/** "vừa xong", "5 phút trước", "2 giờ trước" so với `now`. */
export function formatAgo(iso: string, now: number): string {
  const elapsed = now - Date.parse(iso);
  if (elapsed < 60_000) return "vừa xong";
  if (elapsed < 60 * 60_000) return `${Math.floor(elapsed / 60_000)} phút trước`;
  return `${Math.floor(elapsed / (60 * 60_000))} giờ trước`;
}

/**
 * Nhắc hạn khi còn ≤ 30 phút tới hạn nộp, hoặc trong khoảng nộp trễ: còn bao lâu, mục nào chưa Xong sẽ ra sao khi hệ
 * thống tự nộp (BR-U12-40, 43). Không nhắc khi tài liệu đã đóng hoặc bài không có hạn.
 */
export function describeDeadline(
  view: Pick<WorkspaceView, "closesAt" | "lateUntil" | "window" | "status" | "sections" | "lastSubmission">,
  now: number,
): DeadlineNotice | null {
  if (view.status !== "IN_PROGRESS" || view.window === "CLOSED" || !view.closesAt) return null;
  // Bản xem có thể cũ hơn giờ hiện tại: quá hạn mà còn khoảng nộp trễ thì coi như đang trễ; hết cả khoảng nộp trễ thì
  // không nhắc nữa, hệ thống sắp tự nộp và trang sẽ khóa.
  const closesAt = Date.parse(view.closesAt);
  const late = view.window === "LATE" || now >= closesAt;
  const cutoffIso = late ? view.lateUntil : view.closesAt;
  if (!cutoffIso) return null;
  const remaining = Date.parse(cutoffIso) - now;
  if (remaining <= 0 || (!late && remaining > DEADLINE_WARNING_MS)) return null;

  const title = late
    ? `Đã quá hạn ${formatClock(view.closesAt)}, nộp bây giờ bị tính trễ. Còn ${countMinutes(remaining)} phút tới khi đóng nhận bài (${formatClock(cutoffIso)}).`
    : `Còn ${countMinutes(remaining)} phút tới hạn nộp (${formatClock(view.closesAt)}).`;
  const open = view.sections.filter((s) => s.status === "OPEN").length;
  const claimed = view.sections.filter((s) => s.status === "CLAIMED").length;
  const details: string[] = [];
  if (open > 0) details.push(`${open} mục chưa ai làm, sẽ không vào bản nộp.`);
  if (claimed > 0) {
    details.push(`${claimed} mục đang soạn: hãy bấm Xong. Khi đóng nhận bài, hệ thống tự chốt các mục này bằng bản nháp đã lưu.`);
  }
  if (open === 0 && claimed === 0) details.push("Mọi mục đã Xong.");
  if (!view.lastSubmission) details.push("Nhóm chưa nộp bản nào; trưởng nhóm nên nộp trước hạn.");
  return { tone: late || remaining <= 5 * 60_000 ? "danger" : "warning", title, details };
}
