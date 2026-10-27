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
