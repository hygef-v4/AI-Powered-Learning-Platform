/** Các ngôn ngữ của giao diện; mỗi mã có một tệp `messages/<mã>.json` cùng khóa. */
export const LOCALES = ["en", "vi"] as const;
export type Locale = (typeof LOCALES)[number];

/** Chưa chọn ngôn ngữ (hoặc cookie lạ) thì dùng tiếng Anh. */
export const DEFAULT_LOCALE: Locale = "en";
/** Cookie lưu ngôn ngữ người dùng chọn; máy chủ đọc nó khi dựng trang, trình duyệt đọc khi chạy. */
export const LOCALE_COOKIE = "locale";

/**
 * Trang chủ công khai mặc định tiếng Việt khi chưa có cookie: người dùng chính và từ khóa tìm kiếm của dự án là tiếng Việt,
 * còn máy tìm kiếm không gửi cookie ngôn ngữ. Proxy ghi giá trị này vào header {@link LANDING_LOCALE_HEADER}.
 */
export const LANDING_DEFAULT_LOCALE: Locale = "vi";
/** Header nội bộ proxy đặt cho trang chủ; proxy luôn ghi đè giá trị trình duyệt gửi lên. */
export const LANDING_LOCALE_HEADER = "x-landing-locale";

/** Tên ngôn ngữ viết bằng chính ngôn ngữ đó, để ai cũng nhận ra trong menu chọn. */
export const LOCALE_NAMES: Record<Locale, string> = { en: "English", vi: "Tiếng Việt" };

/** Mã vùng cho Open Graph. */
export const OG_LOCALES: Record<Locale, string> = { en: "en_US", vi: "vi_VN" };

export function parseLocale(value: string | null | undefined): Locale {
  return LOCALES.includes(value as Locale) ? (value as Locale) : DEFAULT_LOCALE;
}

/** Đổi ngôn ngữ: lưu cookie một năm rồi tải lại trang để mọi chuỗi (kể cả hằng số của module) dùng ngôn ngữ mới. */
export function chooseLocale(locale: Locale) {
  document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=31536000; samesite=lax`;
  window.location.reload();
}
