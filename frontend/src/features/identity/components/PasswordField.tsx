"use client";

import { useId, useState, type InputHTMLAttributes } from "react";
import { cx } from "@/shared/ui/cx";
import { Icon } from "@/shared/ui/Icon";
import { AUTH_INPUT, AUTH_LABEL } from "./authStyles";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "id"> & {
  label: string;
  valid?: boolean;
};

/** Ô mật khẩu của màn chưa đăng nhập: nút hiện/ẩn (có nhãn cho trình đọc màn hình, NFR-U01-62), dấu ✓ khi hợp lệ. */
export function PasswordField({ label, valid, className, ...input }: Props) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  return (
    <div className={className}>
      <label htmlFor={id} className={AUTH_LABEL}>
        {label}
      </label>
      <div className="relative mt-1.5">
        <input id={id} type={visible ? "text" : "password"} className={cx(AUTH_INPUT, "pr-16")} {...input} />
        <div className="absolute inset-y-0 right-2.5 flex items-center gap-1.5">
          {valid && <Icon name="check" size={18} className="text-[#059669]" />}
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            className="rounded p-0.5 text-[#757684] hover:text-ink"
          >
            <Icon name={visible ? "visibility_off" : "visibility"} size={18} />
          </button>
        </div>
      </div>
    </div>
  );
}
