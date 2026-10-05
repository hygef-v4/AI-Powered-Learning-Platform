"use client";

import { useState } from "react";
import { Input, type InputProps } from "./Input";

/** Chính sách phía client (BR-U01): ≥ 8 ký tự, có chữ và số. Backend vẫn kiểm lại. */
export const PASSWORD_RULES = [
  { id: "length", label: "Ít nhất 8 ký tự", test: (v: string) => v.length >= 8 },
  { id: "letter", label: "Có chữ cái", test: (v: string) => /[A-Za-z]/.test(v) },
  { id: "digit", label: "Có chữ số", test: (v: string) => /\d/.test(v) },
] as const;

export function isPasswordValid(value: string): boolean {
  return PASSWORD_RULES.every((rule) => rule.test(value));
}

type PasswordFieldProps = Omit<InputProps, "type"> & {
  value: string;
  showPolicy?: boolean;
};

export function PasswordField({ value, showPolicy = false, ...rest }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="flex flex-col gap-1">
      <div className="relative">
        <Input {...rest} value={value} type={visible ? "text" : "password"} autoComplete={rest.autoComplete ?? "current-password"} />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          className="absolute right-2 top-8 text-xs text-slate-500 hover:text-slate-800"
          aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
        >
          {visible ? "Ẩn" : "Hiện"}
        </button>
      </div>
      {showPolicy && (
        <ul className="text-xs" aria-label="Điều kiện mật khẩu">
          {PASSWORD_RULES.map((rule) => {
            const ok = rule.test(value);
            return (
              <li key={rule.id} className={ok ? "text-green-700" : "text-slate-500"}>
                {ok ? "✓" : "•"} {rule.label}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
