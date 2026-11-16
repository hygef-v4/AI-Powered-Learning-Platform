import { ApiError } from "@/shared/api/client";
import type { ActivitySignal, LeavePenalty, PenaltyView, SubmitMode } from "./types";
import { msg } from "@/i18n/msg";

const SYSTEM_BUSY = msg("quizpractice.model.labels.heThongTamBanVuiLong");

const ERROR_LABELS: Record<string, string> = {
  QUIZ_NOT_FOUND: msg("quizpractice.model.labels.khongTimThayQuizHoacBan"),
  QUIZ_ATTEMPT_NOT_FOUND: msg("quizpractice.model.labels.khongTimThayLuotLam"),
  QUIZ_NOT_OPEN: msg("quizpractice.model.labels.quizDaNgungHoacLopKhong"),
  ANSWER_VERSION_CONFLICT: msg("quizpractice.model.labels.cauTraLoiVuaDuocLuu"),
  ATTEMPT_TIME_UP: msg("quizpractice.model.labels.daHetThoiGianLamBai"),
  ATTEMPT_SUBMITTED: msg("quizpractice.model.labels.luotNayDaNop"),
  QUIZ_PRACTICE_STUDENT_ONLY: msg("quizpractice.model.labels.chiSinhVienLamQuizPractice"),
  PENALTY_NOT_DECIDABLE: msg("quizpractice.model.labels.luotNayKhongCoMucTru"),
  PENALTY_POINTS_INVALID: msg("quizpractice.model.labels.diemTruPhaiLonHon0"),
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

export { formatDateTime } from "@/shared/dateTime";

export function formatPoints(value: number | null | undefined): string {
  return value === null || value === undefined ? "—" : Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

export function formatDuration(fromIso: string, toIso: string | null): string {
  if (!toIso) return "—";
  const minutes = Math.max(0, Math.round((new Date(toIso).getTime() - new Date(fromIso).getTime()) / 60000));
  return msg("quizpractice.model.labels.phut", { minutes: String(minutes) });
}

export function attemptsText(used: number, max: number | null): string {
  return max === null ? msg("quizpractice.model.labels.luotKhongGioiHan", { used: String(used) }) : msg("quizpractice.model.labels.luot", { used: String(used), max: String(max) });
}

/** Cờ tín hiệu cho giảng viên (BR-U11-32); chỉ là dấu hiệu, tín hiệu tốt thì không hiện gì. */
export function signalText(signal: ActivitySignal | null | undefined): string | null {
  return signal === "NO_SIGNAL" ? msg("quizpractice.model.labels.khongNhanDuocTinHieu")
    : signal === "SIGNAL_LOST" ? msg("quizpractice.model.labels.matTinHieuMotPhan")
      : signal === "UNAVAILABLE" ? msg("quizpractice.model.labels.khongDocDuocDuLieuLuc") : null;
}

/** Quy tắc gợi ý trừ điểm để báo sinh viên trước và trong khi làm bài (BR-U11-33): giảng viên mới là người quyết định. */
export function penaltyRuleText(rule: LeavePenalty): string {
  const free = rule.freeLeaves > 0 ? msg("quizpractice.model.labels.duocMienLanDauRoiTrang", { freeLeaves: String(rule.freeLeaves) }) : msg("quizpractice.model.labels.moiLanRoiTrang");
  const max = rule.maxPenalty === null ? "" : msg("quizpractice.model.labels.toiDaDiem", { value: String(formatPoints(rule.maxPenalty)) });
  const min = rule.minAwaySeconds > 0 ? msg("quizpractice.model.labels.chiTinhKhiRoiTuGiay", { minAwaySeconds: String(rule.minAwaySeconds) }) : "";
  return msg("quizpractice.model.labels.coTheBiTruDiemDo", { free: String(free), value: String(formatPoints(rule.pointsPerLeave)), max: String(max), min: String(min) });
}

/** Dòng rời trang và điểm trừ trên kết quả của sinh viên (BR-U11-33); không có gì để báo thì trả về null. */
export function penaltyResultText(penalty: PenaltyView): string | null {
  const leaves = msg("quizpractice.model.labels.roiTrangLan", { value: String(penalty.leaveCount ?? 0) });
  if (penalty.decision === "APPLIED") {
    return msg("quizpractice.model.labels.diemGocGiangVienTruDiem", { value: String(formatPoints(penalty.rawScore)), value2: String(leaves.toLowerCase()), value3: String(formatPoints(penalty.penaltyPoints)), decisionReason: String(penalty.decisionReason) });
  }
  if (penalty.decision === "DISMISSED") return msg("quizpractice.model.labels.giangVienQuyetDinhKhongTru", { leaves: String(leaves), decisionReason: String(penalty.decisionReason) });
  if (penalty.decisionPending) return msg("quizpractice.model.labels.chuaBiTruDiemGiangVien", { leaves: String(leaves) });
  return (penalty.leaveCount ?? 0) > 0 ? `${leaves}.` : null;
}

/** Lượt được hệ thống tự nộp thì ghi rõ lý do (L4); nộp tay thì không có nhãn. */
export function submitModeText(mode: SubmitMode): string | null {
  return mode === "AUTO_TIME_LIMIT" ? msg("quizpractice.model.labels.tuNopKhiHetGio") : mode === "AUTO_RETIRED" ? msg("quizpractice.model.labels.tuNopKhiQuizBiNgung") : null;
}
