"use client";

import { useCallback, useState, type ReactNode } from "react";
import { Button } from "./Button";
import { Dialog } from "./Dialog";

type PendingConfirm = { title: string; confirmLabel: string; onConfirm: () => void };

/**
 * Popup xác nhận trước một thao tác làm mất dữ liệu chưa lưu, thay cho `window.confirm`. Tiêu đề là câu hỏi; một nút màu
 * đỏ để đồng ý, đóng popup (X, Esc, bấm ra ngoài) là không làm. Trả về phần tử popup để đặt vào cây giao diện và hàm
 * `requestConfirm` để mở.
 */
export function useConfirmDialog(): [ReactNode, (title: string, onConfirm: () => void, confirmLabel?: string) => void] {
  const [pending, setPending] = useState<PendingConfirm | null>(null);

  const requestConfirm = useCallback((title: string, onConfirm: () => void, confirmLabel = "Bỏ chỉnh sửa") => {
    setPending({ title, confirmLabel, onConfirm });
  }, []);

  function handleConfirm() {
    const confirmed = pending;
    setPending(null);
    confirmed?.onConfirm();
  }

  const dialog = pending && (
    <Dialog
      open
      title={pending.title}
      onClose={() => setPending(null)}
      footer={
        <Button variant="danger" onClick={handleConfirm} data-testid="confirm-dialog-accept">
          {pending.confirmLabel}
        </Button>
      }
    />
  );
  return [dialog, requestConfirm];
}
