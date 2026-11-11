import { describe, expect, it } from "vitest";
import { formatDate, formatDateTime, formatRelativeTime, formatTimeUntil } from "@/shared/dateTime";

const NOW = new Date("2026-11-11T07:00:00Z"); // 14:00 ngày 11/11 giờ Việt Nam

describe("dateTime", () => {
  it("formats in Vietnam time regardless of the machine zone", () => {
    expect(formatDateTime("2026-11-10T17:30:00Z")).toBe("00:30 11/11/2026");
    expect(formatDate("2026-11-10T17:30:00Z")).toBe("11/11/2026");
    expect(formatDateTime(null)).toBe("—");
    expect(formatDate("không phải ngày")).toBe("—");
  });

  it("writes past times relative to now and falls back to the full date after a week", () => {
    expect(formatRelativeTime("2026-11-11T06:59:40Z", NOW)).toBe("Vừa xong");
    expect(formatRelativeTime("2026-11-11T06:55:00Z", NOW)).toBe("5 phút trước");
    expect(formatRelativeTime("2026-11-11T03:00:00Z", NOW)).toBe("4 giờ trước");
    expect(formatRelativeTime("2026-11-10T07:32:00Z", NOW)).toBe("Hôm qua 14:32");
    expect(formatRelativeTime("2026-11-07T07:00:00Z", NOW)).toBe("4 ngày trước");
    expect(formatRelativeTime("2026-11-01T07:00:00Z", NOW)).toBe("14:00 01/11/2026");
  });

  it("counts down to a deadline and marks the last 24 hours as urgent", () => {
    expect(formatTimeUntil("2026-11-11T07:45:00Z", NOW)).toEqual({ text: "còn 45 phút", urgent: true });
    expect(formatTimeUntil("2026-11-11T12:00:00Z", NOW)).toEqual({ text: "còn 5 giờ", urgent: true });
    expect(formatTimeUntil("2026-11-13T08:00:00Z", NOW)).toEqual({ text: "còn 2 ngày", urgent: false });
    expect(formatTimeUntil("2026-11-11T06:00:00Z", NOW)).toBeNull();
    expect(formatTimeUntil(null, NOW)).toBeNull();
  });
});
