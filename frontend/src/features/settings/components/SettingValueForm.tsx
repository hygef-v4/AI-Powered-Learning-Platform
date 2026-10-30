"use client";

import { useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert } from "@/shared/ui/Alert";
import { Button } from "@/shared/ui/Button";
import { ChipToggleGroup, Field, Select, Switch, TextInput } from "@/shared/ui/inputs";
import { updateSetting } from "../api";
import type { SettingItem, SettingValue } from "../model/types";
import { canBeEmpty, options, unitOf, validateSettingValue } from "../model/validation";

type Props = {
  item: SettingItem;
  onSaved: (item: SettingItem) => void;
  onReload: () => void;
};

const LABEL = "Giá trị mới";

/**
 * Ô nhập theo kiểu (UC 15). Kiểm ngay khi sửa và khóa nút Lưu khi giá trị sai; 400 hiện lý do,
 * 409 báo người khác vừa sửa; lỗi không xóa nội dung đang nhập.
 */
export function SettingValueForm({ item, onSaved, onReload }: Props) {
  const [draft, setDraft] = useState<SettingValue>(item.value);
  const [touched, setTouched] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const [saving, setSaving] = useState(false);

  const invalid = validateSettingValue(item, draft);
  const changed = JSON.stringify(draft) !== JSON.stringify(item.value);
  const error = (touched ? invalid : null) ?? serverError;

  function change(value: SettingValue) {
    setDraft(value);
    setTouched(true);
    setServerError(null);
  }

  async function save(value: SettingValue) {
    if (validateSettingValue(item, value)) return;
    setSaving(true);
    setServerError(null);
    setConflict(false);
    try {
      onSaved(await updateSetting(item.key, value, item.version));
    } catch (e) {
      if (e instanceof ApiError && e.problem.status === 409) {
        setConflict(true);
      } else if (e instanceof ApiError && e.problem.status === 400) {
        setServerError(e.problem.detail ?? "Giá trị không hợp lệ");
      } else {
        setServerError("Không lưu được, thử lại sau");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form
      noValidate
      className="flex flex-col gap-3.5"
      onSubmit={(event) => {
        event.preventDefault();
        void save(draft);
      }}
    >
      {conflict && (
        <Alert
          tone="warning"
          role="alert"
          title="Người khác vừa sửa"
          action={
            <Button size="sm" icon="refresh" onClick={onReload}>
              Tải lại
            </Button>
          }
        >
          Cài đặt đã được người khác sửa. Tải lại để xem giá trị mới trước khi lưu.
        </Alert>
      )}
      <ValueField item={item} value={draft} error={error} onChange={change} disabled={saving} />
      <div className="flex items-center justify-end gap-2">
        {canBeEmpty(item) && item.value !== null && (
          <Button disabled={saving} onClick={() => void save(null)} className="mr-auto">
            Bỏ trống
          </Button>
        )}
        <Button
          disabled={saving || !changed}
          onClick={() => {
            setDraft(item.value);
            setTouched(false);
            setServerError(null);
          }}
        >
          Hủy
        </Button>
        <Button type="submit" variant="primary" icon="save" disabled={saving || !changed || invalid !== null}>
          {saving ? "Đang lưu…" : "Lưu"}
        </Button>
      </div>
    </form>
  );
}

type FieldProps = {
  item: SettingItem;
  value: SettingValue;
  error: string | null;
  onChange: (value: SettingValue) => void;
  disabled: boolean;
};

function ValueField({ item, value, error, onChange, disabled }: FieldProps) {
  const id = useId();
  const required = !canBeEmpty(item);
  switch (item.type) {
    case "INTEGER":
      return (
        <Field label={LABEL} htmlFor={id} required={required} error={error}>
          <TextInput
            id={id}
            type="number"
            inputMode="numeric"
            invalid={error !== null}
            suffix={unitOf(item)}
            wrapperClassName="w-[220px]"
            value={typeof value === "number" ? value : ""}
            min={Number(item.constraints?.min)}
            max={Number(item.constraints?.max)}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value === "" ? null : Number(e.target.value))}
          />
        </Field>
      );
    case "BOOLEAN":
      return (
        <Field label={LABEL} required={required} error={error}>
          <Switch
            ariaLabel={LABEL}
            checked={value === true}
            disabled={disabled}
            label={value === true ? "Bật" : "Tắt"}
            onChange={onChange}
          />
        </Field>
      );
    case "TIME_OF_DAY":
      return (
        <Field label={LABEL} htmlFor={id} required={required} error={error} hint="Giờ Việt Nam (Asia/Ho_Chi_Minh)">
          <TextInput
            id={id}
            type="time"
            icon="schedule"
            invalid={error !== null}
            wrapperClassName="w-[220px]"
            value={typeof value === "string" ? value : ""}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value === "" ? null : e.target.value)}
          />
        </Field>
      );
    case "ENUM":
      return (
        <Field label={LABEL} htmlFor={id} required={required} error={error}>
          <Select
            id={id}
            invalid={error !== null}
            wrapperClassName="w-[280px]"
            value={typeof value === "string" ? value : ""}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value === "" ? null : e.target.value)}
          >
            {value === null && <option value="">Chưa chọn</option>}
            {options(item).map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </Select>
        </Field>
      );
    case "ENUM_SET":
      return (
        <Field label={LABEL} required={required} error={error}>
          <ChipToggleGroup
            ariaLabel={LABEL}
            options={options(item)}
            value={Array.isArray(value) ? value : []}
            disabled={disabled}
            onChange={onChange}
          />
        </Field>
      );
  }
}
