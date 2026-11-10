"use client";

import { useId, useState, type InputHTMLAttributes, type Ref } from "react";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { AUTH_INPUT, AUTH_LABEL } from "./authStyles";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "id"> & {
  label: string;
  valid?: boolean;
  /** Lỗi của riêng ô này: hiện ngay dưới ô, ô được tô viền lỗi và trình đọc màn hình đọc kèm. */
  error?: string | null;
  inputRef?: Ref<HTMLInputElement>;
};

/** Lỗi của một ô trên màn chưa đăng nhập, nằm ngay dưới ô. */
export function FieldError({ id, children }: { id: string; children: string }) {
  return (
    <p id={id} role="alert" className="mt-1.5 flex items-start gap-1.5 text-[13px] text-danger">
      <Icon name="error" size={16} className="mt-px" />
      {children}
    </p>
  );
}

/** Ô mật khẩu của màn chưa đăng nhập: nút hiện/ẩn (có nhãn cho trình đọc màn hình, NFR-U01-62), dấu ✓ khi hợp lệ. */
export function PasswordField({ label, valid, error, inputRef, className, ...input }: Props) {
  const id = useId();
  const errorId = `${id}-error`;
  const [visible, setVisible] = useState(false);
  return (
    <div className={className}>
      <label htmlFor={id} className={AUTH_LABEL}>
        {label}
      </label>
      <div className="relative mt-1.5">
        <input
          id={id}
          ref={inputRef}
          type={visible ? "text" : "password"}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={cx(AUTH_INPUT, "pr-16", error && "border-danger")}
          {...input}
        />
        <div className="absolute inset-y-0 right-2.5 flex items-center gap-1.5">
          {valid && <Icon name="check" size={18} className="text-success" />}
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            className="-mr-1 flex size-8 cursor-pointer items-center justify-center rounded-lg text-muted transition-colors hover:bg-sunken hover:text-ink"
          >
            <Icon name={visible ? "visibility_off" : "visibility"} size={18} />
          </button>
        </div>
      </div>
      {error && <FieldError id={errorId}>{error}</FieldError>}
    </div>
  );
}
