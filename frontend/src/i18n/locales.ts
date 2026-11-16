/** Các ngôn ngữ của giao diện; mỗi mã có một tệp `messages/<mã>.json` cùng khóa. */
export const LOCALES = ["en", "vi", "ja"] as const;
export type Locale = (typeof LOCALES)[number];

/** Chưa chọn ngôn ngữ (hoặc cookie lạ) thì dùng tiếng Anh. */
export const DEFAULT_LOCALE: Locale = "en";
/** Cookie lưu ngôn ngữ người dùng chọn; máy chủ đọc nó khi dựng trang, trình duyệt đọc khi chạy. */
export const LOCALE_COOKIE = "locale";

/** Tên ngôn ngữ viết bằng chính ngôn ngữ đó, để ai cũng nhận ra trong menu chọn. */
export const LOCALE_NAMES: Record<Locale, string> = { en: "English", vi: "Tiếng Việt", ja: "日本語" };

/** Mã vùng cho Open Graph. */
export const OG_LOCALES: Record<Locale, string> = { en: "en_US", vi: "vi_VN", ja: "ja_JP" };

export function parseLocale(value: string | null | undefined): Locale {
  return LOCALES.includes(value as Locale) ? (value as Locale) : DEFAULT_LOCALE;
}

/** Đổi ngôn ngữ: lưu cookie một năm rồi tải lại trang để mọi chuỗi (kể cả hằng số của module) dùng ngôn ngữ mới. */
export function chooseLocale(locale: Locale) {
  document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=31536000; samesite=lax`;
  window.location.reload();
}
