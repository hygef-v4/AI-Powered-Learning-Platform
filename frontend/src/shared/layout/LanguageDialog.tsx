"use client";

import { useState } from "react";
import { LOCALE, msg } from "@/i18n/msg";
import { LOCALES, LOCALE_NAMES, chooseLocale, type Locale } from "@/i18n/locales";
import { Dialog } from "@/shared/ui/Dialog";
import { cx } from "@/shared/ui/cx";

/** Thời gian cho chấm radio kịp hiện rồi mới tải lại trang bằng ngôn ngữ mới. */
const SWITCH_DELAY_MS = 180;

/**
 * Popup chọn ngôn ngữ mở từ menu tài khoản (yêu cầu 2026-11-16): các ngôn ngữ là một nhóm radio, tên viết bằng chính ngôn
 * ngữ đó; bấm một ngôn ngữ là chọn luôn, đóng popup và tải lại trang bằng ngôn ngữ mới.
 */
export function LanguageDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [picked, setPicked] = useState<Locale>(LOCALE);

  function pick(code: Locale) {
    setPicked(code);
    if (code === LOCALE) {
      onClose();
      return;
    }
    setTimeout(() => {
      onClose();
      chooseLocale(code);
    }, SWITCH_DELAY_MS);
  }

  return (
    <Dialog open={open} title={msg("shared.layout.AppShell.ngonNgu")} onClose={onClose}>
      <div role="radiogroup" aria-label={msg("shared.layout.AppShell.ngonNgu")} className="flex flex-col gap-1.5">
        {LOCALES.map((code) => {
          const checked = picked === code;
          return (
            <label key={code} lang={code} data-testid={`locale-${code}`}
              className={cx("group flex cursor-pointer items-center gap-3 rounded-xl border px-4 py-3 transition-[background-color,border-color,box-shadow] duration-200",
                checked ? "border-primary bg-primary-soft shadow-[0_0_0_3px_var(--c-primary-soft)]" : "border-line hover:border-line-strong hover:bg-sunken")}>
              <input type="radio" name="locale" value={code} checked={checked} onChange={() => pick(code)}
                onClick={() => { if (checked) pick(code); }} className="peer sr-only" />
              {/* Vòng radio: chấm giữa phóng ra khi chọn; vòng sáng khi focus bằng bàn phím. */}
              <span aria-hidden
                className={cx("flex size-[18px] shrink-0 items-center justify-center rounded-full border-2 transition-colors duration-200 peer-focus-visible:ring-2 peer-focus-visible:ring-focus peer-focus-visible:ring-offset-2",
                  checked ? "border-primary" : "border-line-strong group-hover:border-muted")}>
                <span className={cx("size-2 rounded-full bg-primary transition-transform duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]",
                  checked ? "scale-100" : "scale-0")} />
              </span>
              <span className={cx("text-[14px] text-ink", checked ? "font-semibold" : "font-medium")}>{LOCALE_NAMES[code]}</span>
            </label>
          );
        })}
      </div>
    </Dialog>
  );
}
