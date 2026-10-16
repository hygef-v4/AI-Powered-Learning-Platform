import type { SettingItem, SettingValue } from "./types";

// Kiểm sơ bộ phía client theo khai báo; backend vẫn kiểm lại (BR-U03-82).

export function options(item: SettingItem): string[] {
  const value = item.constraints?.options;
  return Array.isArray(value) ? value.map(String) : [];
}

export function canBeEmpty(item: SettingItem): boolean {
  return item.defaultValue === null;
}

export function validateSettingValue(item: SettingItem, value: SettingValue): string | null {
  if (value === null) {
    return canBeEmpty(item) ? null : "Mục này phải có giá trị";
  }
  switch (item.type) {
    case "INTEGER": {
      if (typeof value !== "number" || !Number.isInteger(value)) return "Giá trị phải là số nguyên";
      const min = Number(item.constraints?.min);
      const max = Number(item.constraints?.max);
      if ((!Number.isNaN(min) && value < min) || (!Number.isNaN(max) && value > max)) {
        return `Giá trị phải từ ${min} đến ${max}`;
      }
      return null;
    }
    case "BOOLEAN":
      return typeof value === "boolean" ? null : "Giá trị phải là bật hoặc tắt";
    case "TIME_OF_DAY":
      return typeof value === "string" && /^([01]\d|2[0-3]):[0-5]\d$/.test(value)
        ? null
        : "Giờ phải có dạng giờ:phút, ví dụ 02:30";
    case "ENUM":
      return typeof value === "string" && options(item).includes(value)
        ? null
        : "Giá trị không nằm trong danh sách cho phép";
    case "ENUM_SET": {
      if (!Array.isArray(value)) return "Giá trị phải là danh sách";
      if (value.some((v) => !options(item).includes(v))) return "Có giá trị không nằm trong danh sách cho phép";
      if (value.length === 0 && item.constraints?.nonEmpty === true) return "Phải chọn ít nhất một giá trị";
      return null;
    }
  }
}

export function formatSettingValue(item: Pick<SettingItem, "type" | "value">): string {
  const { value } = item;
  if (value === null || (Array.isArray(value) && value.length === 0)) return "Chưa cấu hình";
  if (item.type === "BOOLEAN") return value ? "Bật" : "Tắt";
  if (Array.isArray(value)) return value.join(", ");
  return String(value);
}
