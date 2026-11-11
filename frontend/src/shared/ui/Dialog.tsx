"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";
import type { Tone } from "./tones";

type Props = {
  open: boolean;
  title: ReactNode;
  /** Giữ để tương thích; popup tối giản không vẽ ô icon cạnh tiêu đề. */
  icon?: IconName;
  tone?: Tone;
  onClose: () => void;
  dismissible?: boolean;
  size?: "md" | "lg" | "xl";
  footer?: ReactNode;
  /** Bỏ trống với popup xác nhận chỉ cần tiêu đề và nút. */
  children?: ReactNode;
};

/** Các popup đang mở theo thứ tự mở; chỉ popup trên cùng xử lý Esc, Tab và giữ focus. */
const openPanels: HTMLElement[] = [];

const WIDTH = { md: "w-[440px]", lg: "w-[560px]", xl: "w-[960px]" };

/** Popup giữa màn hình, nền mờ; Esc, nút X hoặc bấm ra ngoài để đóng, nên footer không cần nút Hủy. */
export function Dialog({ open, title, onClose, dismissible = true, size = "md", footer, children }: Props) {
  const titleId = useId();
  const panel = useRef<HTMLDivElement>(null);
  const closeState = useRef({ onClose, dismissible });
  useEffect(() => { closeState.current = { onClose, dismissible }; }, [onClose, dismissible]);

  // Khóa cuộn trang nền khi có popup mở; nhiều popup chồng nhau thì popup cuối đóng mới trả lại.
  useEffect(() => {
    if (!open) return;
    const body = document.body;
    const openCount = Number(body.dataset.dialogCount ?? "0");
    if (openCount === 0) {
      body.dataset.dialogOverflow = body.style.overflow;
      body.style.overflow = "hidden";
    }
    body.dataset.dialogCount = String(openCount + 1);
    return () => {
      const remaining = Number(body.dataset.dialogCount ?? "1") - 1;
      if (remaining > 0) {
        body.dataset.dialogCount = String(remaining);
        return;
      }
      body.style.overflow = body.dataset.dialogOverflow ?? "";
      delete body.dataset.dialogCount;
      delete body.dataset.dialogOverflow;
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const currentPanel = panel.current;
    if (currentPanel) openPanels.push(currentPanel);
    const isTopmost = () => openPanels.at(-1) === currentPanel;
    currentPanel?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (!isTopmost()) return;
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
    const keepFocus = (event: FocusEvent) => {
      if (isTopmost() && event.target instanceof Node && !currentPanel?.contains(event.target)) currentPanel?.focus();
    };
    document.addEventListener("keydown", onKey);
    document.addEventListener("focusin", keepFocus);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.removeEventListener("focusin", keepFocus);
      if (currentPanel) openPanels.splice(openPanels.indexOf(currentPanel), 1);
      if (previousFocus?.isConnected) previousFocus.focus();
    };
  }, [open]);

  if (!open) return null;
  const overlay = (
    <div
      className="fixed inset-0 z-[100] flex animate-fade items-center justify-center bg-[var(--c-scrim)] p-4 backdrop-blur-[2px]"
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
        className={`${WIDTH[size]} flex max-h-[calc(100dvh-32px)] max-w-full animate-pop flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-[var(--shadow-float)] outline-none`}
      >
        <div className="flex shrink-0 items-start gap-3 px-6 pt-5 pb-1">
          <h2
            id={titleId}
            className={cx(
              "min-w-0 flex-1 self-center font-display leading-tight tracking-[-0.02em] text-ink",
              size === "md" ? "text-[22px]" : "text-[26px]",
            )}
          >
            {title}
          </h2>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            disabled={!dismissible}
            className="-mr-1.5 inline-flex size-8 cursor-pointer pointer-coarse:size-11 items-center justify-center rounded-lg text-muted transition-colors hover:bg-sunken hover:text-ink disabled:cursor-not-allowed disabled:opacity-45"
          >
            <Icon name="close" size={16} />
          </button>
        </div>
        {children ? (
          <div className="flex min-h-0 flex-col gap-4 overflow-y-auto px-6 pt-3 pb-5 text-[13.5px] leading-relaxed text-body">{children}</div>
        ) : (
          <div className="h-4" />
        )}
        {footer && <div className="flex shrink-0 flex-wrap items-center justify-end gap-2 px-6 pt-1 pb-5">{footer}</div>}
      </div>
    </div>
  );
  // Gắn vào body để lớp phủ phủ cả sidebar và thanh trên cùng, không bị kẹt trong khung nội dung.
  return typeof document === "undefined" ? overlay : createPortal(overlay, document.body);
}
