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

function parts(iso: string, withYear: boolean): Record<string, string> {
  const list = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    ...(withYear ? { year: "numeric" as const } : {}),
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(iso));
  return Object.fromEntries(list.map((part) => [part.type, part.value]));
}

/** "21/10 14:05" theo giờ Việt Nam. */
export function formatShort(iso: string): string {
  const p = parts(iso, false);
  return `${p.day}/${p.month} ${p.hour}:${p.minute}`;
}

/** "21/10/2026" theo giờ Việt Nam. */
export function formatDate(iso: string): string {
  const p = parts(iso, true);
  return `${p.day}/${p.month}/${p.year}`;
}

/** "••••3kQf": chỉ 4 ký tự cuối của khóa hoặc token. */
export function masked(hint: string | null | undefined): string {
  return hint ? `••••${hint}` : "—";
}
