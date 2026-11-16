"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { fetchApi } from "@/shared/api/client";
import { clearCurrentUser } from "@/shared/auth/useCurrentUser";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";
import { msg } from "@/i18n/msg";

/** Xác nhận đăng xuất (UC 03): gọi logout, xóa trạng thái phía client, về trang đăng nhập. */
export function LogoutDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function logout() {
    setBusy(true);
    setError(null);
    try {
      await fetchApi("/api/v1/auth/logout", { method: "POST" });
      clearCurrentUser();
      router.replace("/login");
    } catch {
      setError(msg("shared.layout.LogoutDialog.couldNotSignOutPlease"));
      setBusy(false);
    }
  }

  return (
    <Dialog
      open={open}
      title={msg("shared.layout.LogoutDialog.signOut")}
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button variant="danger" icon="logout" onClick={() => void logout()} disabled={busy}>
            {msg("shared.layout.LogoutDialog.signOut2")}</Button>
        </>
      }
    >
      {error && (
        <p role="alert" className="text-danger">
          {error}
        </p>
      )}
    </Dialog>
  );
}
