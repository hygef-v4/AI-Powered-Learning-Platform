import { describe, expect, it } from "vitest";
import en from "@/i18n/messages/en.json";
import ja from "@/i18n/messages/ja.json";
import vi from "@/i18n/messages/vi.json";
import { DEFAULT_LOCALE, LOCALES, LOCALE_NAMES, parseLocale } from "@/i18n/locales";
import { LOCALE, msg, translatorFor } from "@/i18n/msg";

function keys(obj: object, prefix = ""): string[] {
  return Object.entries(obj).flatMap(([key, value]) =>
    typeof value === "string" ? [`${prefix}${key}`] : keys(value as object, `${prefix}${key}.`));
}

describe("ngôn ngữ giao diện", () => {
  it("mặc định là tiếng Việt khi chưa chọn ngôn ngữ", () => {
    expect(LOCALE).toBe(DEFAULT_LOCALE);
    expect(msg("shared.layout.AppShell.ngonNgu")).toBe("Ngôn ngữ");
  });

  it("tiếng Anh và tiếng Nhật có đủ mọi khóa của tiếng Việt", () => {
    const all = keys(vi).sort();
    expect(keys(en).sort()).toEqual(all);
    expect(keys(ja).sort()).toEqual(all);
  });

  it("mỗi ngôn ngữ dịch theo tệp của nó, giá trị chèn vào câu giữ nguyên", () => {
    expect(translatorFor("en")("shared.layout.AppShell.ngonNgu")).toBe("Language");
    expect(translatorFor("ja")("shared.layout.AppShell.ngonNgu")).toBe("言語");
    expect(translatorFor("en")("grading.components.BatchActionDialog.daCongBoDiemChoBai", { changed: "1234" }))
      .toBe("Grades published for 1234 submissions.");
  });

  it("mã ngôn ngữ lạ quay về tiếng Việt; menu ghi tên ngôn ngữ bằng chính nó", () => {
    expect(parseLocale("fr")).toBe("vi");
    expect(parseLocale(undefined)).toBe("vi");
    expect(parseLocale("ja")).toBe("ja");
    expect(LOCALES.map((code) => LOCALE_NAMES[code])).toEqual(["Tiếng Việt", "English", "日本語"]);
  });
});
