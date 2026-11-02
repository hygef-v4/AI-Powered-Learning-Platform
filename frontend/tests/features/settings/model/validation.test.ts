import { describe, expect, it } from "vitest";
import type { SettingItem } from "@/features/settings/model/types";
import {
  describeConstraints,
  formatDateTime,
  formatSettingValue,
  formatShortDateTime,
  validateSettingValue,
} from "@/features/settings/model/validation";

function item(overrides: Partial<SettingItem>): SettingItem {
  return {
    key: "k",
    group: "FILES",
    name: "n",
    description: "d",
    type: "INTEGER",
    value: 50,
    defaultValue: 50,
    constraints: { min: 1, max: 50 },
    version: 1,
    updatedBy: null,
    updatedAt: null,
    ...overrides,
  };
}

describe("validateSettingValue", () => {
  it("kiểm khoảng của số nguyên", () => {
    expect(validateSettingValue(item({}), 20)).toBeNull();
    expect(validateSettingValue(item({}), 51)).toBe("Giá trị phải từ 1 đến 50");
    expect(validateSettingValue(item({}), 2.5)).toBe("Giá trị phải là số nguyên");
  });

  it("chỉ mục không có mặc định được bỏ trống", () => {
    expect(validateSettingValue(item({}), null)).toBe("Mục này phải có giá trị");
    expect(validateSettingValue(item({ type: "TIME_OF_DAY", defaultValue: null }), null)).toBeNull();
  });

  it("kiểm giờ:phút và tập chọn nhiều", () => {
    const time = item({ type: "TIME_OF_DAY", defaultValue: null, constraints: {} });
    expect(validateSettingValue(time, "02:30")).toBeNull();
    expect(validateSettingValue(time, "24:00")).not.toBeNull();
    const set = item({ type: "ENUM_SET", value: ["PDF"], defaultValue: ["PDF"], constraints: { options: ["PDF", "DOCX"], nonEmpty: true } });
    expect(validateSettingValue(set, [])).toBe("Phải chọn ít nhất một giá trị");
    expect(validateSettingValue(set, ["EXE"])).not.toBeNull();
  });

  it("hiện giá trị dễ đọc", () => {
    expect(formatSettingValue({ type: "BOOLEAN", value: false })).toBe("Tắt");
    expect(formatSettingValue({ type: "TIME_OF_DAY", value: null })).toBe("Chưa cấu hình");
    expect(formatSettingValue({ type: "ENUM_SET", value: ["PDF", "DOCX"] })).toBe("PDF, DOCX");
    expect(formatSettingValue({ type: "INTEGER", value: 50, constraints: { unit: "MB" } })).toBe("50 MB");
    expect(formatSettingValue({ type: "ENUM_SET", value: [] })).toBe("Chưa cấu hình");
  });

  it("mô tả giới hạn hợp lệ theo kiểu", () => {
    expect(describeConstraints(item({ constraints: { min: 1, max: 50, unit: "MB" } }))).toBe("Số nguyên từ 1 đến 50 (MB)");
    const set = item({ type: "ENUM_SET", constraints: { options: ["PDF", "DOCX"], nonEmpty: true } });
    expect(describeConstraints(set)).toBe("Chọn trong: PDF, DOCX (ít nhất một)");
    expect(describeConstraints(item({ type: "BOOLEAN", constraints: {} }))).toBe("Bật hoặc tắt");
  });

  it("hiện thời điểm theo giờ Việt Nam", () => {
    expect(formatShortDateTime("2026-10-02T07:15:00Z")).toBe("02/10 14:15");
    expect(formatDateTime("2026-10-02T17:05:00Z")).toBe("03/10/2026 00:05");
  });
});
