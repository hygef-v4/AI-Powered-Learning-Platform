import type { SettingItem, SettingValue } from "./types";

// Kiểm sơ bộ phía client theo khai báo; backend vẫn kiểm lại (BR-U03-82).

export function options(item: Pick<SettingItem, "constraints">): string[] {
  const value = item.constraints?.options;
  return Array.isArray(value) ? value.map(String) : [];
}

/** Đơn vị hiển thị khai báo trong constraints.unit, ví dụ "MB". */
export function unitOf(item: Pick<SettingItem, "constraints">): string | null {
  const unit = item.constraints?.unit;
  return typeof unit === "string" && unit !== "" ? unit : null;
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

export function isEmptyValue(value: SettingValue): boolean {
  return value === null || (Array.isArray(value) && value.length === 0);
}

export function formatSettingValue(item: Pick<SettingItem, "type" | "value"> & Partial<Pick<SettingItem, "constraints">>): string {
  const { value } = item;
  if (isEmptyValue(value)) return "Chưa cấu hình";
  if (item.type === "BOOLEAN") return value ? "Bật" : "Tắt";
  if (Array.isArray(value)) return value.join(", ");
  const unit = unitOf({ constraints: item.constraints ?? null });
  return unit && item.type === "INTEGER" ? `${value} ${unit}` : String(value);
}

/** Mô tả giới hạn hợp lệ cho màn Setting Detail. */
export function describeConstraints(item: SettingItem): string {
  const unit = unitOf(item);
  switch (item.type) {
    case "INTEGER": {
      const min = item.constraints?.min;
      const max = item.constraints?.max;
      const range = min !== undefined && max !== undefined ? `Số nguyên từ ${min} đến ${max}` : "Số nguyên";
      return unit ? `${range} (${unit})` : range;
    }
    case "BOOLEAN":
      return "Bật hoặc tắt";
    case "TIME_OF_DAY":
      return "Giờ:phút theo giờ Việt Nam, ví dụ 02:30";
    case "ENUM":
      return `Một trong: ${options(item).join(", ")}`;
    case "ENUM_SET": {
      const base = `Chọn trong: ${options(item).join(", ")}`;
      return item.constraints?.nonEmpty === true ? `${base} (ít nhất một)` : base;
    }
  }
}

function dateParts(iso: string): Record<string, string> {
  const parts = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(iso));
  return Object.fromEntries(parts.map((part) => [part.type, part.value]));
}

/** "02/10 14:15" theo giờ Việt Nam. */
export function formatShortDateTime(iso: string): string {
  const p = dateParts(iso);
  return `${p.day}/${p.month} ${p.hour}:${p.minute}`;
}

/** "02/10/2026 14:15" theo giờ Việt Nam. */
export function formatDateTime(iso: string): string {
  const p = dateParts(iso);
  return `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}`;
}
