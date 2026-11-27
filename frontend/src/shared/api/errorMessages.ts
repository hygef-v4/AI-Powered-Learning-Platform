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
  AI_TOO_MANY_TASKS: "aiTooManyTasks",
  AI_TASK_NOT_CANCELLABLE: "aiNotCancellable",
  AI_REQUEST_TOO_LONG: "aiRequestTooLong",
  AI_PROMPT_TOO_LARGE: "aiPromptTooLarge",
  AI_SOURCE_UNAVAILABLE: "aiSourceUnavailable",
  AI_PROVIDER_DNS_FAILED: "aiProviderDnsFailed",
  AI_PROVIDER_CONNECT_FAILED: "aiProviderConnectFailed",
  AI_PROVIDER_TIMEOUT: "aiProviderTimeout",
  AI_PROVIDER_CONNECTION_LOST: "aiProviderConnectionLost",
  AI_PROVIDER_INTERRUPTED: "aiProviderInterrupted",
  AI_PROVIDER_RESULT_UNKNOWN: "aiProviderResultUnknown",
  AI_PROVIDER_AUTH_FAILED: "aiProviderAuthFailed",
  AI_PROVIDER_MODEL_UNAVAILABLE: "aiProviderModelUnavailable",
  AI_PROVIDER_QUOTA_EXHAUSTED: "aiProviderQuotaExhausted",
  AI_PROVIDER_RATE_LIMITED: "aiProviderRateLimited",
  AI_PROVIDER_RATE_OR_QUOTA: "aiProviderRateOrQuota",
  AI_PROVIDER_OVERLOADED: "aiProviderOverloaded",
  AI_PROVIDER_REQUEST_REJECTED: "aiProviderRequestRejected",
  AI_PROVIDER_ERROR: "aiProviderError",
  AI_IMAGES_UNSUPPORTED: "aiImagesUnsupported",
  AI_OUTPUT_LIMIT: "aiOutputLimit",
  AI_OUTPUT_INVALID: "aiOutputInvalid",
  AI_OUTPUT_UNUSABLE: "aiOutputUnusable",
  TEMPLATE_TITLE_DUPLICATE: "templateTitleDuplicate",
  SETTING_DEPENDENCY: "settingDependency",
};

/** Lý do lỗi AI mà backend lưu vào `safeError` (tiếng Việt, không kèm mã); khớp nguyên văn câu của server. */
const AI_FAILURE_KEYS: Record<string, string> = {
  "Bạn đang có 3 yêu cầu AI chưa xong; chờ hoặc hủy bớt rồi thử lại": "aiTooManyTasks",
  "Người dùng đã hủy": "aiCancelled",
  "Chờ quá 30 phút, đã hủy": "aiTimedOut",
  "Kết nối AI đã bị tắt hoặc thu hồi": "aiConnectionRevoked",
  "Kết quả AI không đúng định dạng yêu cầu": "aiInvalidResult",
  "Kết quả AI không đúng định dạng": "aiInvalidResult",
  "AI không trả kết quả": "aiNoResult",
  "AI chưa sẵn sàng": "aiNotReady",
  "AI trả nội dung không đọc được, hãy thử lại": "aiUnreadable",
  "Bài nộp đã thay đổi": "submissionChanged",
};

const NON_ASCII = /[^\x00-\x7F]/;

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
  const aiError = describeAiCode(problem.code, problem.detail);
  if (aiError) return aiError;
  if (LOCALE === "vi" && problem.detail) return problem.detail;
  const key = findMessageKey(problem);
  if (key === "fileTooLarge") {
    const limitMb = /(\d+)\s*MB/i.exec(problem.detail ?? "")?.[1];
    if (limitMb) return msg("shared.api.errorMessages.fileTooLargeMb", { value: limitMb });
  }
  return msg(`shared.api.errorMessages.${key}`);
}

function describeAiCode(code: string | undefined, detail?: string): string | undefined {
  if (!code?.startsWith("AI_") || !CODE_KEYS[code]) return undefined;
  const text = LOCALE === "vi" && detail ? detail : msg(`shared.api.errorMessages.${CODE_KEYS[code]}`);
  return `[${code}] ${text}`;
}

/**
 * Lý do lỗi của task/góp ý AI theo ngôn ngữ giao diện. Kết quả mới kèm mã (`[AI_...] chi tiết`) dịch theo mã. Kết quả
 * cũ chỉ có câu tiếng Việt: giao diện tiếng Việt giữ câu của server; ngôn ngữ khác dịch câu đã biết (KAN-232), giữ câu
 * tiếng Anh, còn câu tiếng Việt chưa biết thì dùng `fallback` nếu bên gọi truyền.
 */
export function describeAiFailure(reason: string | null | undefined, fallback?: string): string {
  const text = reason?.trim();
  if (!text) return fallback ?? "";
  const match = /^\[(AI_[A-Z_]+)\]\s+([\s\S]*)$/.exec(text);
  if (match) return describeAiCode(match[1], match[2]) ?? text;
  if (LOCALE === "vi") return text;
  const key = AI_FAILURE_KEYS[text];
  if (key) return msg(`shared.api.errorMessages.${key}`);
  return NON_ASCII.test(text) && fallback ? fallback : text;
}
