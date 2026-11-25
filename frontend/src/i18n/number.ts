import type { Locale } from "./locales";
import { LOCALE } from "./msg";

/** Cách viết số theo ngôn ngữ giao diện: tiếng Anh "3.5", "12,500"; tiếng Việt "3,5", "12.500". */
const NUMBER_LOCALES: Record<Locale, string> = { en: "en-US", vi: "vi-VN" };

export const NUMBER_LOCALE = NUMBER_LOCALES[LOCALE];

export function formatNumber(value: number, options?: Intl.NumberFormatOptions): string {
  return new Intl.NumberFormat(NUMBER_LOCALE, options).format(value);
}
