"use client";

import { useRef, type ClipboardEvent, type KeyboardEvent } from "react";

const LENGTH = 6;

/**
 * 6 ô số cho OTP (BR-U01-20): chỉ nhận chữ số, tự nhảy ô, Backspace lùi ô, dán cả mã điền đủ 6 ô.
 * `value` là chuỗi tối đa 6 chữ số.
 */
export function OtpInput({
  value,
  onChange,
  invalid,
  label = "Mã xác thực OTP (6 chữ số)",
}: {
  value: string;
  onChange: (value: string) => void;
  invalid?: boolean;
  label?: string;
}) {
  const inputs = useRef<(HTMLInputElement | null)[]>([]);
  const digits = Array.from({ length: LENGTH }, (_, i) => value[i] ?? "");

  function focus(index: number) {
    inputs.current[Math.max(0, Math.min(LENGTH - 1, index))]?.focus();
  }

  function setDigit(index: number, raw: string) {
    const typed = raw.replace(/\D/g, "");
    // Trình duyệt tự điền (autocomplete="one-time-code") đưa cả mã vào một ô: điền tiếp từ ô này như khi dán.
    // Ô đã có số mà gõ thêm thì giữ số mới gõ.
    const incoming = typed.length > 1 && !digits[index] ? typed : typed.slice(-1);
    const next = digits.slice();
    for (let i = 0; i < Math.max(incoming.length, 1) && index + i < LENGTH; i++) {
      next[index + i] = incoming[i] ?? "";
    }
    onChange(next.join("").slice(0, LENGTH));
    if (incoming) focus(index + incoming.length);
  }

  function onKeyDown(index: number, event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      event.preventDefault();
      const next = digits.slice();
      next[index - 1] = "";
      onChange(next.join(""));
      focus(index - 1);
    } else if (event.key === "ArrowLeft") {
      focus(index - 1);
    } else if (event.key === "ArrowRight") {
      focus(index + 1);
    }
  }

  function onPaste(event: ClipboardEvent<HTMLInputElement>) {
    const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, LENGTH);
    if (pasted) {
      event.preventDefault();
      onChange(pasted);
      focus(pasted.length);
    }
  }

  return (
    <fieldset>
      <legend className="text-[11px] font-semibold tracking-wide text-slate-700 uppercase">{label}</legend>
      <div className="mt-1 flex gap-2">
        {digits.map((digit, index) => (
          <input
            key={index}
            ref={(element) => {
              inputs.current[index] = element;
            }}
            value={digit}
            onChange={(e) => setDigit(index, e.target.value)}
            onKeyDown={(e) => onKeyDown(index, e)}
            onPaste={onPaste}
            inputMode="numeric"
            autoComplete={index === 0 ? "one-time-code" : "off"}
            aria-label={`Chữ số ${index + 1}`}
            aria-invalid={invalid || undefined}
            className={`h-11 w-11 rounded-lg border text-center text-lg font-semibold outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 ${
              invalid ? "border-red-400" : digit ? "border-[#1f2a6b]" : "border-slate-300"
            }`}
          />
        ))}
      </div>
    </fieldset>
  );
}
