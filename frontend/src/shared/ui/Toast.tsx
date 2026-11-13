"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { cx } from "./cx";
import { Icon } from "./Icon";

export type ToastTone = "success" | "warning" | "danger";
export type ToastItem = { id: number; text: string; tone: ToastTone; persistent?: boolean };
export type ToastOptions = {
  /** Lỗi cần người dùng đọc: không tự ẩn, ở lại tới khi bấm đóng, trình đọc màn hình đọc ngay. */
  persistent?: boolean;
};

export const TOAST_VISIBLE_MS = 4000;
const listeners = new Set<(item: ToastItem) => void>();
let nextId = 1;

/**
 * Hiện một câu ngắn nổi ở góc phải, ngay dưới thanh trên cùng; giữ qua lần chuyển trang. Mặc định tự ẩn sau vài giây, kể
 * cả câu màu đỏ báo đã xong một thao tác xóa hay lưu trữ. Báo lỗi thì dùng {@link showErrorToast}.
 */
export function showToast(text: string, tone: ToastTone = "success", options: ToastOptions = {}) {
  const item: ToastItem = { id: nextId++, text, tone, persistent: options.persistent };
  listeners.forEach((listener) => listener(item));
}

/** Báo lỗi: màu đỏ, không tự ẩn, ở lại tới khi người dùng bấm đóng. */
export function showErrorToast(text: string) {
  showToast(text, "danger", { persistent: true });
}

/** Nơi vẽ thông báo nổi; đặt một lần ở layout gốc. */
export function Toaster() {
  const [items, setItems] = useState<ToastItem[]>([]);
  const timers = useRef(new Map<number, ReturnType<typeof setTimeout>>());

  const dismissToast = useCallback((id: number) => {
    const timer = timers.current.get(id);
    if (timer) clearTimeout(timer);
    timers.current.delete(id);
    setItems((current) => current.filter((item) => item.id !== id));
  }, []);

  const scheduleDismiss = useCallback((item: ToastItem) => {
    if (item.persistent || timers.current.has(item.id)) return;
    timers.current.set(item.id, setTimeout(() => dismissToast(item.id), TOAST_VISIBLE_MS));
  }, [dismissToast]);

  /** Rê chuột hoặc focus vào chồng thông báo thì dừng đếm giờ; rời ra thì đếm lại từ đầu. */
  const pauseDismiss = useCallback(() => {
    timers.current.forEach(clearTimeout);
    timers.current.clear();
  }, []);

  useEffect(() => {
    const pending = timers.current;
    const listener = (item: ToastItem) => {
      setItems((current) => [...current, item]);
      scheduleDismiss(item);
    };
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
      pending.forEach(clearTimeout);
      pending.clear();
    };
  }, [scheduleDismiss]);

  return (
    <ToastStack
      items={items}
      className="top-[68px]"
      onDismiss={dismissToast}
      onPause={pauseDismiss}
      onResume={() => items.forEach(scheduleDismiss)}
    />
  );
}

type ToastStackProps = {
  items: ToastItem[];
  className?: string;
  /** Có thì mỗi thông báo có nút đóng. */
  onDismiss?: (id: number) => void;
  onPause?: () => void;
  onResume?: () => void;
};

/** Chồng thông báo ở góc phải; dùng chung cho màn sau đăng nhập (dưới thanh trên) và màn chưa đăng nhập. */
export function ToastStack({ items, className, onDismiss, onPause, onResume }: ToastStackProps) {
  return (
    <div
      className={cx("pointer-events-none fixed right-4 z-[110] flex max-w-[calc(100vw-2rem)] flex-col items-end gap-2 md:right-6", className)}
      onMouseEnter={onPause}
      onMouseLeave={onResume}
      onFocus={onPause}
      onBlur={onResume}
    >
      {items.map((item) => (
        <div
          key={item.id}
          role={item.persistent ? "alert" : "status"}
          className={cx(
            "pointer-events-auto flex animate-slide-in items-center gap-2 rounded-xl border bg-surface pl-4 text-[13px] font-semibold shadow-[var(--shadow-float)]",
            onDismiss ? "py-1.5 pr-1.5" : "py-2.5 pr-4",
            item.tone === "success" && "border-success-soft text-success",
            item.tone === "warning" && "border-warning-soft text-warning",
            item.tone === "danger" && "border-danger-soft text-danger",
          )}
        >
          <Icon name={item.tone === "success" ? "check_circle" : item.tone === "danger" ? "error" : "warning"} size={16} className="shrink-0" />
          <p className="min-w-0">{item.text}</p>
          {onDismiss && (
            <button
              type="button"
              aria-label="Đóng thông báo"
              onClick={() => onDismiss(item.id)}
              className="inline-flex size-8 shrink-0 cursor-pointer items-center justify-center rounded-lg text-muted transition-colors hover:bg-sunken hover:text-ink"
            >
              <Icon name="close" size={14} />
            </button>
          )}
        </div>
      ))}
    </div>
  );
}
