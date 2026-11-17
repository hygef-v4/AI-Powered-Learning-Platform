import type { AiProvider } from "./types";

export const PROVIDERS: AiProvider[] = ["GEMINI", "OPENAI", "CLAUDE"];

export const PROVIDER_LABEL: Record<AiProvider, string> = {
  GEMINI: "Gemini",
  OPENAI: "OpenAI",
  CLAUDE: "Claude",
};

/** "12.500" theo cách viết số Việt Nam. */
export function formatTokens(value: number): string {
  return new Intl.NumberFormat("vi-VN").format(value);
}

/** Token có thể không có số liệu: hiện "—", không bao giờ đổi thành 0 (BR-U06-40). */
export function formatMaybeTokens(value: number | null | undefined): string {
  return value === null || value === undefined ? "—" : formatTokens(value);
}

/** Ngày giờ hiển thị dùng chung từ shared/dateTime (giờ Việt Nam). */
export { formatDate, formatDateTime, formatShort } from "@/shared/dateTime";

/** "••••3kQf": chỉ 4 ký tự cuối của khóa hoặc token. */
export function maskHint(hint: string | null | undefined): string {
  return hint ? `••••${hint}` : "—";
}
