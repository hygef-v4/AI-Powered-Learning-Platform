import type { ReactNode } from "react";
import { createTranslator, type AbstractIntlMessages } from "next-intl";
import en from "./messages/en.json";
import ja from "./messages/ja.json";
import vi from "./messages/vi.json";
import { DEFAULT_LOCALE, LOCALE_COOKIE, parseLocale, type Locale } from "./locales";

/**
 * Chuỗi giao diện nằm trong `src/i18n/messages/<mã ngôn ngữ>.json`, viết theo cú pháp ICU của next-intl, cùng khóa ở mọi
 * ngôn ngữ. Khóa thiếu ở một ngôn ngữ thì dùng câu tiếng Việt.
 */
const MESSAGES = { vi, en, ja } as unknown as Record<Locale, AbstractIntlMessages>;

type Values = Record<string, string>;
type Translate = (key: string, values?: Values) => string;

/** Ngôn ngữ trên trình duyệt đọc từ cookie lúc nạp trang; trên máy chủ (và trong test) là tiếng Việt. */
function browserLocale(): Locale {
  if (typeof document === "undefined") return DEFAULT_LOCALE;
  const match = new RegExp(`(?:^|; )${LOCALE_COOKIE}=([^;]*)`).exec(document.cookie);
  return parseLocale(match?.[1]);
}

/** Ngôn ngữ đang dùng trên trình duyệt; đổi ngôn ngữ là tải lại trang ({@link chooseLocale}). */
export const LOCALE: Locale = browserLocale();

function hasMessage(messages: AbstractIntlMessages, key: string): boolean {
  let current: unknown = messages;
  for (const part of key.split(".")) {
    if (!current || typeof current !== "object" || !(part in current)) return false;
    current = (current as Record<string, unknown>)[part];
  }
  return typeof current === "string";
}

const translators = new Map<Locale, Translate>();

/** Hàm dịch của một ngôn ngữ (dùng lại giữa các lần gọi). */
export function translatorFor(locale: Locale): Translate {
  const cached = translators.get(locale);
  if (cached) return cached;
  const own = createTranslator({ locale, messages: MESSAGES[locale] });
  const fallback = locale === DEFAULT_LOCALE ? null : translatorFor(DEFAULT_LOCALE);
  const translate: Translate = (key, values) => fallback && !hasMessage(MESSAGES[locale], key)
    ? fallback(key, values)
    : own(key as never, values as never);
  translators.set(locale, translate);
  return translate;
}

type RichTags = Record<string, (chunks: ReactNode) => ReactNode>;

/**
 * Câu có thẻ định dạng trong tệp ngôn ngữ (ví dụ `<b>…</b>`), trả về phần tử React; `tags` nói mỗi thẻ vẽ thành gì.
 * Khóa thiếu ở một ngôn ngữ thì dùng câu tiếng Việt.
 */
export function richTranslatorFor(locale: Locale) {
  const own = createTranslator({ locale, messages: MESSAGES[locale] });
  const fallback = createTranslator({ locale: DEFAULT_LOCALE, messages: MESSAGES[DEFAULT_LOCALE] });
  return (key: string, tags: RichTags): ReactNode => (hasMessage(MESSAGES[locale], key) ? own : fallback)
    .rich(key as never, tags as never);
}

/**
 * Chuỗi giao diện theo khóa, ví dụ `msg("grading.components.BatchActionDialog.congBoDiem")`. Giá trị chèn vào câu đều là
 * chữ (`{ count: String(count) }`) để không bị định dạng lại như số.
 */
export function msg(key: string, values?: Values): string {
  return translatorFor(LOCALE)(key, values);
}
