import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";

const BOX =
  "flex h-10 items-center gap-2 rounded-lg border bg-surface px-3 transition-[border-color,box-shadow] duration-200 " +
  "focus-within:border-focus focus-within:shadow-[0_0_0_3px_var(--c-primary-soft)] " +
  // Ô đang khóa: nền chìm, chữ nhạt, con trỏ cấm, để khác hẳn ô sửa được.
  "has-[:disabled]:bg-sunken has-[:disabled]:opacity-70 [&_:disabled]:cursor-not-allowed [&_:disabled]:text-muted";

type FieldProps = {
  label: ReactNode;
  htmlFor?: string;
  required?: boolean;
  /** Giữ để tương thích; giao diện tối giản không hiện chữ hướng dẫn dưới trường. */
  hint?: ReactNode;
  /** Lỗi hiện dưới ô, trình đọc màn hình đọc ngay. */
  error?: string | null;
  children: ReactNode;
};

/** Nhãn, ô nhập và lỗi của một trường. */
export function Field({ label, htmlFor, required, error, children }: FieldProps) {
  return (
    <div className="flex min-w-0 flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-xs font-semibold text-body">
        {label}
        {required && <span className="text-primary-ink"> *</span>}
      </label>
      {children}
      {error && (
        <p role="alert" className="flex animate-fade items-center gap-1 text-xs text-danger">
          <Icon name="error" size={13} />
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
    <div className={cx(BOX, invalid ? "border-danger" : "border-field", wrapperClassName)}>
      {icon && <Icon name={icon} size={16} className="text-muted" />}
      <input
        aria-invalid={invalid || undefined}
        className={cx(
          "min-w-0 flex-1 bg-transparent text-[13.5px] text-ink outline-none placeholder:text-muted",
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
    <div className={cx(BOX, "relative pr-2", invalid ? "border-danger" : "border-field", wrapperClassName)}>
      <select
        aria-invalid={invalid || undefined}
        className={cx("min-w-0 flex-1 cursor-pointer appearance-none bg-transparent pr-6 text-[13.5px] text-ink outline-none", className)}
        {...rest}
      >
        {children}
      </select>
      <Icon name="unfold_more" size={14} className="pointer-events-none absolute right-2.5 text-muted" />
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
    <span className="inline-flex items-center gap-2.5 text-[13.5px] text-ink">
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={ariaLabel}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={cx(
          "relative inline-block h-5 w-9 shrink-0 rounded-full transition-colors duration-200 disabled:opacity-45",
          checked ? "bg-primary" : "bg-line-strong",
        )}
      >
        <span
          className={cx(
            "absolute top-0.5 left-0.5 size-4 rounded-full bg-surface shadow-sm transition-transform duration-300 ease-[cubic-bezier(.16,1,.3,1)]",
            checked && "translate-x-4",
          )}
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
              "inline-flex h-8 items-center gap-1 rounded-full border px-3 text-xs font-semibold transition-colors duration-200 disabled:opacity-45",
              selected ? "border-primary bg-primary-soft text-primary-ink" : "border-dashed border-line-strong text-body hover:bg-surface",
            )}
          >
            {selected && <Icon name="check" size={12} />}
            {option}
          </button>
        );
      })}
    </div>
  );
}

type SegmentedProps<T extends string> = {
  value: T;
  onChange: (value: T) => void;
  options: { value: T; label: ReactNode; disabled?: boolean; title?: string }[];
  ariaLabel: string;
  /** "tab": mỗi lựa chọn là tab (lọc danh sách); "radio": chọn giá trị trong form. */
  as?: "radio" | "tab";
  className?: string;
};

/** Nhóm lựa chọn liền khối nền chìm; mục đang chọn nổi lên. */
export function Segmented<T extends string>({ value, onChange, options, ariaLabel, as = "radio", className }: SegmentedProps<T>) {
  return (
    <div
      role={as === "tab" ? "tablist" : "radiogroup"}
      aria-label={ariaLabel}
      className={cx("inline-flex max-w-full gap-0.5 overflow-x-auto rounded-xl bg-sunken p-[3px]", className)}
    >
      {options.map((option) => {
        const on = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            role={as === "tab" ? "tab" : "radio"}
            aria-selected={as === "tab" ? on : undefined}
            aria-checked={as === "radio" ? on : undefined}
            disabled={option.disabled}
            title={option.title}
            onClick={() => onChange(option.value)}
            className={cx(
              "flex-1 rounded-[7px] px-3 py-1.5 text-[12.5px] font-semibold whitespace-nowrap transition-[background-color,color,box-shadow] duration-200",
              "disabled:cursor-not-allowed disabled:opacity-45",
              on ? "bg-surface text-ink shadow-[0_1px_2px_rgb(0_0_0/0.06)]" : "text-muted hover:text-ink",
            )}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
