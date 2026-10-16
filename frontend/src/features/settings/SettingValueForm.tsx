"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { updateSetting } from "./api";
import type { SettingItem, SettingValue } from "./types";
import { canBeEmpty, options, validateSettingValue } from "./validation";

type Props = {
  item: SettingItem;
  onSaved: (item: SettingItem) => void;
  onReload: () => void;
};

/** Ô nhập theo kiểu (UC 15); 400 hiện lý do, 409 báo dữ liệu đã đổi, lỗi không xóa nội dung đang nhập. */
export function SettingValueForm({ item, onSaved, onReload }: Props) {
  const [draft, setDraft] = useState<SettingValue>(item.value);
  const [error, setError] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const [saving, setSaving] = useState(false);

  async function save(value: SettingValue) {
    const invalid = validateSettingValue(item, value);
    if (invalid) {
      setError(invalid);
      return;
    }
    setSaving(true);
    setError(null);
    setConflict(false);
    try {
      onSaved(await updateSetting(item.key, value, item.version));
    } catch (e) {
      if (e instanceof ApiError && e.problem.status === 409) {
        setConflict(true);
      } else if (e instanceof ApiError && e.problem.status === 400) {
        setError(e.problem.detail ?? "Giá trị không hợp lệ");
      } else {
        setError("Không lưu được, thử lại sau");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form
      noValidate
      className="mt-4 space-y-3"
      onSubmit={(event) => {
        event.preventDefault();
        void save(draft);
      }}
    >
      <ValueInput item={item} value={draft} onChange={setDraft} />
      {error && (
        <p role="alert" className="text-sm text-red-700">
          {error}
        </p>
      )}
      {conflict && (
        <p role="alert" className="text-sm text-amber-700">
          Cài đặt đã được người khác sửa.{" "}
          <button type="button" className="underline" onClick={onReload}>
            Tải lại để xem giá trị mới
          </button>
        </p>
      )}
      <div className="flex gap-2">
        <button type="submit" disabled={saving} className="rounded bg-blue-700 px-3 py-1 text-white disabled:opacity-50">
          {saving ? "Đang lưu..." : "Lưu"}
        </button>
        {canBeEmpty(item) && (
          <button type="button" disabled={saving} className="rounded border px-3 py-1" onClick={() => void save(null)}>
            Bỏ trống
          </button>
        )}
      </div>
    </form>
  );
}

function ValueInput({
  item,
  value,
  onChange,
}: {
  item: SettingItem;
  value: SettingValue;
  onChange: (value: SettingValue) => void;
}) {
  const label = `Giá trị của ${item.name}`;
  switch (item.type) {
    case "INTEGER":
      return (
        <input
          aria-label={label}
          type="number"
          className="rounded border px-2 py-1"
          value={typeof value === "number" ? value : ""}
          min={Number(item.constraints?.min)}
          max={Number(item.constraints?.max)}
          onChange={(e) => onChange(e.target.value === "" ? null : Number(e.target.value))}
        />
      );
    case "BOOLEAN":
      return (
        <label className="flex items-center gap-2">
          <input aria-label={label} type="checkbox" checked={value === true} onChange={(e) => onChange(e.target.checked)} />
          Bật
        </label>
      );
    case "TIME_OF_DAY":
      return (
        <input
          aria-label={label}
          type="time"
          className="rounded border px-2 py-1"
          value={typeof value === "string" ? value : ""}
          onChange={(e) => onChange(e.target.value === "" ? null : e.target.value)}
        />
      );
    case "ENUM":
      return (
        <select
          aria-label={label}
          className="rounded border px-2 py-1"
          value={typeof value === "string" ? value : ""}
          onChange={(e) => onChange(e.target.value)}
        >
          {options(item).map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      );
    case "ENUM_SET": {
      const chosen = Array.isArray(value) ? value : [];
      return (
        <fieldset aria-label={label} className="flex flex-wrap gap-3">
          {options(item).map((option) => (
            <label key={option} className="flex items-center gap-1">
              <input
                type="checkbox"
                checked={chosen.includes(option)}
                onChange={(e) =>
                  onChange(e.target.checked ? [...chosen, option] : chosen.filter((c) => c !== option))
                }
              />
              {option}
            </label>
          ))}
        </fieldset>
      );
    }
  }
}
