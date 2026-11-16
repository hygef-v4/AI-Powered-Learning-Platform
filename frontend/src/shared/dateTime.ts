import { msg } from "@/i18n/msg";

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

const timeSecondsFormat = new Intl.DateTimeFormat("vi-VN", {
  timeZone: APP_TIME_ZONE, hour: "2-digit", minute: "2-digit", second: "2-digit",
});
const shortFormat = new Intl.DateTimeFormat("vi-VN", {
  timeZone: APP_TIME_ZONE, day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23",
});

/** Giờ phút "14:32" (thêm giây khi `withSeconds`); thiếu hoặc sai giá trị thì "—". */
export function formatTime(value: string | Date | null | undefined, withSeconds = false): string {
  const date = parseDate(value);
  return date ? (withSeconds ? timeSecondsFormat : timeFormat).format(date) : "—";
}

/** Ngày tháng gọn kèm giờ "21/10 14:05" theo giờ Việt Nam; thiếu hoặc sai giá trị thì "—". */
export function formatShort(value: string | Date | null | undefined): string {
  const date = parseDate(value);
  if (!date) return "—";
  const p = Object.fromEntries(shortFormat.formatToParts(date).map((part) => [part.type, part.value]));
  return `${p.day}/${p.month} ${p.hour}:${p.minute}`;
}

/** Ngày giờ đủ giây "14/10/2026 17:55:12" theo giờ Việt Nam, cho nhật ký cần phân biệt từng giây. */
export function formatFull(value: string | Date | null | undefined): string {
  const date = parseDate(value);
  return date ? `${dateFormat.format(date)} ${timeSecondsFormat.format(date)}` : "—";
}

/** Chuỗi ngày thuần `yyyy-MM-dd` (không giờ, không múi giờ) thành "d/M/yyyy"; sai dạng thì "—". */
export function formatDateOnly(value: string | null | undefined): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? "");
  return match ? `${Number(match[3])}/${Number(match[2])}/${match[1]}` : "—";
}

const VN_OFFSET_MS = 7 * HOUR_MS;

/** ISO → giá trị ô `datetime-local` theo giờ Việt Nam. */
export function convertToLocalInput(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(new Date(iso).getTime() + VN_OFFSET_MS).toISOString().slice(0, 16);
}

/** Giá trị ô `datetime-local` (giờ Việt Nam) → ISO; rỗng thì `null`. */
export function convertFromLocalInput(value: string): string | null {
  if (!value) return null;
  return new Date(`${value}:00+07:00`).toISOString();
}

/**
 * Thời gian còn lại tới `deadline` dạng đếm lùi: "2 ngày 3 giờ", "3 giờ 15 phút", "45 phút", "30 giây"; đã qua hạn
 * hoặc không có hạn thì `null`.
 */
export function formatRemaining(deadline: string | null | undefined, now: number): string | null {
  if (!deadline) return null;
  const ms = Date.parse(deadline) - now;
  if (Number.isNaN(ms) || ms <= 0) return null;
  const minutes = Math.floor(ms / MINUTE_MS);
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  if (days > 0) return msg("shared.dateTime.ngayGio", { days: String(days), hours: String(hours) });
  if (hours > 0) return msg("shared.dateTime.gioPhut", { hours: String(hours), value: String(minutes % 60) });
  if (minutes > 0) return msg("shared.dateTime.phut", { minutes: String(minutes) });
  return msg("shared.dateTime.giay", { value: String(Math.ceil(ms / 1000)) });
}

/** Số ngày lịch (theo giờ Việt Nam) từ `earlier` tới `later`. */
function countCalendarDaysBetween(earlier: Date, later: Date): number {
  const toDayKey = (date: Date) => Date.parse(`${dayKeyFormat.format(date)}T00:00:00Z`);
  return Math.round((toDayKey(later) - toDayKey(earlier)) / DAY_MS);
}

/**
 * Thời điểm đã qua, viết tương đối: "Vừa xong", "5 phút trước", "3 giờ trước", "Hôm qua 14:32", "4 ngày trước"; quá 7 ngày
 * thì hiện ngày giờ đầy đủ.
 */
export function formatRelativeTime(value: string | Date | null | undefined, now: Date = new Date()): string {
  const date = parseDate(value);
  if (!date) return "—";
  const elapsed = now.getTime() - date.getTime();
  if (elapsed < MINUTE_MS) return msg("shared.dateTime.vuaXong");
  if (elapsed < HOUR_MS) return msg("shared.dateTime.phutTruoc", { value: String(Math.floor(elapsed / MINUTE_MS)) });
  const days = countCalendarDaysBetween(date, now);
  if (days === 0) return msg("shared.dateTime.gioTruoc", { value: String(Math.floor(elapsed / HOUR_MS)) });
  if (days === 1) return msg("shared.dateTime.homQua", { value: String(timeFormat.format(date)) });
  if (days < 7) return msg("shared.dateTime.ngayTruoc", { days: String(days) });
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
  if (left < HOUR_MS) return { text: msg("shared.dateTime.conPhut", { value: String(Math.max(1, Math.floor(left / MINUTE_MS))) }), urgent: true };
  if (left < DAY_MS) return { text: msg("shared.dateTime.conGio", { value: String(Math.floor(left / HOUR_MS)) }), urgent: true };
  return { text: msg("shared.dateTime.conNgay", { value: String(Math.floor(left / DAY_MS)) }), urgent: false };
}
