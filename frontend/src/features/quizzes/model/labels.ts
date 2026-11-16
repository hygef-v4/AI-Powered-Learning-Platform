import { ApiError } from "@/shared/api/client";
import type { Tone } from "@/shared/ui";
import type { Difficulty, QuestionStatus, QuestionType, QuizStatus } from "./types";
import { msg } from "@/i18n/msg";

export const TYPE_LABELS: Record<QuestionType, string> = { MCQ_SINGLE: msg("quizzes.model.labels.motDapAn"), MCQ_MULTI: msg("quizzes.model.labels.nhieuDapAn") };
export const DIFFICULTY_LABELS: Record<Difficulty, string> = { EASY: msg("quizzes.model.labels.de"), MEDIUM: msg("quizzes.model.labels.trungBinh"), HARD: msg("quizzes.model.labels.kho") };
export const STATUS_LABELS: Record<QuestionStatus, string> = { DRAFT: msg("quizzes.model.labels.nhap"), PENDING: msg("quizzes.model.labels.choDuyet"), REJECTED: msg("quizzes.model.labels.biTuChoi"), ACTIVE: msg("quizzes.model.labels.dangDung"), RETIRED: msg("quizzes.model.labels.ngung") };
export const STATUS_TONES: Record<QuestionStatus, Tone> = { DRAFT: "warning", PENDING: "info", REJECTED: "danger", ACTIVE: "success", RETIRED: "neutral" };
export const DIFFICULTY_TONES: Record<Difficulty, Tone> = { EASY: "success", MEDIUM: "info", HARD: "danger" };

const SYSTEM_BUSY = msg("quizzes.model.labels.heThongTamBanVuiLong");

const ERROR_LABELS: Record<string, string> = {
  QUESTION_NOT_FOUND: msg("quizzes.model.labels.khongTimThayCauHoiHoac"),
  SUBJECT_NOT_FOUND: msg("quizzes.model.labels.khongTimThayMonHoacBan"),
  SUBJECT_NOT_ACTIVE: msg("quizzes.model.labels.monDaLuuTruNganHang"),
  QUESTION_VERSION_CONFLICT: msg("quizzes.model.labels.cauHoiDaDuocThayDoi"),
  QUESTION_DELETE_BLOCKED: msg("quizzes.model.labels.cauDaKichHoatKhongXoa"),
  IMPORT_FILE_TYPE: msg("quizzes.model.labels.chiNhanTepXlsxHoacCsv"),
  IMPORT_FILE_TOO_LARGE: msg("quizzes.model.labels.tepToiDa5Mb"),
  IMPORT_TOO_MANY_ROWS: msg("quizzes.model.labels.toiDa500DongMoiLan"),
  QUIZ_NOT_FOUND: msg("quizzes.model.labels.khongTimThayQuizHoacBan"),
  CLASS_NOT_FOUND: msg("quizzes.model.labels.khongTimThayLopHoacBan"),
  QUIZ_VERSION_CONFLICT: msg("quizzes.model.labels.quizDaDuocThayDoiO"),
  QUIZ_NOT_EDITABLE: msg("quizzes.model.labels.quizDaPhatHanhKhongSua"),
  QUIZ_DELETE_BLOCKED: msg("quizzes.model.labels.quizDaPhatHanhKhongXoa"),
  QUIZ_VERSION_EXISTS: msg("quizzes.model.labels.quizNayDaCoMotVersion"),
  CLASS_NOT_OPEN: msg("quizzes.model.labels.lopChuaMoNenChuaPhat"),
  CLASS_ARCHIVED: msg("quizzes.model.labels.lopDaLuuTruQuizCua"),
  QUIZ_COPY_SUBJECT_MISMATCH: msg("quizzes.model.labels.chiCopyQuizGiuaCacLop"),
  QUIZ_FULL: msg("quizzes.model.labels.quizToiDa200Cau"),
  AI_DRAFT_NOT_FOUND: msg("quizzes.model.labels.khongTimThayDeXuatAi"),
  AI_DRAFT_CLOSED: msg("quizzes.model.labels.deXuatDaDuocChapNhan"),
  AI_DRAFT_NOT_READY: msg("quizzes.model.labels.aiChuaSoanXongDeXuat"),
  AI_DRAFT_INVALID: msg("quizzes.model.labels.coCauChuaHopLeHay"),
  MATERIAL_NOT_SELECTABLE: msg("quizzes.model.labels.coTaiLieuKhongChonDuoc"),
  MATERIAL_SERVICE_UNAVAILABLE: msg("quizzes.model.labels.chuaLayDuocTaiLieuNguon"),
  AI_SERVICE_UNAVAILABLE: msg("quizzes.model.labels.chuaGuiDuocYeuCauAi"),
  AI_OUTPUT_INVALID: msg("quizzes.model.labels.ketQuaAiKhongDungDinh"),
  SYSTEM_BUSY,
};

/** Lý do đề xuất lỗi: nhãn theo mã, không có thì thông điệp an toàn của U06. */
export function failureLabel(code: string | null, message: string | null): string {
  return (code && ERROR_LABELS[code]) || message || msg("quizzes.model.labels.aiChuaSoanDuocDeXuat");
}

export const QUIZ_STATUS_LABELS: Record<QuizStatus, string> = { DRAFT: msg("quizzes.model.labels.nhap"), OPEN: msg("quizzes.model.labels.dangMo"), RETIRED: msg("quizzes.model.labels.luuTru") };
export const QUIZ_STATUS_TONES: Record<QuizStatus, Tone> = { DRAFT: "warning", OPEN: "success", RETIRED: "danger" };

export function formatPoints(value: number): string {
  return Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

/** Thông điệp cho người dùng: ưu tiên nhãn theo mã, sau đó thông điệp an toàn của server. */
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
