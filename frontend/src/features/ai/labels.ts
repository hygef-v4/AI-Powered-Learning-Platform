import type { Tone } from "@/shared/ui";
import type { McpAccess, McpStatus, Provenance, UsageSource } from "./types";
import type { AiProvider } from "@/shared/ai";

export const PROVIDER_CONSOLE: Record<AiProvider, string> = {
  GEMINI: "Google AI Studio",
  OPENAI: "platform.openai.com",
  CLAUDE: "console.anthropic.com",
};

export const ACCESS_LABEL: Record<McpAccess, string> = {
  READ_ONLY: "Chỉ đọc",
  READ_WRITE: "Đọc và ghi",
};

export const MCP_STATUS: Record<McpStatus, { text: string; tone: Tone }> = {
  ACTIVE: { text: "Hiệu lực", tone: "success" },
  EXPIRED: { text: "Hết hạn", tone: "neutral" },
  REVOKED: { text: "Thu hồi", tone: "danger" },
};

export const SOURCE_LABEL: Record<UsageSource, string> = {
  SHARED: "API hệ thống",
  PERSONAL: "API cá nhân",
  MCP: "Agent MCP",
};

export const PROVENANCE_LABEL: Record<Provenance, string> = {
  PROVIDER_MEASURED: "Nhà cung cấp đo",
  ESTIMATED: "Ước tính",
  EXTERNALLY_REPORTED: "Agent tự báo",
  UNAVAILABLE: "Không có số liệu",
};

export const OUTCOME_LABEL: Record<string, { text: string; tone: Tone }> = {
  READY: { text: "Xong", tone: "success" },
  OK: { text: "Xong", tone: "success" },
  REJECTED: { text: "Bị từ chối", tone: "danger" },
  FAILED: { text: "Lỗi", tone: "danger" },
  ERROR: { text: "Lỗi", tone: "danger" },
  DENIED: { text: "Không có quyền", tone: "danger" },
  UNKNOWN: { text: "Không rõ kết quả", tone: "warning" },
  CANCELLED: { text: "Đã hủy", tone: "neutral" },
};

/** Tên tác vụ AI theo mã loại task của unit gọi; mã chưa biết thì hiện nguyên mã. */
export const TASK_LABEL: Record<string, string> = {
  ASSIGNMENT_DRAFT: "Tạo nháp bài tập",
  QUIZ_QUESTION_DRAFT: "Tạo nháp câu hỏi",
  GRADING_PROPOSAL: "Đề xuất chấm bài",
  PRACTICE_FEEDBACK: "Góp ý bài luyện tập",
  CONNECTION_TEST: "Kiểm tra kết nối",
};

export const MAX_AGENTS = 5;

/** Thông báo an toàn cho lỗi API của màn AI Connection theo mã lỗi backend. */
export function errorMessage(code: string | undefined, fallback: string): string {
  switch (code) {
    case "AI_KEY_ENCRYPTION_NOT_CONFIGURED":
      return "Hệ thống chưa bật mã hóa khóa API, hãy báo Quản trị viên.";
    case "AI_CONNECTION_VERSION_CONFLICT":
    case "AI_QUOTA_VERSION_CONFLICT":
      return "Dữ liệu vừa được thay đổi ở nơi khác, hãy tải lại.";
    case "MCP_LIMIT_REACHED":
      return "Bạn đã có 5 agent còn hiệu lực; hãy thu hồi bớt.";
    default:
      return fallback;
  }
}

/**
 * Kết quả kiểm tra khóa API rút còn 1–2 từ để đọc lướt (câu đầy đủ của máy chủ để ở tooltip): Đạt, Sai khóa, Sai model,
 * Hết hạn mức, Quá tải, Quá giờ, Mất kết nối, Bị từ chối; không nhận ra thì "Không đạt".
 */
export function shortTestResult(text: string | null | undefined): string {
  const value = text?.trim() ?? "";
  if (value === "Đạt") return "Đạt";
  if (/key api|khóa|HTTP 40[13]/i.test(value)) return "Sai khóa";
  if (/model|HTTP 404/i.test(value)) return "Sai model";
  if (/HTTP 429|giới hạn|hạn mức/i.test(value)) return "Hết hạn mức";
  if (/quá tải|HTTP 5(03|29)/i.test(value)) return "Quá tải";
  if (/kịp|timeout/i.test(value)) return "Quá giờ";
  if (/kết nối/i.test(value)) return "Mất kết nối";
  if (/từ chối|HTTP 400/i.test(value)) return "Bị từ chối";
  return "Không đạt";
}
