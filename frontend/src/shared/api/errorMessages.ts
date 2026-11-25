import { LOCALE, msg } from "@/i18n/msg";

/** Lỗi theo RFC 9457 mà backend trả: `detail` luôn là tiếng Việt, `code` là mã ổn định để dịch. */
export type ProblemText = {
  status: number;
  code?: string;
  detail?: string;
};

/** Mã lỗi có câu riêng; mã khác dùng câu chung theo HTTP status (quy ước mã nguồn, mục 9). */
const CODE_KEYS: Record<string, string> = {
  FILE_TYPE_NOT_ALLOWED: "fileTypeNotAllowed",
  FILE_TOO_LARGE: "fileTooLarge",
  FILE_REF_EXPIRED: "fileRefExpired",
  FILE_REF_INVALID: "fileRefInvalid",
  FILE_UNAVAILABLE: "fileUnavailable",
  UPLOAD_FAILED: "uploadFailed",
  UPLOAD_BUSY: "systemBusy",
  STORAGE_UNAVAILABLE: "systemBusy",
  MATERIAL_YOUTUBE_INVALID: "materialYoutubeInvalid",
  MATERIAL_ARCHIVED: "materialArchived",
  MATERIAL_SOURCE_INVALID: "materialSourceInvalid",
  MATERIAL_MODULE_INVALID: "materialModuleInvalid",
  MATERIAL_FILE_INVALID: "materialFileInvalid",
  MATERIAL_VERSION_CONFLICT: "conflict",
  ANNOUNCEMENT_INVALID: "announcementInvalid",
  ANNOUNCEMENT_EMAIL_INVALID: "announcementEmailInvalid",
  ANNOUNCEMENT_STALE: "conflict",
};

const STATUS_KEYS: Record<number, string> = {
  400: "invalidRequest",
  403: "forbidden",
  404: "notFound",
  409: "conflict",
  410: "fileUnavailable",
  413: "fileTooLarge",
  415: "fileTypeNotAllowed",
  429: "tooManyRequests",
};

function findMessageKey({ status, code }: ProblemText): string {
  if (code && CODE_KEYS[code]) return CODE_KEYS[code];
  if (code?.endsWith("_NOT_FOUND")) return "notFound";
  if (STATUS_KEYS[status]) return STATUS_KEYS[status];
  return status >= 500 ? "systemBusy" : "requestFailed";
}

/**
 * Câu báo lỗi theo ngôn ngữ giao diện. Giao diện tiếng Việt giữ `detail` của server (đã là tiếng Việt, cụ thể hơn);
 * ngôn ngữ khác dịch theo `code`, không có thì theo status, không bao giờ hiện câu tiếng Việt của server.
 */
export function describeApiProblem(problem: ProblemText): string {
  if (LOCALE === "vi" && problem.detail) return problem.detail;
  const key = findMessageKey(problem);
  if (key === "fileTooLarge") {
    const limitMb = /(\d+)\s*MB/i.exec(problem.detail ?? "")?.[1];
    if (limitMb) return msg("shared.api.errorMessages.fileTooLargeMb", { value: limitMb });
  }
  return msg(`shared.api.errorMessages.${key}`);
}
