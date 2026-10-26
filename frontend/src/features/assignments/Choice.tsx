"use client";

import { cx } from "@/shared/ui";

export type ChoiceOption<T extends string> = { value: T; label: string; disabled?: boolean; title?: string };

/** Chọn một trong khối liền nền chìm (popup tạo bài, popup cài đặt); lựa chọn đang chọn nổi lên. */
export function Choice<T extends string>({ name, value, options, onChange, disabled }: {
  name: string;
  value: T;
  options: ChoiceOption<T>[];
  onChange: (value: T) => void;
  disabled?: boolean;
}) {
  return (
    <div role="radiogroup" aria-label={name} className="flex gap-0.5 rounded-[10px] bg-sunken p-[3px]">
      {options.map((option) => {
        const off = option.disabled || disabled;
        const on = value === option.value;
        return (
          <label key={option.value} title={option.title}
            className={cx("relative flex flex-1 cursor-pointer items-center justify-center rounded-[7px] px-3 py-2 text-[13px] font-semibold transition-[background-color,color,box-shadow] duration-200",
              "has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-focus",
              on ? "bg-surface text-ink shadow-[0_1px_2px_rgb(0_0_0/0.06)]" : "text-muted hover:text-ink",
              off && "cursor-not-allowed opacity-45 hover:text-muted")}>
            <input type="radio" name={name} className="sr-only" checked={on} disabled={off} onChange={() => onChange(option.value)} />
            {option.label}
          </label>
        );
      })}
    </div>
  );
}
