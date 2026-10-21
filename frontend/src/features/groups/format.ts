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
