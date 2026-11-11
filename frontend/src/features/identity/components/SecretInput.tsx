"use client";

import { useState, type InputHTMLAttributes } from "react";
import { Icon } from "@/shared/ui/Icon";
import { TextInput } from "@/shared/ui/inputs";

/** Ô mật khẩu trong màn sau đăng nhập: `TextInput` chung kèm nút hiện/ẩn ở cuối ô (design `c06`). */
export function SecretInput(props: Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & { invalid?: boolean }) {
  const [visible, setVisible] = useState(false);
  return (
    <TextInput
      {...props}
      type={visible ? "text" : "password"}
      suffix={
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          className="-mr-1.5 flex size-8 shrink-0 cursor-pointer items-center justify-center rounded-lg text-muted transition-colors hover:bg-sunken hover:text-ink"
        >
          <Icon name={visible ? "visibility_off" : "visibility"} size={18} />
        </button>
      }
    />
  );
}
