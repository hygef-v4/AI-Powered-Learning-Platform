import type { Tone } from "@/shared/ui";
import type { McpAccess, McpStatus, Provenance, UsageSource } from "./types";
import type { AiProvider } from "@/shared/ai";
import { msg } from "@/i18n/msg";

export const PROVIDER_CONSOLE: Record<AiProvider, string> = {
  GEMINI: "Google AI Studio",
  OPENAI: "platform.openai.com",
  CLAUDE: "console.anthropic.com",
};

/** Mô hình chọn sẵn khi thêm khóa; không có trong danh sách thì lấy mô hình đầu tiên. */
const PROVIDER_DEFAULT_MODEL: Partial<Record<AiProvider, string>> = {
  GEMINI: "gemini-3.1-flash-lite",
};

export function defaultModel(provider: AiProvider, models: string[]): string {
  const preferred = PROVIDER_DEFAULT_MODEL[provider];
  return preferred && models.includes(preferred) ? preferred : (models[0] ?? "");
}

export const ACCESS_LABEL: Record<McpAccess, string> = {
  READ_ONLY: msg("ai.model.labels.chiDoc"),
  READ_WRITE: msg("ai.model.labels.docVaGhi"),
};

export const MCP_STATUS: Record<McpStatus, { text: string; tone: Tone }> = {
  ACTIVE: { text: msg("ai.model.labels.hieuLuc"), tone: "success" },
  EXPIRED: { text: msg("ai.model.labels.hetHan"), tone: "neutral" },
  REVOKED: { text: msg("ai.model.labels.thuHoi"), tone: "danger" },
};

export const SOURCE_LABEL: Record<UsageSource, string> = {
  SHARED: msg("ai.model.labels.apiHeThong"),
  PERSONAL: msg("ai.model.labels.apiCaNhan"),
  MCP: "Agent MCP",
};

export const PROVENANCE_LABEL: Record<Provenance, string> = {
  PROVIDER_MEASURED: msg("ai.model.labels.nhaCungCapDo"),
  ESTIMATED: msg("ai.model.labels.uocTinh"),
  EXTERNALLY_REPORTED: msg("ai.model.labels.agentTuBao"),
  UNAVAILABLE: msg("ai.model.labels.khongCoSoLieu"),
};

export const OUTCOME_LABEL: Record<string, { text: string; tone: Tone }> = {
  READY: { text: "Xong", tone: "success" },
  OK: { text: "Xong", tone: "success" },
  REJECTED: { text: msg("ai.model.labels.biTuChoi"), tone: "danger" },
  FAILED: { text: msg("ai.model.labels.loi"), tone: "danger" },
  ERROR: { text: msg("ai.model.labels.loi"), tone: "danger" },
  DENIED: { text: msg("ai.model.labels.khongCoQuyen"), tone: "danger" },
  UNKNOWN: { text: msg("ai.model.labels.khongRoKetQua"), tone: "warning" },
  CANCELLED: { text: msg("ai.model.labels.daHuy"), tone: "neutral" },
};

/** Tên tác vụ AI theo mã loại task của unit gọi; mã chưa biết thì hiện nguyên mã. */
export const TASK_LABEL: Record<string, string> = {
  ASSIGNMENT_DRAFT: msg("ai.model.labels.taoNhapBaiTap"),
  QUIZ_QUESTION_DRAFT: msg("ai.model.labels.taoNhapCauHoi"),
  GRADING_PROPOSAL: msg("ai.model.labels.deXuatChamBai"),
  PRACTICE_FEEDBACK: msg("ai.model.labels.gopYBaiLuyenTap"),
  CONNECTION_TEST: msg("ai.model.labels.kiemTraKetNoi"),
};

export const MAX_AGENTS = 5;

/** Thông báo an toàn cho lỗi API của màn AI Connection theo mã lỗi backend. */
export function describeErrorCode(code: string | undefined, fallback: string): string {
  switch (code) {
    case "AI_KEY_ENCRYPTION_NOT_CONFIGURED":
      return msg("ai.model.labels.heThongChuaBatMaHoa");
    case "AI_CONNECTION_VERSION_CONFLICT":
    case "AI_QUOTA_VERSION_CONFLICT":
      return msg("ai.model.labels.duLieuVuaDuocThayDoi");
    case "MCP_LIMIT_REACHED":
      return msg("ai.model.labels.banDaCo5AgentCon");
    default:
      return fallback;
  }
}

/**
 * Kết quả kiểm tra khóa API rút gọn để đọc lướt; câu đầy đủ của máy chủ ở tooltip.
 */
export function shortenTestResult(text: string | null | undefined): string {
  const value = text?.trim() ?? "";
  if (value === msg("ai.model.labels.dat")) return msg("ai.model.labels.dat");
  if (/key api|khóa|HTTP 40[13]/i.test(value)) return msg("ai.model.labels.saiKhoa");
  if (/model|HTTP 404/i.test(value)) return "Sai model";
  if (/giới hạn token/i.test(value)) return msg("ai.model.labels.gioiHanToken");
  if (/tần suất.*hoặc.*hạn mức/i.test(value)) return msg("ai.model.labels.tanSuatHanMuc");
  if (/tần suất/i.test(value)) return msg("ai.model.labels.gioiHanTanSuat");
  if (/hạn mức|số dư/i.test(value)) return msg("ai.model.labels.hetHanMuc");
  if (/HTTP 429/i.test(value)) return msg("ai.model.labels.tanSuatHanMuc");
  if (/quá tải|HTTP 5(03|29)/i.test(value)) return msg("ai.model.labels.quaTai");
  if (/kịp|timeout/i.test(value)) return msg("ai.model.labels.quaGio");
  if (/kết nối/i.test(value)) return msg("ai.model.labels.matKetNoi");
  if (/từ chối|HTTP 400/i.test(value)) return msg("ai.model.labels.biTuChoi");
  return msg("ai.model.labels.khongDat");
}
