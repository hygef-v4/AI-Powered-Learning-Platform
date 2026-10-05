"use client";

import { useEffect, useRef, type ReactNode } from "react";

type DialogProps = {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
};

/** Dùng `<dialog>` gốc của trình duyệt: có focus trap và Esc để đóng. */
export function Dialog({ open, title, onClose, children, footer }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal?.();
    if (!open && dialog.open) dialog.close?.();
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-label={title}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      className="w-full max-w-lg rounded-lg p-0 shadow-xl backdrop:bg-black/40"
    >
      {open && (
        <div className="flex flex-col">
          <header className="flex items-center justify-between border-b px-5 py-3">
            <h2 className="text-base font-semibold">{title}</h2>
            <button type="button" onClick={onClose} aria-label="Đóng" className="text-slate-500 hover:text-slate-800">
              ×
            </button>
          </header>
          <div className="px-5 py-4">{children}</div>
          {footer && <footer className="flex justify-end gap-2 border-t px-5 py-3">{footer}</footer>}
        </div>
      )}
    </dialog>
  );
}
