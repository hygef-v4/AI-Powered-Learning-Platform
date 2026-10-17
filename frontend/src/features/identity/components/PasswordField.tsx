"use client";

import { useId, useState, type InputHTMLAttributes } from "react";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "id"> & {
  label: string;
  valid?: boolean;
};

/** Ô mật khẩu có nút con mắt (nhãn cho trình đọc màn hình, NFR-U01-62) và dấu ✓ khi hợp lệ. */
export function PasswordField({ label, valid, className, ...input }: Props) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  return (
    <div className={className}>
      <label htmlFor={id} className="text-[11px] font-semibold tracking-wide text-slate-700 uppercase">
        {label}
      </label>
      <div className="relative mt-1">
        <input
          id={id}
          type={visible ? "text" : "password"}
          className="w-full rounded-lg border border-slate-300 px-3 py-2.5 pr-16 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
          {...input}
        />
        <div className="absolute inset-y-0 right-2 flex items-center gap-1">
          {valid && (
            <span className="text-emerald-600" aria-hidden="true">
              ✓
            </span>
          )}
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            className="rounded p-1 text-slate-400 hover:text-slate-600"
          >
            <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
}
