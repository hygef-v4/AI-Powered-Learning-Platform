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
  dismissible?: boolean;
  size?: "md" | "lg" | "xl";
  footer?: ReactNode;
  children: ReactNode;
};

/** Popup giữa màn hình, nền mờ; Esc hoặc bấm ra ngoài để đóng. */
export function Dialog({ open, title, icon, tone = "primary", onClose, dismissible = true, size = "md", footer, children }: Props) {
  const titleId = useId();
  const panel = useRef<HTMLDivElement>(null);
  const closeState = useRef({ onClose, dismissible });
  useEffect(() => { closeState.current = { onClose, dismissible }; }, [onClose, dismissible]);

  useEffect(() => {
    if (!open) return;
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const currentPanel = panel.current;
    currentPanel?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        if (closeState.current.dismissible) closeState.current.onClose();
      }
      if (event.key === "Tab" && currentPanel) {
        const controls = [...currentPanel.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]')]
          .filter((element) => !element.closest('[hidden], [aria-hidden="true"]') && !element.matches('input[type="hidden"]'));
        const first = controls[0], last = controls.at(-1);
        if (!first || !last) { event.preventDefault(); currentPanel.focus(); }
        else if (event.shiftKey && (document.activeElement === first || document.activeElement === currentPanel)) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && (document.activeElement === last || document.activeElement === currentPanel)) { event.preventDefault(); first.focus(); }
      }
    };
    const keepFocus = (event: FocusEvent) => { if (event.target instanceof Node && !currentPanel?.contains(event.target)) currentPanel?.focus(); };
    document.addEventListener("keydown", onKey);
    document.addEventListener("focusin", keepFocus);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.removeEventListener("focusin", keepFocus);
      if (previousFocus?.isConnected) previousFocus.focus();
    };
  }, [open]);

  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/45"
      onMouseDown={(event) => {
        if (dismissible && event.target === event.currentTarget) onClose();
      }}
    >
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={`${size === "xl" ? "w-[960px]" : size === "lg" ? "w-[540px]" : "w-[420px]"} flex max-h-[calc(100dvh-48px)] max-w-[calc(100%-48px)] flex-col overflow-hidden rounded-[18px] bg-white shadow-[0_24px_60px_rgba(15,23,42,.28)] outline-none`}
      >
        <div className="flex shrink-0 items-start gap-3 border-b border-line px-[22px] pt-[18px] pb-3.5">
          {icon && <IconTile icon={icon} tone={tone} />}
          <h2 id={titleId} className="min-w-0 self-center text-[17px] font-bold text-ink">
            {title}
          </h2>
          <button type="button" aria-label="Đóng" onClick={onClose} disabled={!dismissible} className="ml-auto rounded-md text-muted hover:text-ink disabled:opacity-45">
            <Icon name="close" />
          </button>
        </div>
        <div className="flex min-h-0 flex-col gap-3.5 overflow-y-auto px-[22px] py-[18px] text-[13px] leading-normal text-body">{children}</div>
        {footer && <div className="flex shrink-0 flex-wrap items-center justify-end gap-2 border-t border-line px-[22px] py-3.5">{footer}</div>}
      </div>
    </div>
  );
}
