import { ApiError } from "@/shared/api/client";
import type { ActivitySignal, LeavePenalty, PenaltyView, SubmitMode } from "./types";

const SYSTEM_BUSY = "Hệ thống tạm bận, vui lòng thử lại sau.";

const ERROR_LABELS: Record<string, string> = {
  QUIZ_NOT_FOUND: "Không tìm thấy quiz hoặc bạn không học lớp này.",
  QUIZ_ATTEMPT_NOT_FOUND: "Không tìm thấy lượt làm.",
  QUIZ_NOT_OPEN: "Quiz đã ngưng hoặc lớp không còn mở.",
  ANSWER_VERSION_CONFLICT: "Câu trả lời vừa được lưu từ nơi khác. Dữ liệu mới nhất đã được tải lại.",
  ATTEMPT_TIME_UP: "Đã hết thời gian làm bài.",
  ATTEMPT_SUBMITTED: "Lượt này đã nộp.",
  QUIZ_PRACTICE_STUDENT_ONLY: "Chỉ sinh viên làm Quiz Practice.",
  PENALTY_NOT_DECIDABLE: "Lượt này không có mức trừ gợi ý.",
  PENALTY_POINTS_INVALID: "Điểm trừ phải lớn hơn 0, không quá điểm gốc, tối đa 2 chữ số thập phân.",
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

/** Cờ tín hiệu cho giảng viên (BR-U11-32); chỉ là dấu hiệu, tín hiệu tốt thì không hiện gì. */
export function signalText(signal: ActivitySignal | null | undefined): string | null {
  return signal === "NO_SIGNAL" ? "Không nhận được tín hiệu"
    : signal === "SIGNAL_LOST" ? "Mất tín hiệu một phần"
      : signal === "UNAVAILABLE" ? "Không đọc được dữ liệu lúc nộp" : null;
}

/** Quy tắc gợi ý trừ điểm để báo sinh viên trước và trong khi làm bài (BR-U11-33): giảng viên mới là người quyết định. */
export function penaltyRuleText(rule: LeavePenalty): string {
  const free = rule.freeLeaves > 0 ? `Được miễn ${rule.freeLeaves} lần đầu rời trang; sau đó mỗi lần` : "Mỗi lần rời trang";
  const max = rule.maxPenalty === null ? "" : `, tối đa ${formatPoints(rule.maxPenalty)} điểm`;
  const min = rule.minAwaySeconds > 0 ? ` Chỉ tính khi rời từ ${rule.minAwaySeconds} giây trở lên.` : "";
  return `${free} có thể bị trừ ${formatPoints(rule.pointsPerLeave)} điểm${max}, do giảng viên quyết định khi xem lượt làm.${min}`;
}

/** Dòng rời trang và điểm trừ trên kết quả của sinh viên (BR-U11-33); không có gì để báo thì trả về null. */
export function penaltyResultText(penalty: PenaltyView): string | null {
  const leaves = `Rời trang ${penalty.leaveCount ?? 0} lần`;
  if (penalty.decision === "APPLIED") {
    return `Điểm gốc ${formatPoints(penalty.rawScore)} · ${leaves.toLowerCase()} · giảng viên trừ ${formatPoints(penalty.penaltyPoints)} điểm: ${penalty.decisionReason}`;
  }
  if (penalty.decision === "DISMISSED") return `${leaves} · giảng viên quyết định không trừ điểm: ${penalty.decisionReason}`;
  if (penalty.decisionPending) return `${leaves} · chưa bị trừ điểm, giảng viên sẽ xem xét.`;
  return (penalty.leaveCount ?? 0) > 0 ? `${leaves}.` : null;
}

/** Lượt được hệ thống tự nộp thì ghi rõ lý do (L4); nộp tay thì không có nhãn. */
export function submitModeText(mode: SubmitMode): string | null {
  return mode === "AUTO_TIME_LIMIT" ? "Tự nộp khi hết giờ" : mode === "AUTO_RETIRED" ? "Tự nộp khi quiz bị ngưng" : null;
}
