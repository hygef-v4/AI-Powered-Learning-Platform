"use client";

import { useId, useRef, useState, type InputHTMLAttributes } from "react";
import { cx } from "./cx";
import { Icon } from "./Icon";
import { TextInput } from "./Inputs";
import { msg } from "@/i18n/msg";

type DateInputProps = Omit<InputHTMLAttributes<HTMLInputElement>,
  "type" | "value" | "defaultValue" | "onChange" | "min" | "max"> & {
  value: string;
  onChange: (value: string) => void;
  onValidityChange?: (valid: boolean) => void;
  min?: string;
  max?: string;
  invalid?: boolean;
  wrapperClassName?: string;
  "data-testid"?: string;
};

function formatDate(value: string): string {
  return value ? `${value.slice(8, 10)}/${value.slice(5, 7)}/${value.slice(0, 4)}` : "";
}

function parseDate(text: string, min?: string, max?: string): string {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(text);
  if (!match) return "";
  const [, day, month, year] = match;
  const iso = `${year}-${month}-${day}`;
  const date = new Date(`${iso}T00:00:00Z`);
  if (year === "0000" || !Number.isFinite(date.getTime()) || date.toISOString().slice(0, 10) !== iso) return "";
  return (min && iso < min) || (max && iso > max) ? "" : iso;
}

/** Hiển thị ngày cố định; chỉ chuyển ngày ISO hợp lệ hoặc rỗng cho form/API. */
export function DateInput({ value, onChange, onValidityChange, min, max, invalid,
  disabled, readOnly, wrapperClassName, "aria-describedby": describedBy, ...props }: DateInputProps) {
  const errorId = useId();
  const nativeInput = useRef<HTMLInputElement>(null);
  const [draft, setDraft] = useState({ value, text: formatDate(value) });
  if (draft.value !== value) setDraft({ value, text: formatDate(value) });
  const text = draft.value === value ? draft.text : formatDate(value);
  const dateInvalid = text !== "" && !parseDate(text, min, max);

  function updateDate(nextText: string) {
    const iso = parseDate(nextText, min, max);
    setDraft({ value: iso, text: nextText });
    onValidityChange?.(nextText === "" || iso !== "");
    onChange(iso);
  }

  function openCalendar() {
    const input = nativeInput.current;
    if (!input || disabled || readOnly) return;
    try {
      if (input.showPicker) input.showPicker();
      else input.click();
    } catch {
      // Một số trình duyệt có API nhưng không cho mở picker trong ngữ cảnh hiện tại.
      input.click();
    }
  }

  return (
    <div className={cx("relative min-w-0", wrapperClassName)}>
      <TextInput
        size={10}
        {...props}
        type="text"
        value={text}
        disabled={disabled}
        readOnly={readOnly}
        placeholder="dd/MM/yyyy"
        maxLength={10}
        invalid={invalid || dateInvalid}
        aria-describedby={[describedBy, dateInvalid && errorId].filter(Boolean).join(" ") || undefined}
        onChange={(event) => updateDate(event.target.value)}
        onKeyDown={(event) => {
          props.onKeyDown?.(event);
          if (!event.defaultPrevented && event.altKey && event.key === "ArrowDown") {
            event.preventDefault();
            openCalendar();
          }
        }}
        suffix={<button type="button" aria-label={msg("shared.ui.DateInput.chonNgayTuLich")} disabled={disabled || readOnly}
          data-testid={props["data-testid"] ? `${props["data-testid"]}-calendar` : undefined}
          onClick={openCalendar}
          className="flex size-7 items-center justify-center rounded text-muted hover:bg-sunken focus-visible:outline-2 focus-visible:outline-focus disabled:cursor-not-allowed">
          <Icon name="calendar_month" size={18} />
        </button>}
      />
      <input ref={nativeInput} type="date" value={value} min={min} max={max}
        disabled={disabled || readOnly} tabIndex={-1} aria-hidden="true"
        className="pointer-events-none absolute bottom-0 left-0 size-px opacity-0"
        onChange={(event) => updateDate(formatDate(event.target.value))} />
      {dateInvalid && <p id={errorId} role="alert" className="mt-1.5 text-xs text-danger">
        {msg("shared.ui.DateInput.nhapNgayHopLeTheoDd")}</p>}
    </div>
  );
}
