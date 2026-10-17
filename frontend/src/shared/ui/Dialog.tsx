"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { Icon, type IconName } from "./Icon";
import { IconTile } from "./IconTile";
import type { Tone } from "./tones";

type Props = {
  open: boolean;
  title: ReactNode;
  icon?: IconName;
  tone?: Tone;
  onClose: () => void;
  footer?: ReactNode;
  children: ReactNode;
};

/** Popup giữa màn hình, nền mờ; Esc hoặc bấm ra ngoài để đóng. */
export function Dialog({ open, title, icon, tone = "primary", onClose, footer, children }: Props) {
  const titleId = useId();
  const panel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    panel.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/45"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className="w-[420px] max-w-[calc(100%-48px)] overflow-hidden rounded-[18px] bg-white shadow-[0_24px_60px_rgba(15,23,42,.28)] outline-none"
      >
        <div className="flex items-start gap-3 border-b border-line px-[22px] pt-[18px] pb-3.5">
          {icon && <IconTile icon={icon} tone={tone} />}
          <h2 id={titleId} className="min-w-0 self-center text-[17px] font-bold text-ink">
            {title}
          </h2>
          <button type="button" aria-label="Đóng" onClick={onClose} className="ml-auto rounded-md text-muted hover:text-ink">
            <Icon name="close" />
          </button>
        </div>
        <div className="flex flex-col gap-3.5 px-[22px] py-[18px] text-[13px] leading-normal text-body">{children}</div>
        {footer && <div className="flex items-center justify-end gap-2 border-t border-line px-[22px] py-3.5">{footer}</div>}
      </div>
    </div>
  );
}
