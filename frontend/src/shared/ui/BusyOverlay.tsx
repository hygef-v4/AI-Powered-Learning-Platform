"use client";
import type { ReactNode } from "react";
import { createPortal } from "react-dom";

type Props = {
  /** Tên việc đang chạy, trình đọc màn hình đọc ngay ("Đang tạo bản nháp", "Đang nộp"). */
  label: string;
  /** Vòng xoay bên trái nội dung. */
  spinner?: boolean;
  testId?: string;
  children: ReactNode;
};

/**
 * Màn chắn bận: phủ kín trang với một alertdialog ở giữa, không có nút đóng; dùng khi hệ thống đang làm việc dở và
 * người dùng phải chờ (AI đang soạn, đang tự nộp bài). Popup có tiêu đề và nút đóng thì dùng `Dialog`.
 */
export function BusyOverlay({ label, spinner, testId, children }: Props) {
  if (typeof document === "undefined") return null;
  return createPortal(
    <div
      role="alertdialog"
      aria-modal="true"
      aria-busy="true"
      aria-label={label}
      data-testid={testId}
      className="fixed inset-0 z-[100] flex animate-fade items-center justify-center bg-[var(--c-scrim)] p-4 backdrop-blur-[2px]"
    >
      <div className="flex max-w-[420px] animate-pop items-center gap-3 rounded-xl border border-line bg-surface px-6 py-5 shadow-[var(--shadow-float)]">
        {spinner && (
          <span aria-hidden="true" className="size-5 shrink-0 animate-spin rounded-full border-2 border-line-strong border-t-primary motion-reduce:animate-none" />
        )}
        <div className="min-w-0">{children}</div>
      </div>
    </div>,
    document.body,
  );
}
