import { LOAD_FAILED_MESSAGE } from "@/shared/api/messages";
import { ApiError } from "@/shared/api/client";
import { msg } from "@/i18n/msg";

export const ADJUSTMENT_REASON_MESSAGE = msg("grading.model.format.nhapLyDoKhiCongHoac");

export function formatErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.problem.code === "ADJUSTMENT_REASON_REQUIRED") {
    return ADJUSTMENT_REASON_MESSAGE;
  }
  if (error instanceof ApiError && error.problem.status === 409) {
    switch (error.problem.code) {
      case "GRADING_CLASS_READ_ONLY":
        return msg("grading.model.format.lopDaLuuTruHoacChua");
      case "GRADE_LOCKED":
        return msg("grading.model.format.diemDaChotKhongTheChinh");
      case "SUBMISSION_CHANGED":
      case "NOT_LATEST_ATTEMPT":
        return msg("grading.model.format.baiNopDaThayDoiMo");
      case "RUBRIC_CHANGED":
        return msg("grading.model.format.rubricDaThayDoiTaiLai");
      case "GRADE_VERSION_CONFLICT":
        return msg("grading.model.format.duLieuDaThayDoiTai");
    }
    if (error.problem.detail) return error.problem.detail;
    if (error.message && error.message !== "Conflict" && error.message !== "HTTP 409") {
      return error.message;
    }
    return msg("grading.model.format.duLieuDaThayDoiTai");
  }
  return error instanceof Error
    ? error.message
    : LOAD_FAILED_MESSAGE;
}
