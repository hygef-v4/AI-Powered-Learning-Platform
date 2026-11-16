import { cookies } from "next/headers";
import { LOCALE_COOKIE, parseLocale, type Locale } from "./locales";
import { richTranslatorFor, translatorFor } from "./msg";

/** Ngôn ngữ của lượt tải trang hiện tại, đọc từ cookie người dùng đã chọn. Chỉ dùng trong server component. */
export async function requestLocale(): Promise<Locale> {
  return parseLocale((await cookies()).get(LOCALE_COOKIE)?.value);
}

/** Hàm dịch theo ngôn ngữ của lượt tải trang: tiêu đề tab, trang chỉ dựng trên máy chủ. */
export async function serverTranslator() {
  return translatorFor(await requestLocale());
}

export async function serverMsg(key: string, values?: Record<string, string>): Promise<string> {
  return (await serverTranslator())(key, values);
}

/** Như {@link serverTranslator} nhưng cho câu có thẻ định dạng (`<b>…</b>`), trả về phần tử React. */
export async function serverRichTranslator() {
  return richTranslatorFor(await requestLocale());
}
