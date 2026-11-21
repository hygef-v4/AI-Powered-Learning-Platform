import { describe, expect, it } from "vitest";
import en from "@/i18n/messages/en.json";
import vi from "@/i18n/messages/vi.json";
import { DEFAULT_LOCALE, LOCALES, LOCALE_NAMES, parseLocale } from "@/i18n/locales";
import { LOCALE, msg, buildTranslator } from "@/i18n/msg";

function keys(obj: object, prefix = ""): string[] {
  return Object.entries(obj).flatMap(([key, value]) =>
    typeof value === "string" ? [`${prefix}${key}`] : keys(value as object, `${prefix}${key}.`));
}

describe("ngôn ngữ giao diện", () => {
  it("ngôn ngữ mặc định là tiếng Anh; trình duyệt dùng ngôn ngữ trong cookie (test đặt tiếng Việt)", () => {
    expect(DEFAULT_LOCALE).toBe("en");
    expect(LOCALE).toBe("vi");
    expect(msg("shared.layout.AppShell.language")).toBe("Ngôn ngữ");
  });

  it("tiếng Anh có đủ mọi khóa của tiếng Việt", () => {
    const all = keys(vi).sort();
    expect(keys(en).sort()).toEqual(all);
  });

  it("mỗi ngôn ngữ dịch theo tệp của nó, giá trị chèn vào câu giữ nguyên", () => {
    expect(buildTranslator("en")("shared.layout.AppShell.language")).toBe("Language");
    expect(buildTranslator("en")("grading.components.BatchActionDialog.gradesPublishedForSubmissions", { changed: "1234" }))
      .toBe("Grades published for 1234 submissions.");
  });

  it("mã ngôn ngữ lạ quay về tiếng Anh; menu ghi tên ngôn ngữ bằng chính nó", () => {
    expect(parseLocale("fr")).toBe("en");
    expect(parseLocale(undefined)).toBe("en");
    // Đã bỏ tiếng Nhật (2026-11-21): cookie cũ "ja" quay về tiếng Anh.
    expect(parseLocale("ja")).toBe("en");
    expect(LOCALES.map((code) => LOCALE_NAMES[code])).toEqual(["English", "Tiếng Việt"]);
  });
});
