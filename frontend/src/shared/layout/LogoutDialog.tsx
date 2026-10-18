"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { apiFetch } from "@/shared/api/client";
import { clearCurrentUser } from "@/shared/auth/useCurrentUser";
import { Button } from "@/shared/ui/Button";
import { Dialog } from "@/shared/ui/Dialog";

/** Xác nhận đăng xuất (UC 03): gọi logout, xóa trạng thái phía client, về trang đăng nhập. */
export function LogoutDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function logout() {
    setBusy(true);
    setError(null);
    try {
      await apiFetch("/api/v1/auth/logout", { method: "POST" });
      clearCurrentUser();
      router.replace("/login");
    } catch {
      setError("Không đăng xuất được, thử lại sau.");
      setBusy(false);
    }
  }

  return (
    <Dialog
      open={open}
      title="Đăng xuất?"
      icon="logout"
      tone="danger"
      onClose={onClose}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            Hủy
          </Button>
          <Button variant="danger" icon="logout" onClick={() => void logout()} disabled={busy}>
            Đăng xuất
          </Button>
        </>
      }
    >
      <p>Bạn sẽ thoát khỏi phiên trên thiết bị này. Bài đang làm đã được tự lưu.</p>
      {error && (
        <p role="alert" className="text-danger">
          {error}
        </p>
      )}
    </Dialog>
  );
}
