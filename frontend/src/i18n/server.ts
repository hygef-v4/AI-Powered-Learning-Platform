import { cookies, headers } from "next/headers";
import { DEFAULT_LOCALE, LANDING_LOCALE_HEADER, LOCALE_COOKIE, parseLocale, type Locale } from "./locales";
import { buildRichTranslator, buildTranslator } from "./msg";

/**
 * Ngôn ngữ của lượt tải trang hiện tại: cookie người dùng đã chọn. Chưa có cookie thì theo ngôn ngữ mặc định của trang mà
 * proxy ghi vào header (trang chủ dùng tiếng Việt cho người xem lần đầu và máy tìm kiếm), không có thì tiếng Anh.
 * Chỉ dùng trong server component.
 */
export async function requestLocale(): Promise<Locale> {
  const chosen = (await cookies()).get(LOCALE_COOKIE)?.value;
  if (chosen) return parseLocale(chosen);
  return parseLocale((await headers()).get(LANDING_LOCALE_HEADER) ?? DEFAULT_LOCALE);
}

/**
 * Có phải ngôn ngữ trên trình duyệt (đọc từ cookie, mặc định tiếng Anh) khác ngôn ngữ máy chủ dựng trang không. Khi khác,
 * client component phải chờ nạp xong mới vẽ để không lệch chữ. Trang chủ không có cookie thì không cần chờ: mọi chữ của nó
 * đi qua props từ máy chủ.
 */
export async function clientLocaleDiffers(): Promise<boolean> {
  const chosen = (await cookies()).get(LOCALE_COOKIE)?.value;
  return chosen !== undefined && parseLocale(chosen) !== DEFAULT_LOCALE;
}

/** Hàm dịch theo ngôn ngữ của lượt tải trang: tiêu đề tab, trang chỉ dựng trên máy chủ. */
export async function loadServerTranslator() {
  return buildTranslator(await requestLocale());
}

export async function translateOnServer(key: string, values?: Record<string, string>): Promise<string> {
  return (await loadServerTranslator())(key, values);
}

/** Như {@link loadServerTranslator} nhưng cho câu có thẻ định dạng (`<b>…</b>`), trả về phần tử React. */
export async function loadServerRichTranslator() {
  return buildRichTranslator(await requestLocale());
}
