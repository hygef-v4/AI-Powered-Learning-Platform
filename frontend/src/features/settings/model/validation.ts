import type { SettingItem, SettingValue } from "./types";
import { msg } from "@/i18n/msg";

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
    return canBeEmpty(item) ? null : msg("settings.model.validation.mucNayPhaiCoGiaTri");
  }
  switch (item.type) {
    case "INTEGER": {
      if (typeof value !== "number" || !Number.isInteger(value)) return msg("settings.model.validation.giaTriPhaiLaSoNguyen");
      const min = Number(item.constraints?.min);
      const max = Number(item.constraints?.max);
      if ((!Number.isNaN(min) && value < min) || (!Number.isNaN(max) && value > max)) {
        return msg("settings.model.validation.giaTriPhaiTuDen", { min: String(min), max: String(max) });
      }
      return null;
    }
    case "BOOLEAN":
      return typeof value === "boolean" ? null : msg("settings.model.validation.giaTriPhaiLaBatHoac");
    case "TIME_OF_DAY":
      return typeof value === "string" && /^([01]\d|2[0-3]):[0-5]\d$/.test(value)
        ? null
        : msg("settings.model.validation.gioPhaiCoDangGioPhut");
    case "ENUM":
      return typeof value === "string" && options(item).includes(value)
        ? null
        : msg("settings.model.validation.giaTriKhongNamTrongDanh");
    case "ENUM_SET": {
      if (!Array.isArray(value)) return msg("settings.model.validation.giaTriPhaiLaDanhSach");
      if (value.some((v) => !options(item).includes(v))) return msg("settings.model.validation.coGiaTriKhongNamTrong");
      if (value.length === 0 && item.constraints?.nonEmpty === true) return msg("settings.model.validation.phaiChonItNhatMotGia");
      return null;
    }
  }
}

export function isEmptyValue(value: SettingValue): boolean {
  return value === null || (Array.isArray(value) && value.length === 0);
}

export function formatSettingValue(item: Pick<SettingItem, "type" | "value"> & Partial<Pick<SettingItem, "constraints">>): string {
  const { value } = item;
  if (isEmptyValue(value)) return msg("settings.model.validation.chuaCauHinh");
  if (item.type === "BOOLEAN") return value ? msg("settings.model.validation.bat") : msg("settings.model.validation.tat");
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
      const range = min !== undefined && max !== undefined ? msg("settings.model.validation.soNguyenTuDen", { min: String(min), max: String(max) }) : msg("settings.model.validation.soNguyen");
      return unit ? `${range} (${unit})` : range;
    }
    case "BOOLEAN":
      return msg("settings.model.validation.batHoacTat");
    case "TIME_OF_DAY":
      return msg("settings.model.validation.gioPhutTheoGioVietNam");
    case "ENUM":
      return msg("settings.model.validation.motTrong", { value: String(options(item).join(", ")) });
    case "ENUM_SET": {
      const base = msg("settings.model.validation.chonTrong", { value: String(options(item).join(", ")) });
      return item.constraints?.nonEmpty === true ? msg("settings.model.validation.itNhatMot", { base: String(base) }) : base;
    }
  }
}

/** Ngày giờ hiển thị dùng chung từ shared/dateTime (giờ Việt Nam). */
export { formatDateTime, formatShort as formatShortDateTime } from "@/shared/dateTime";
