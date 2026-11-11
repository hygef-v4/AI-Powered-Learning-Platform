/** Ngày giờ hiển thị luôn theo giờ Việt Nam, không theo múi giờ của máy người dùng. */
export const APP_TIME_ZONE = "Asia/Ho_Chi_Minh";

const MINUTE_MS = 60_000;
const HOUR_MS = 60 * MINUTE_MS;
const DAY_MS = 24 * HOUR_MS;

const dateTimeFormat = new Intl.DateTimeFormat("vi-VN", {
  timeZone: APP_TIME_ZONE, hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric",
});
const dateFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: APP_TIME_ZONE, day: "2-digit", month: "2-digit", year: "numeric" });
const timeFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: APP_TIME_ZONE, hour: "2-digit", minute: "2-digit" });
const dayKeyFormat = new Intl.DateTimeFormat("en-CA", { timeZone: APP_TIME_ZONE, year: "numeric", month: "2-digit", day: "2-digit" });

function parseDate(value: string | Date | null | undefined): Date | null {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Ngày giờ dạng "14:32 10/11/2026"; thiếu hoặc sai giá trị thì "—". */
export function formatDateTime(value: string | Date | null | undefined): string {
  const date = parseDate(value);
  return date ? dateTimeFormat.format(date) : "—";
}

/** Ngày dạng "10/11/2026"; thiếu hoặc sai giá trị thì "—". */
export function formatDate(value: string | Date | null | undefined): string {
  const date = parseDate(value);
  return date ? dateFormat.format(date) : "—";
}

/** Số ngày lịch (theo giờ Việt Nam) từ `earlier` tới `later`. */
function calendarDaysBetween(earlier: Date, later: Date): number {
  const key = (date: Date) => Date.parse(`${dayKeyFormat.format(date)}T00:00:00Z`);
  return Math.round((key(later) - key(earlier)) / DAY_MS);
}

/**
 * Thời điểm đã qua, viết tương đối: "Vừa xong", "5 phút trước", "3 giờ trước", "Hôm qua 14:32", "4 ngày trước"; quá 7 ngày
 * thì hiện ngày giờ đầy đủ.
 */
export function formatRelativeTime(value: string | Date | null | undefined, now: Date = new Date()): string {
  const date = parseDate(value);
  if (!date) return "—";
  const elapsed = now.getTime() - date.getTime();
  if (elapsed < MINUTE_MS) return "Vừa xong";
  if (elapsed < HOUR_MS) return `${Math.floor(elapsed / MINUTE_MS)} phút trước`;
  const days = calendarDaysBetween(date, now);
  if (days === 0) return `${Math.floor(elapsed / HOUR_MS)} giờ trước`;
  if (days === 1) return `Hôm qua ${timeFormat.format(date)}`;
  if (days < 7) return `${days} ngày trước`;
  return formatDateTime(date);
}

/**
 * Thời gian còn lại tới hạn: "còn 45 phút", "còn 5 giờ", "còn 2 ngày"; `urgent` khi còn dưới 24 giờ. Đã qua hạn hoặc
 * không có hạn thì `null`.
 */
export function formatTimeUntil(
  value: string | Date | null | undefined,
  now: Date = new Date(),
): { text: string; urgent: boolean } | null {
  const date = parseDate(value);
  if (!date) return null;
  const left = date.getTime() - now.getTime();
  if (left <= 0) return null;
  if (left < HOUR_MS) return { text: `còn ${Math.max(1, Math.floor(left / MINUTE_MS))} phút`, urgent: true };
  if (left < DAY_MS) return { text: `còn ${Math.floor(left / HOUR_MS)} giờ`, urgent: true };
  return { text: `còn ${Math.floor(left / DAY_MS)} ngày`, urgent: false };
}
