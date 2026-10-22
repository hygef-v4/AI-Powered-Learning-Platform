"use client";

import { cx } from "@/shared/ui";

export type ChoiceOption<T extends string> = { value: T; label: string; disabled?: boolean; title?: string };

/** Nhóm thẻ chọn một (popup tạo bài, popup cài đặt): mỗi lựa chọn một thẻ, thẻ đang chọn tô màu chính. */
export function Choice<T extends string>({ name, value, options, onChange, disabled }: {
  name: string;
  value: T;
  options: ChoiceOption<T>[];
  onChange: (value: T) => void;
  disabled?: boolean;
}) {
  return (
    <div role="radiogroup" aria-label={name} className="grid grid-cols-2 gap-2">
      {options.map((option) => (
        <label key={option.value} title={option.title}
          className={cx("flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-2 text-[13px] font-semibold text-ink",
            value === option.value ? "border-primary bg-primary-soft" : "border-line-strong",
            (option.disabled || disabled) && "cursor-not-allowed opacity-50")}>
          <input type="radio" name={name} checked={value === option.value} disabled={option.disabled || disabled}
            onChange={() => onChange(option.value)} />
          {option.label}
        </label>
      ))}
    </div>
  );
}
