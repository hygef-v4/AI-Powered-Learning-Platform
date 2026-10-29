import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";

const BOX =
  "flex h-[38px] items-center gap-2 rounded-[9px] border bg-white px-3 " +
  "focus-within:outline-2 focus-within:outline-offset-1 focus-within:outline-primary";

type FieldProps = {
  label: ReactNode;
  htmlFor?: string;
  required?: boolean;
  hint?: ReactNode;
  /** Lỗi hiện dưới ô, trình đọc màn hình đọc ngay. */
  error?: string | null;
  children: ReactNode;
};

/** Nhãn, ô nhập, gợi ý và lỗi của một trường. */
export function Field({ label, htmlFor, required, hint, error, children }: FieldProps) {
  return (
    <div className="flex min-w-0 flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-xs font-semibold text-body">
        {label}
        {required && <span className="text-danger"> *</span>}
      </label>
      {children}
      {hint && !error && <p className="text-[11px] text-muted">{hint}</p>}
      {error && (
        <p role="alert" className="flex items-center gap-1 text-[11px] text-danger">
          <Icon name="error" size={14} />
          {error}
        </p>
      )}
    </div>
  );
}

type TextInputProps = InputHTMLAttributes<HTMLInputElement> & {
  invalid?: boolean;
  /** Chữ nhỏ cuối ô, ví dụ đơn vị "MB". */
  suffix?: ReactNode;
  icon?: IconName;
  wrapperClassName?: string;
};

export function TextInput({ invalid, suffix, icon, wrapperClassName, className, ...rest }: TextInputProps) {
  return (
    <div className={cx(BOX, invalid ? "border-danger" : "border-line-strong", wrapperClassName)}>
      {icon && <Icon name={icon} size={18} className="text-muted" />}
      <input
        aria-invalid={invalid || undefined}
        className={cx(
          "min-w-0 flex-1 bg-transparent text-[13px] text-ink outline-none placeholder:text-faint",
          // Ô số không có nút tăng giảm, giống design.
          "[appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none",
          className,
        )}
        {...rest}
      />
      {suffix && <span className="text-xs text-muted">{suffix}</span>}
    </div>
  );
}

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean; wrapperClassName?: string };

export function Select({ invalid, wrapperClassName, className, children, ...rest }: SelectProps) {
  return (
    <div className={cx(BOX, "relative pr-2", invalid ? "border-danger" : "border-line-strong", wrapperClassName)}>
      <select
        aria-invalid={invalid || undefined}
        className={cx("min-w-0 flex-1 appearance-none bg-transparent pr-6 text-[13px] text-ink outline-none", className)}
        {...rest}
      >
        {children}
      </select>
      <Icon name="expand_more" size={18} className="pointer-events-none absolute right-2 text-muted" />
    </div>
  );
}

type SwitchProps = {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label: ReactNode;
  ariaLabel?: string;
  disabled?: boolean;
};

/** Công tắc bật/tắt. */
export function Switch({ checked, onChange, label, ariaLabel, disabled }: SwitchProps) {
  return (
    <span className="inline-flex items-center gap-2.5 text-[13px] text-ink">
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={ariaLabel}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={cx(
          "relative inline-block h-5 w-9 shrink-0 rounded-full transition-colors disabled:opacity-45",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary",
          checked ? "bg-primary" : "bg-line-strong",
        )}
      >
        <span
          className={cx("absolute top-0.5 size-4 rounded-full bg-white transition-all", checked ? "left-[18px]" : "left-0.5")}
        />
      </button>
      <span>{label}</span>
    </span>
  );
}

type ChipGroupProps = {
  options: string[];
  value: string[];
  onChange: (value: string[]) => void;
  ariaLabel: string;
  disabled?: boolean;
};

/** Chọn nhiều bằng các nhãn bấm bật/tắt, giữ thứ tự của danh sách lựa chọn. */
export function ChipToggleGroup({ options, value, onChange, ariaLabel, disabled }: ChipGroupProps) {
  return (
    <div role="group" aria-label={ariaLabel} className="flex flex-wrap items-center gap-1.5">
      {options.map((option) => {
        const selected = value.includes(option);
        return (
          <button
            key={option}
            type="button"
            aria-pressed={selected}
            disabled={disabled}
            onClick={() => onChange(selected ? value.filter((v) => v !== option) : options.filter((o) => o === option || value.includes(o)))}
            className={cx(
              "inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold transition-colors",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-45",
              selected ? "border-primary/30 bg-primary-soft text-primary" : "border-line-strong bg-white text-body hover:bg-shell",
            )}
          >
            {selected && <Icon name="check" size={14} />}
            {option}
          </button>
        );
      })}
    </div>
  );
}
