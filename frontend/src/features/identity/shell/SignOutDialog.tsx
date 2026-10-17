"use client";

import { useState } from "react";
import { logout } from "../api";

/** Popup xác nhận Logout (UC 03): chỉ thu hồi phiên hiện tại, các thiết bị khác giữ nguyên (BR-U01-45). */
export function SignOutDialog({ onCancel, onSignedOut }: { onCancel: () => void; onSignedOut: () => void }) {
  const [busy, setBusy] = useState(false);

  async function confirm() {
    setBusy(true);
    try {
      await logout();
    } catch {
      // Backend luôn xóa cookie; lỗi mạng thì vẫn rời phiên phía client.
    }
    onSignedOut();
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div role="dialog" aria-modal="true" aria-labelledby="sign-out-title" className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl">
        <h2 id="sign-out-title" className="text-lg font-semibold text-slate-900">
          Đăng xuất?
        </h2>
        <p className="mt-2 text-sm text-slate-600">Bạn sẽ đăng xuất khỏi thiết bị này.</p>
        <div className="mt-6 flex justify-end gap-2">
          <button type="button" onClick={onCancel} className="rounded-lg px-4 py-2 text-sm text-slate-700 hover:bg-slate-100">
            Hủy
          </button>
          <button
            type="button"
            onClick={confirm}
            disabled={busy}
            className="rounded-lg bg-[#1f2a6b] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            Đăng xuất
          </button>
        </div>
      </div>
    </div>
  );
}
