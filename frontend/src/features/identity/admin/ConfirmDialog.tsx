"use client";

import type { ReactNode } from "react";

/** Popup xác nhận dùng chung cho đổi vai trò và đổi trạng thái. */
export function ConfirmDialog({
  title,
  children,
  confirmLabel,
  danger,
  busy,
  error,
  onConfirm,
  onCancel,
}: {
  title: string;
  children: ReactNode;
  confirmLabel: string;
  danger?: boolean;
  busy?: boolean;
  error?: ReactNode;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div role="dialog" aria-modal="true" aria-label={title} className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        <div className="mt-3 text-sm text-slate-600">{children}</div>
        {error && (
          <div role="alert" className="mt-3 text-sm text-red-600">
            {error}
          </div>
        )}
        <div className="mt-6 flex justify-end gap-2">
          <button type="button" onClick={onCancel} className="rounded-lg px-4 py-2 text-sm text-slate-700 hover:bg-slate-100">
            Hủy
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className={`rounded-lg px-4 py-2 text-sm font-semibold text-white disabled:opacity-60 ${danger ? "bg-red-600" : "bg-[#1f2a6b]"}`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
