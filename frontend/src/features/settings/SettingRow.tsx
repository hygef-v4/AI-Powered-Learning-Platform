"use client";

import { useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Button } from "@/shared/ui/Button";
import { ChipToggleGroup, Select, Switch, TextInput } from "@/shared/ui/inputs";
import { SettingLine } from "@/shared/ui/SettingsCard";
import { updateSetting } from "./api";
import type { SettingItem, SettingValue } from "./types";
import { options, unitOf, validateSettingValue } from "./validation";

type Props = {
  item: SettingItem;
  disabled?: boolean;
  onSaved: (item: SettingItem) => void;
  /** 409: người khác vừa sửa, tải lại danh sách. */
  onConflict: () => void;
};

/**
 * Một mục cài đặt sửa ngay trên Setting List (UC 14, 15). Bật/tắt lưu ngay; mục cần nhập giá trị hiện nút Lưu khi đã
 * sửa. Giới hạn hợp lệ chỉ hiện khi nhập sai.
 */
export function SettingRow({ item, disabled = false, onSaved, onConflict }: Props) {
  const id = useId();
  const [draft, setDraft] = useState<SettingValue>(item.value);
  const [saving, setSaving] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const invalid = validateSettingValue(item, draft);
  const dirty = JSON.stringify(draft) !== JSON.stringify(item.value);
  const error = (dirty ? invalid : null) ?? serverError;
  const locked = disabled || saving;

  async function save(value: SettingValue) {
    if (validateSettingValue(item, value)) return;
    setSaving(true);
    setServerError(null);
    try {
      onSaved(await updateSetting(item.key, value, item.version));
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      if (problem?.status === 409) {
        setServerError("Vừa có người sửa mục này, đã tải lại giá trị mới.");
        onConflict();
      } else {
        setServerError(problem?.status === 400 ? (problem.detail ?? "Giá trị không hợp lệ") : "Không lưu được, thử lại sau");
      }
    } finally {
      setSaving(false);
    }
  }

  const change = (value: SettingValue) => {
    setDraft(value);
    setServerError(null);
  };

  let control;
  switch (item.type) {
    case "BOOLEAN":
      control = (
        <Switch
          ariaLabel={item.name}
          checked={item.value === true}
          disabled={locked}
          label=""
          onChange={(next) => void save(next)}
        />
      );
      break;
    case "INTEGER":
      control = (
        <TextInput
          id={id}
          type="number"
          inputMode="numeric"
          invalid={error !== null}
          suffix={unitOf(item)}
          wrapperClassName="w-[160px]"
          value={typeof draft === "number" ? draft : ""}
          disabled={locked}
          onChange={(e) => change(e.target.value === "" ? null : Number(e.target.value))}
        />
      );
      break;
    case "TIME_OF_DAY":
      control = (
        <TextInput
          id={id}
          type="time"
          invalid={error !== null}
          wrapperClassName="w-[160px]"
          value={typeof draft === "string" ? draft : ""}
          disabled={locked}
          onChange={(e) => change(e.target.value === "" ? null : e.target.value)}
        />
      );
      break;
    case "ENUM":
      control = (
        <Select
          id={id}
          invalid={error !== null}
          wrapperClassName="w-[200px]"
          value={typeof draft === "string" ? draft : ""}
          disabled={locked}
          onChange={(e) => change(e.target.value === "" ? null : e.target.value)}
        >
          {draft === null && <option value="">Chưa chọn</option>}
          {options(item).map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </Select>
      );
      break;
    case "ENUM_SET":
      control = (
        <ChipToggleGroup
          ariaLabel={item.name}
          options={options(item)}
          value={Array.isArray(draft) ? draft : []}
          disabled={locked}
          onChange={change}
        />
      );
      break;
  }

  return (
    <SettingLine
      label={item.name}
      htmlFor={item.type === "BOOLEAN" || item.type === "ENUM_SET" ? undefined : id}
      info={item.description || undefined}
      muted={disabled}
      control={control}
      error={error}
      action={
        item.type !== "BOOLEAN" &&
        dirty && (
          <Button size="sm" variant="primary" disabled={locked || invalid !== null} onClick={() => void save(draft)}>
            Lưu
          </Button>
        )
      }
    />
  );
}
