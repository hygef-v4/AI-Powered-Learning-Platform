import type { SettingItem, SettingValue } from "./types";
import { msg } from "@/i18n/msg";

// Kiểm sơ bộ phía client theo khai báo; backend vẫn kiểm lại (BR-U03-82).

export function listOptions(item: Pick<SettingItem, "constraints">): string[] {
  const value = item.constraints?.options;
  return Array.isArray(value) ? value.map(String) : [];
}

/** Đơn vị hiển thị khai báo trong constraints.unit, ví dụ "MB". */
export function readUnit(item: Pick<SettingItem, "constraints">): string | null {
  const unit = item.constraints?.unit;
  return typeof unit === "string" && unit !== "" ? unit : null;
}

export function canBeEmpty(item: SettingItem): boolean {
  return item.defaultValue === null;
}

export function validateSettingValue(item: SettingItem, value: SettingValue): string | null {
  if (value === null) {
    return canBeEmpty(item) ? null : msg("settings.model.validation.thisFieldMustHaveA");
  }
  switch (item.type) {
    case "INTEGER": {
      if (typeof value !== "number" || !Number.isInteger(value)) return msg("settings.model.validation.theValueMustBeAn");
      const min = Number(item.constraints?.min);
      const max = Number(item.constraints?.max);
      if ((!Number.isNaN(min) && value < min) || (!Number.isNaN(max) && value > max)) {
        return msg("settings.model.validation.theValueMustBeFrom", { min: String(min), max: String(max) });
      }
      return null;
    }
    case "BOOLEAN":
      return typeof value === "boolean" ? null : msg("settings.model.validation.theValueMustBeOn");
    case "TIME_OF_DAY":
      return typeof value === "string" && /^([01]\d|2[0-3]):[0-5]\d$/.test(value)
        ? null
        : msg("settings.model.validation.theTimeMustBeIn");
    case "ENUM":
      return typeof value === "string" && listOptions(item).includes(value)
        ? null
        : msg("settings.model.validation.theValueIsNotIn");
    case "ENUM_SET": {
      if (!Array.isArray(value)) return msg("settings.model.validation.theValueMustBeA");
      if (value.some((v) => !listOptions(item).includes(v))) return msg("settings.model.validation.someValuesAreNotIn");
      if (value.length === 0 && item.constraints?.nonEmpty === true) return msg("settings.model.validation.chooseAtLeastOneValue");
      return null;
    }
  }
}

export function isEmptyValue(value: SettingValue): boolean {
  return value === null || (Array.isArray(value) && value.length === 0);
}

export function formatSettingValue(item: Pick<SettingItem, "type" | "value"> & Partial<Pick<SettingItem, "constraints">>): string {
  const { value } = item;
  if (isEmptyValue(value)) return msg("settings.model.validation.notConfigured");
  if (item.type === "BOOLEAN") return value ? msg("settings.model.validation.on") : msg("settings.model.validation.off");
  if (Array.isArray(value)) return value.join(", ");
  const unit = readUnit({ constraints: item.constraints ?? null });
  return unit && item.type === "INTEGER" ? `${value} ${unit}` : String(value);
}

/** Mô tả giới hạn hợp lệ cho màn Setting Detail. */
export function describeConstraints(item: SettingItem): string {
  const unit = readUnit(item);
  switch (item.type) {
    case "INTEGER": {
      const min = item.constraints?.min;
      const max = item.constraints?.max;
      const range = min !== undefined && max !== undefined ? msg("settings.model.validation.integerFromTo", { min: String(min), max: String(max) }) : msg("settings.model.validation.integer");
      return unit ? `${range} (${unit})` : range;
    }
    case "BOOLEAN":
      return msg("settings.model.validation.onOrOff");
    case "TIME_OF_DAY":
      return msg("settings.model.validation.hourMinuteInVietnamTime");
    case "ENUM":
      return msg("settings.model.validation.oneOf", { value: String(listOptions(item).join(", ")) });
    case "ENUM_SET": {
      const base = msg("settings.model.validation.chooseFrom", { value: String(listOptions(item).join(", ")) });
      return item.constraints?.nonEmpty === true ? msg("settings.model.validation.atLeastOne", { base: String(base) }) : base;
    }
  }
}

/** Ngày giờ hiển thị dùng chung từ shared/dateTime (giờ Việt Nam). */
export { formatDateTime, formatShort as formatShortDateTime } from "@/shared/dateTime";
